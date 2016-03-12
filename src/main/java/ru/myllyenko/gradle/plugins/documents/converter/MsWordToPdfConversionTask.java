/*
 * Copyright (C) 2016 Igor Melnichenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.myllyenko.gradle.plugins.documents.converter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
import org.mozilla.universalchardet.UniversalDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * A task that converts Microsoft Word documents to PDF.
 *
 * <p>It processes input files as a {@link org.gradle.api.tasks.Copy} task configured in the following way:</p>
 * <ul>
 *     <li><code>include("**{@literal /}*.doc")</code>;
 *     <li><code>include("**{@literal /}*.docx")</code>;
 *     <li><code>rename("(.+)\\.docx?$", "$1.pdf")</code>.
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <table border="1" cellpadding="4">
 *     <caption>Task Properties</caption>
 *     <tr>
 *         <th>Property</th>
 *         <th>Default Value</th>
 *     </tr>
 *     <tr>
 *         <td>{@link #useLocalMsWord}</td>
 *         <td>{@value #DEFAULT_USE_LOCAL_MS_WORD_VALUE}</td>
 *     </tr>
 * </table>
 *
 * <h2>Configuration Example</h2>
 * <pre><code>
 * task("wordToPdf", type: ru.myllyenko.gradle.plugins.documents.converter.MsWordToPdfConversionTask)
 * {
 *     from("src/main/word")
 *     into("build/docs/pdf")
 *
 *     useLocalMsWord = true
 * }
 * </code></pre>
 *
 * @author <a href="mailto:myllyenko@ya.ru">Igor Melnichenko</a>
 */
public class MsWordToPdfConversionTask extends AbstractConversionTask
{
	/**
	 * A default value of the {@link #useLocalMsWord} property.
	 */
	public static final boolean DEFAULT_USE_LOCAL_MS_WORD_VALUE = false;

	private static final String MS_WORD_TO_PDF_VBS_SCRIPT_NAME = "ms-word-to-pdf.vbs";

	private static Path vbsScript = null;

	static
	{
		if (Os.isFamily(Os.FAMILY_WINDOWS))
		{
			try
			{
				vbsScript = Files.createTempDirectory(null).resolve(MS_WORD_TO_PDF_VBS_SCRIPT_NAME);
				Files.copy(MsWordToPdfConversionTask.class.getResourceAsStream(MS_WORD_TO_PDF_VBS_SCRIPT_NAME), vbsScript);
			}
			catch (IOException exception)
			{
				throw new GradleException("Failed to extract VBS script into temporary directory", exception);
			}
		}
	}

	/**
	 * Indicates whether or not document conversion must be performed by means of a local Microsoft Word instance.
	 *
	 * <p>The {@code true} value of this property is supported only on Windows platforms. If it is set, a local Microsoft Word instance is
	 *         called via the <a href="http://www.suodenjoki.dk/us/productions/articles/word2pdf.htm">Michael Suodenjoki's script</a>.</p>
	 * <p>If the property is set to {@code false}, conversion is performed by means of third-party Java libraries: Apache POI
	 *         (<code>doc</code> → <code>HWPF</code> → <code>FO</code> → <code>PDF</code>) and Apache POI + xdocreport (<code>docx</code> →
	 *         <code>XWPF</code> → <code>PDF</code>).</p>
	 * <p>Please note that the latter approach is ill-suited for complex documents so use it carefully. It is chosen as default only for
	 *         cross-platform compatibility reasons.</p>
	 */
	@Input
	public boolean useLocalMsWord = DEFAULT_USE_LOCAL_MS_WORD_VALUE;

	/**
	 * Constructs a task.
	 */
	public MsWordToPdfConversionTask()
	{
		super();

		include("**/*.doc");
		include("**/*.docx");

		rename("(.+)\\.docx?$", "$1.pdf");
	}

	@Override
	protected void copy()
	{
		if (this.useLocalMsWord && (vbsScript == null))
		{
			throw new GradleException("The useLocalMsWord parameter set to true is not supported on the current platform");
		}

		super.copy();
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
						if (useLocalMsWord)
						{
							this.convertWithWord(details, destination);
						}
						else
						{
							this.convertWithLibrary(details.getFile().toPath(), destination);
						}
					}
					catch (DocumentConversionException exception)
					{
						throw new GradleException("Failed to convert the file: " + details.getSourcePath(), exception);
					}
				}
			}

			private void convertWithWord(FileCopyDetailsInternal details, Path destination) throws DocumentConversionException
			{
				String baseExceptionMessage = "Failed to convert a document by means of a local MS Word instance";

				try
				{
					ProcessBuilder processBuilder = new ProcessBuilder(/*"cmd.exe", "/c", */"cscript",
							vbsScript.toString(), details.getFile().getAbsolutePath(), "/o:" + destination.toString());
					processBuilder.redirectErrorStream(true);
					Process process = processBuilder.start();
					process.waitFor();

					if (!Files.exists(destination))
					{
						StringBuilder exceptionMessageBuilder = new StringBuilder(baseExceptionMessage);
						exceptionMessageBuilder.append(". VB script output:\n");

						byte[] rawOutput;

						try (InputStream processOutput = process.getInputStream())
						{
							rawOutput = IOUtils.toByteArray(processOutput);
						}

						UniversalDetector charsetDetector = new UniversalDetector(null);
						charsetDetector.handleData(rawOutput, 0, rawOutput.length);
						charsetDetector.dataEnd();

						String detectedCharsetName = charsetDetector.getDetectedCharset();
						Charset charset = (detectedCharsetName == null) ? StandardCharsets.UTF_8 : Charset.forName(detectedCharsetName);

						exceptionMessageBuilder.append(new String(rawOutput, charset));

						throw new DocumentConversionException(exceptionMessageBuilder.toString());
					}
				}
				catch (IOException | InterruptedException | TransformerFactoryConfigurationError exception)
				{
					throw new DocumentConversionException(baseExceptionMessage, exception);
				}
			}

			private void convertWithLibrary(Path source, Path destination) throws DocumentConversionException
			{
				try
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
							throw new DocumentConversionException("The file to be processed has the unsupported extension: " + extension);
					}
				}
				catch (IOException | ParserConfigurationException | TransformerException | TransformerFactoryConfigurationError |
						FOPException exception)
				{
					throw new DocumentConversionException("Failed to convert a document by means of Java libraries", exception);
				}
			}
		}
	}
}
