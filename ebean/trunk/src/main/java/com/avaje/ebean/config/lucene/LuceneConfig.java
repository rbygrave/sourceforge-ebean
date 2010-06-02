/**
 * Copyright (C) 2009 Authors
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebean.config.lucene;

import org.apache.lucene.analysis.Analyzer;

import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.GlobalProperties.PropertySource;

public class LuceneConfig {

    protected String baseDirectory;
    
    protected Analyzer defaultAnalyzer;

    protected UseIndex defaultUseIndex;
    
    public Analyzer getDefaultAnalyzer() {
        return defaultAnalyzer;
    }

    public void setDefaultAnalyzer(Analyzer defaultAnalyzer) {
        this.defaultAnalyzer = defaultAnalyzer;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public UseIndex getDefaultUseIndex() {
        return defaultUseIndex;
    }

    public void setDefaultUseIndex(UseIndex defaultUseIndex) {
        this.defaultUseIndex = defaultUseIndex;
    }
    
    public void loadSettings(String serverName){
        
        PropertySource p = GlobalProperties.getPropertySource(serverName);
        
        baseDirectory = p.get("lucene.baseDirectory", "lucene");
        defaultUseIndex = p.getEnum(UseIndex.class, "lucene.useIndex", UseIndex.NO);
    }
    
}
