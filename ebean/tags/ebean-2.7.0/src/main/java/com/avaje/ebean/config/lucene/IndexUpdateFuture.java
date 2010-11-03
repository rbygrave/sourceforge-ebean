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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Future returned when indexes are updated.
 * 
 * @author rbygrave
 */
public interface IndexUpdateFuture {

    /**
     * Return the type of the bean index being updated.
     */
    public Class<?> getBeanType();

    /**
     * Return true if the update is cancelled.
     */
    public boolean isCancelled();

    public boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Return the number of entries updated.
     */
    public Integer get() throws InterruptedException, ExecutionException;

    /**
     * Return the number of entries updated with a timeout.
     */
    public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Return true if the update is finished.
     */
    public boolean isDone();

}