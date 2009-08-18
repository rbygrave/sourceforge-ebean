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

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Based on the idea of the AspectJ Plugin this class collects all successfully compiled classes
 *
 * @author Mario Ivankovits, mario@ops.co.at
 */
public class RecentlyCompiledCollector implements TranslatingCompiler
{
    public final static Key<List<OutputItem>> RECENTLY_COMPILED = new Key<List<OutputItem>>(RecentlyCompiledCollector.class.getName() + ".RECENTLY_COMPILED");

    private final TranslatingCompiler delegate;

    public RecentlyCompiledCollector(TranslatingCompiler delegate)
    {
        this.delegate = delegate;
    }

    public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext)
    {
        return delegate.isCompilableFile(virtualFile, compileContext);
    }

    public ExitStatus compile(CompileContext compileContext, VirtualFile[] virtualFiles)
    {
        // Perform actual compilation
        ExitStatus exitStatus = delegate.compile(compileContext, virtualFiles);

        // Collect all files which have been compiled
        List<OutputItem> recentlyCompiled = compileContext.getUserData(RECENTLY_COMPILED);
        if (recentlyCompiled == null)
        {
            recentlyCompiled = new ArrayList<OutputItem>();
            compileContext.putUserData(RECENTLY_COMPILED, recentlyCompiled);
        }

        OutputItem[] compiled = exitStatus.getSuccessfullyCompiled();
        recentlyCompiled.addAll(Arrays.asList(compiled));

        return exitStatus;
    }

    @NotNull
    public String getDescription()
    {
        return delegate.getDescription();
    }

    public boolean validateConfiguration(CompileScope compileScope)
    {
        return delegate.validateConfiguration(compileScope);
    }

    public static List<OutputItem> getRecentlyCompiled(final CompileContext compileContext)
    {
        final List<OutputItem> list = compileContext.getUserData(RecentlyCompiledCollector.RECENTLY_COMPILED);
        if (list == null)
        {
            return Collections.emptyList();
        }

        return list;
    }
}
