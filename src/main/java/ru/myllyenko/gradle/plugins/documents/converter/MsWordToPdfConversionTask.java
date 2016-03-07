/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Igor Melnichenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ru.myllyenko.gradle.plugins.documents.converter;

import org.apache.commons.io.FilenameUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToFoConverter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.WorkResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 *
 *
 * @author <a href="mailto:myllyenko@ya.ru">Igor Melnichenko</a>
 */
public class MsWordToPdfConversionTask extends AbstractConversionTask
{
	private static final String MS_WORD_TO_PDF_VBS_SCRIPT_NAME = "ms-word-to-pdf.vbs";

	private static Path temporaryDirectory;
	private static Path vbsScript = null;

	static
	{
		if (Os.isFamily(Os.FAMILY_WINDOWS))
		{
			try
			{
				temporaryDirectory = Files.createTempDirectory(null);
				vbsScript = temporaryDirectory.resolve(MS_WORD_TO_PDF_VBS_SCRIPT_NAME);
				Files.copy(MsWordToPdfConversionTask.class.getResourceAsStream(MS_WORD_TO_PDF_VBS_SCRIPT_NAME), vbsScript);
			}
			catch (IOException exception)
			{
				throw new GradleException("Failed to extract VBS script into temporary directory", exception);
			}
		}
	}

	@Input
	public boolean useLocalMsWord = (vbsScript != null);

	/**
	 * Constructs a task.
	 */
	public MsWordToPdfConversionTask()
	{
		include("**/*.doc");
		include("**/*.docx");

		rename("(.+)\\.docx?$", "$1.pdf");
	}

	@Override
	protected CopyAction createCopyAction()
	{
		File destinationDirectory = getDestinationDir();

		if (destinationDirectory == null)
		{
			throw new InvalidUserDataException("No destination directory has been specified, use 'into' to specify a target directory");
		}

		return new MsWordToPdfConversionCopyAction(getFileLookup().getFileResolver(destinationDirectory));
	}

	private final class MsWordToPdfConversionCopyAction implements CopyAction
	{
		private final FileResolver fileResolver;

		public MsWordToPdfConversionCopyAction(FileResolver fileResolver)
		{
			this.fileResolver = fileResolver;
		}

		@Override
		public WorkResult execute(CopyActionProcessingStream stream)
		{
			FileCopyDetailsInternalAction action = new FileCopyDetailsInternalAction();
			stream.process(action);

			return new SimpleWorkResult(true);
		}

		private class FileCopyDetailsInternalAction implements CopyActionProcessingStreamAction
		{
			@Override
			public void processFile(FileCopyDetailsInternal details)
			{
				Path destination = fileResolver.resolve(details.getRelativePath().getPathString()).toPath().toAbsolutePath();

				if (details.isDirectory())
				{
					details.copyTo(destination.toFile());
				}
				else
				{
					try
					{
						if (useLocalMsWord && (vbsScript != null))
						{
							String command = "cscript " + vbsScript + " " + details.getFile().getAbsolutePath() + " /o:" + destination;
							Runtime.getRuntime().exec(command).waitFor();

							if (!Files.exists(destination))
							{
								useLocalMsWord = false;
								this.convertWithLibrary(details.getFile().toPath(), destination);
							}
						}
						else
						{
							this.convertWithLibrary(details.getFile().toPath(), destination);
						}
					}
					catch (IOException | InterruptedException | ParserConfigurationException | TransformerException |
							TransformerFactoryConfigurationError | FOPException exception)
					{
						throw new GradleException("Failed to convert the file: " + details.getName(), exception);
					}
				}
			}

			private void convertWithLibrary(Path source, Path destination) throws IOException, ParserConfigurationException,
					TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError, FOPException
			{
				String extension = FilenameUtils.getExtension(source.getFileName().toString());

				switch (extension)
				{
					case "doc":
						HWPFDocument hwpfDocument;

						try (InputStream inputStream = Files.newInputStream(source))
						{
							hwpfDocument = new HWPFDocument(inputStream);
						}

						WordToFoConverter wordToFoConverter =
								new WordToFoConverter(XMLHelper.getDocumentBuilderFactory().newDocumentBuilder().newDocument());
						wordToFoConverter.processDocument(hwpfDocument);

						try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
						{
							DOMSource domSource = new DOMSource(wordToFoConverter.getDocument());
							TransformerFactory.newInstance().newTransformer().transform(domSource, new StreamResult(outputStream));

							try (InputStream foInputStream = new ByteArrayInputStream(outputStream.toByteArray());
									OutputStream pdfStream = Files.newOutputStream(destination))
							{
								Fop fop = FopFactory.newInstance(new File(".").toURI()).newFop(MimeConstants.MIME_PDF, pdfStream);

								Transformer transformer = TransformerFactory.newInstance().newTransformer();
								transformer.transform(new StreamSource(foInputStream), new SAXResult(fop.getDefaultHandler()));
							}
						}

						break;
					case "docx":
						XWPFDocument xwpfDocument;

						try (InputStream inputStream = Files.newInputStream(source))
						{
							xwpfDocument = new XWPFDocument(inputStream);
						}

						try (OutputStream pdfStream = Files.newOutputStream(destination))
						{
							PdfConverter.getInstance().convert(xwpfDocument, pdfStream, PdfOptions.create());
						}

						break;
					default:
						throw new GradleException("The file to be processed has the unsupported extension: " + extension);
				}
			}
		}
	}
}
