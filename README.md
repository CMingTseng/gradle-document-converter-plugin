# Gradle Document Converter Plugin
This Gradle plugin provides document conversion facilities in the form of `Copy`-like Gradle tasks.

Currently provided task types:

*   [`ru.myllyenko.gradle.plugins.documents.converter.MsWordToPdfConversionTask`]: converts Microsoft Word documents to PDF.

## MsWordToPdfConversionTask

It processes input files as a Copy task configured in the following way:

*   `include("**/*.doc")`;
*   `include("**/*.docx")`;
*   `rename("(.+)\\.docx?$", "$1.pdf")`.

By default on Windows systems a local MS Word instance is used for conversion as described [here](http://www.suodenjoki.dk/us/productions/articles/word2pdf.htm). This can be disabled by setting the `useLocalMsWord` parameter to `false`.

On other operating systems and when `useLocalMsWord` is set to `false`, conversion is performed by means of third-party Java libraries: Apache POI (`doc` -> `HWPF` -> `FO` -> `PDF`) and Apache POI + xdocreport (`docx` -> `XWPF` -> `PDF`).

Configuration example:

    task("wordToPdf", type: ru.myllyenko.gradle.plugins.documents.converter.MsWordToPdfConversionTask)
	{
		from("src/main/word")
		into("build/docs/pdf")

		useLocalMsWord = false
	}

## Release Notes

### 1.0.0 (2016-03-07)

First version.
