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

package ru.myllyenko.gradle.plugins.documents.converter

import org.apache.tools.ant.taskdefs.condition.Os
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
@Title("Document Converter Plugin Specification")
@Timeout(value = 20, unit = TimeUnit.SECONDS)
final class DocumentConverterPluginSpecification extends Specification
{
	private static final Collection<File> CLASSPATH = System.properties."test.classpath".split(";").collect { new File(it) }

	private static final String[] COMMON_ARGUMENTS = ["--stacktrace", "--info"]

	private static final String SUCCESSFUL_BUILD_MESSAGE = "BUILD SUCCESSFUL"
	private static final String FAILED_BUILD_MESSAGE = "BUILD FAILED"

	private static final List<String> TARGET_GRADLE_VERSIONS = ["2.10", "2.11"]
	private static final List<Boolean> BOOLEAN_VALUES = [true, false]

	@Unroll
	void "Gradle #gradleVersion, useLocalMsWord = #useLocalMsWord"(String gradleVersion, boolean useLocalMsWord)
	{
		Path projectDirectory = Files.createDirectories(Paths.get("${gradleVersion}-${useLocalMsWord}"))
		Path destinationDirectory = projectDirectory.resolve("pdf")
		String taskName = "msWordToPdf"

		deployDummyProject(projectDirectory, destinationDirectory, taskName, useLocalMsWord)

		GradleRunner gradleRunner = GradleRunner.create().withProjectDir(projectDirectory.toFile()).withDebug(true).forwardOutput().
				withPluginClasspath(CLASSPATH).withArguments((COMMON_ARGUMENTS + [taskName]).flatten()).withGradleVersion(gradleVersion)

		boolean expectSuccess = Os.isFamily(Os.FAMILY_WINDOWS) || !useLocalMsWord

		when:
		BuildResult buildResult = expectSuccess ? gradleRunner.build() : gradleRunner.buildAndFail()

		then:
		if (expectSuccess)
		{
			buildResult.output.contains(SUCCESSFUL_BUILD_MESSAGE)
			checkResults(destinationDirectory)
		}
		else
		{
			buildResult.output.contains(FAILED_BUILD_MESSAGE)
			buildResult.output.contains("The useLocalMsWord parameter set to true is not supported on the current platform")
		}

		where:
		gradleVersion << Collections.nCopies(BOOLEAN_VALUES.size(), TARGET_GRADLE_VERSIONS).flatten().sort()
		useLocalMsWord << Collections.nCopies(TARGET_GRADLE_VERSIONS.size(), BOOLEAN_VALUES).flatten()
	}

	private static void deployDummyProject(Path projectDirectory, Path destinationDirectory, String taskName, boolean useLocalMsWord)
	{
		Path docDirectory = projectDirectory.resolve("doc")
		Path docxDirectory = projectDirectory.resolve("docx")
		Path buildScript = projectDirectory.resolve("build.gradle")

		buildScript.toFile() <<
		"""
			plugins
			{
				id "ru.myllyenko.document-converter"
			}

			task("${taskName}", type: ru.myllyenko.gradle.plugins.documents.converter.MsWordToPdfConversionTask)
			{
				from("${projectDirectory.relativize(docDirectory)}")
				from("${projectDirectory.relativize(docxDirectory)}")
				into("${projectDirectory.relativize(destinationDirectory)}")

				useLocalMsWord = ${useLocalMsWord}
			}
		"""

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
