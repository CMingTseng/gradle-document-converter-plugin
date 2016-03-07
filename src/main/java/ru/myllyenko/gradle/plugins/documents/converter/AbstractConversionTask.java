package ru.myllyenko.gradle.plugins.documents.converter;

import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.file.copy.DestinationRootCopySpec;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.internal.reflect.Instantiator;

import java.io.File;

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

/**
 *
 *
 * @author <a href="mailto:myllyenko@ya.ru">Igor Melnichenko</a>
 */
public abstract class AbstractConversionTask extends AbstractCopyTask
{
	@Override
	protected CopySpecInternal createRootSpec()
	{
		Instantiator instantiator = getInstantiator();
		FileResolver fileResolver = getFileResolver();

		return instantiator.newInstance(DestinationRootCopySpec.class, fileResolver, super.createRootSpec());
	}

	@Override
	public DestinationRootCopySpec getRootSpec()
	{
		return (DestinationRootCopySpec) super.getRootSpec();
	}

	@OutputDirectory
	public File getDestinationDir()
	{
		return getRootSpec().getDestinationDir();
	}

	public void setDestinationDir(File destinationDir)
	{
		into(destinationDir);
	}
}
