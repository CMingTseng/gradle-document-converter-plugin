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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 *
 *
 * @author <a href="mailto:myllyenko@ya.ru">Igor Melnichenko</a>
 */
public class DocumentConverterPlugin implements Plugin<Project>
{
	//static final String TASK_GROUP = "Document conversion";

	//static final String MS_WORD_TO_PDF_TASK_NAME = "msWordToPdf";

	@Override
	public void apply(Project project)
	{
		//declareMsWordToPdfTask(project);
	}

	/*private static final void declareMsWordToPdfTask(Project project)
	{
		Map<String, Class<? extends AbstractConversionTask>> taskCreationArgument = new HashMap<>(1);
		taskCreationArgument.put(Task.TASK_TYPE, MsWordToPdfConversionTask.class);
		project.task(MS_WORD_TO_PDF_TASK_NAME);
	}*/
}
