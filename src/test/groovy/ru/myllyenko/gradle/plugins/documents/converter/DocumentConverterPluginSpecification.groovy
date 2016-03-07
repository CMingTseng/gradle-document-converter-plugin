package ru.myllyenko.gradle.plugins.documents.converter

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Title
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 *
 *
 * @author <a href="mailto:myllyenko@ya.ru">Igor Melnichenko</a>
 */
@Title('Document Converter Plugin Specification')
@Timeout(value = 10, unit = TimeUnit.SECONDS)
final class DocumentConverterPluginSpecification extends Specification
{
	private static final Collection<File> CLASSPATH = System.properties.'test.classpath'.split(';').collect { new File(it) }

	private static final String[] COMMON_ARGUMENTS = ['--stacktrace', '--info']

	private static final String SUCCESSFULT_BUILD_MESSAGE = "BUILD SUCCESSFUL"

	private static final List<String> TARGET_GRADLE_VERSIONS = ['2.10', '2.11']
	private static final List<Boolean> BOOLEAN_VALUES = [true, false]

	@Unroll
	void "Conversion tasks with Gradle #gradleVersion and useLocalMsWord = #useLocalMsWord"(String gradleVersion, boolean useLocalMsWord)
	{
		Path projectDirectory = Files.createDirectories(Paths.get("${gradleVersion}-${useLocalMsWord}"))
		Path docDirectory = projectDirectory.resolve("doc")
		Path docxDirectory = projectDirectory.resolve("docx")
		Path destinationDirectory = projectDirectory.resolve("pdf")
		Path buildScript = projectDirectory.resolve("build.gradle")
		String taskName = "msWordToPdf"

		buildScript.toFile() <<
		"""
			plugins
			{
				id "document-converter"
			}

			task("${taskName}", type: ru.myllyenko.gradle.plugins.documents.converter.MsWordToPdfConversionTask)
			{
				from("${projectDirectory.relativize(docDirectory)}")
				from("${projectDirectory.relativize(docxDirectory)}")
				into("${projectDirectory.relativize(destinationDirectory)}")

				useLocalMsWord = ${useLocalMsWord}
			}
		"""

		when:
		Path doc = Paths.get(DocumentConverterPluginSpecification.getResource("/doc.doc").toURI())
		Path docSubdirectory = docDirectory.resolve("subdirectory")
		Files.createDirectories(docSubdirectory)
		Files.copy(doc, docDirectory.resolve(doc.fileName))
		Files.copy(doc, docSubdirectory.resolve(doc.fileName))

		Path docx = Paths.get(DocumentConverterPluginSpecification.getResource("/docx.docx").toURI())
		Path docxSubdirectory = docxDirectory.resolve("subdirectory")
		Files.createDirectories(docxSubdirectory)
		Files.copy(docx, docxDirectory.resolve(docx.fileName))
		Files.copy(docx, docxSubdirectory.resolve(docx.fileName))

		BuildResult buildResult = GradleRunner.create().withProjectDir(projectDirectory.toFile()).withDebug(true).forwardOutput().
				withPluginClasspath(CLASSPATH).withArguments((COMMON_ARGUMENTS + [taskName]).flatten()).withGradleVersion(gradleVersion).
				build()

		then:
		buildResult.output.contains(SUCCESSFULT_BUILD_MESSAGE)
		checkResults(destinationDirectory)

		where:
		gradleVersion << Collections.nCopies(BOOLEAN_VALUES.size(), TARGET_GRADLE_VERSIONS).flatten().sort()
		useLocalMsWord << Collections.nCopies(TARGET_GRADLE_VERSIONS.size(), BOOLEAN_VALUES).flatten()
	}

	private static boolean checkResults(Path directory)
	{
		boolean correctness = checkDirectory(directory)
		correctness = correctness && checkDirectory(directory.resolve("subdirectory"))

		return correctness
	}

	private static boolean checkDirectory(Path directory)
	{
		boolean correctness = checkFileExistance(directory.resolve("doc.pdf"))
		correctness = correctness && checkFileExistance(directory.resolve("docx.pdf"))

		return correctness
	}

	private static boolean checkFileExistance(Path file)
	{
		boolean existance = Files.exists(file)

		if (!existance)
		{
			println("The expected file is not found: ${file}")
		}

		return existance
	}
}
