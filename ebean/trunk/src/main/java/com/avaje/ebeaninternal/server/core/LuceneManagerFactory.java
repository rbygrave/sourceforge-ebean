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
package com.avaje.ebeaninternal.server.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.lucene.LuceneConfig;
import com.avaje.ebeaninternal.server.lucene.DefaultLuceneIndexManager;
import com.avaje.ebeaninternal.server.lucene.LuceneIndexManager;

public class LuceneManagerFactory {

    public static LuceneIndexManager createLuceneManager(ServerConfig serverConfig) {

        Analyzer defaultAnalyzer = null;
        String baseDir = null;
        UseIndex defaultUseIndex = null;
        
        LuceneConfig luceneConfig = null;//serverConfig.getLuceneConfig();
        if (luceneConfig != null){
            defaultAnalyzer = null;//luceneConfig.getDefaultAnalyzer();
            baseDir = luceneConfig.getBaseDirectory();  
            defaultUseIndex = luceneConfig.getDefaultUseIndex();
        }
        if (defaultAnalyzer == null){
            defaultAnalyzer = new StandardAnalyzer(Version.LUCENE_30);
        }
        if (defaultUseIndex == null){
            defaultUseIndex = UseIndex.NO;
        }
        if (baseDir == null){
            baseDir = "lucene";
        }
        baseDir = GlobalProperties.evaluateExpressions(baseDir);
                
        return new DefaultLuceneIndexManager(defaultAnalyzer, baseDir, serverConfig.getName(), defaultUseIndex);
    }
}
