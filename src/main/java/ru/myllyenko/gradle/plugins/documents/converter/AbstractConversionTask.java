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

import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.file.copy.DestinationRootCopySpec;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.internal.reflect.Instantiator;

import java.io.File;

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
