/*
 * Copyright 2009 Mario Ivankovits
 *
 *     This file is part of Ebean-idea-plugin.
 *
 *     Ebean-idea-plugin is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Ebean-idea-plugin is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Ebean-idea-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ebean.idea.plugin;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.JDOMExternalizer;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Maintains the per project activate flag and setup the compiler stuff appropriate
 *
 * @author Mario Ivankovits, mario@ops.co.at
 */
public class EbeanActionComponent implements ProjectComponent, JDOMExternalizable
{
	private boolean activated;

	protected final Project project;

	private EbeanWeaveTask ebeanCompiler = new EbeanWeaveTask(this);

	public EbeanActionComponent(Project project)
	{
		this.project = project;
	}

	public void projectOpened()
	{
	}

	public void projectClosed()
	{
		setActivated(false);
	}

	@NotNull
	public String getComponentName()
	{
		return "Ebean Action Component";
	}

	public void initComponent()
	{
	}

	public void disposeComponent()
	{
	}

	public boolean isActivated()
	{
		return activated;
	}

	public void setActivated(boolean activated)
	{
		if (!this.activated && activated)
		{
			setupCompiler();

			getCompilerManager().addAfterTask(ebeanCompiler);
			// getCompilerManager().addCompiler(ebeanCompiler);
		}
		else if (this.activated && !activated)
		{
			resetCompiler();

			// getCompilerManager().removeCompiler(ebeanCompiler);
		}
		this.activated = activated;
	}

	private CompilerManager getCompilerManager()
	{
		return CompilerManager.getInstance(project);
	}

	private void resetCompiler()
	{
	}

	private void setupCompiler()
	{
		// we wrap each and every compiler by our RecentlyCompiledCollector so that we are able to also weave scala or groovy stuff
		// at least, this is the idea, I haven't had the chance to test it

		com.intellij.openapi.compiler.Compiler[] compilers = getCompilerManager().getCompilers(TranslatingCompiler.class); // Compiler.class
		for (int i = 0; i < compilers.length; i++)
		{
			com.intellij.openapi.compiler.Compiler compiler = compilers[i];
			if (compiler instanceof RecentlyCompiledCollector)
			{
				break; // Already wrapped
			}
			else if (compiler instanceof TranslatingCompiler)
			{
				// Wrap regular compiler
				RecentlyCompiledCollector wrappingCompiler = new RecentlyCompiledCollector((TranslatingCompiler) compiler);
				getCompilerManager().removeCompiler(compiler); // Remove real compiler
				getCompilerManager().addCompiler(wrappingCompiler); // Add wrapping ocmpiler
			}
		}
	}

	/**
	 * Read on/off state from IWS file
	 */
	public void readExternal(Element element) throws InvalidDataException
	{
		boolean config = JDOMExternalizer.readBoolean(element, "isActivated");
		setActivated(config);
	}

	/**
	 * Persists on/off state in IWS file
	 */
	public void writeExternal(Element element) throws WriteExternalException
	{
		JDOMExternalizer.write(element, "isActivated", activated);
	}
}
