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
package com.avaje.ebeaninternal.server.lucene;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.avaje.ebean.config.lucene.IndexUpdateFuture;

public class LIndexUpdateFuture implements Future<Integer>, IndexUpdateFuture {
    
    private final Class<?> beanType;
    private final Runnable commitRunnable;
    private final FutureTask<Void> commitFuture;
    private FutureTask<Integer> task;
    
    public LIndexUpdateFuture(Class<?> beanType) {
        this.beanType = beanType;
        this.commitRunnable = new DummyRunnable();
        this.commitFuture = new FutureTask<Void>(commitRunnable, null);
    }
    
    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.lucene.IndexUpdFuture#getBeanType()
     */
    public Class<?> getBeanType() {
        return beanType;
    }
    
    public Runnable getCommitRunnable() {
        return commitFuture;
    }

    public void setTask(FutureTask<Integer> task) {
        this.task = task;
    }

    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.lucene.IndexUpdFuture#isCancelled()
     */
    public boolean isCancelled() {
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.lucene.IndexUpdFuture#cancel(boolean)
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.lucene.IndexUpdFuture#get()
     */
    public Integer get() throws InterruptedException, ExecutionException {
        commitFuture.get();
        return task.get();
    }

    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.lucene.IndexUpdFuture#get(long, java.util.concurrent.TimeUnit)
     */
    public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        commitFuture.get(timeout, unit);
        return task.get(0, unit);
    }

    /* (non-Javadoc)
     * @see com.avaje.ebeaninternal.server.lucene.IndexUpdFuture#isDone()
     */
    public boolean isDone() {
        return commitFuture.isDone();
    }
    
    private static class DummyRunnable implements Runnable {
        public void run(){
            System.out.println("-- dummy runnable");
        }
    }

}
