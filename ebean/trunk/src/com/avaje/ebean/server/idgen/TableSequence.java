/**
 * Copyright (C) 2006  Robin Bygrave
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
package com.avaje.ebean.server.idgen;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.GlobalProperties;

/**
 * A sequence of Integer ids generated using a table to store the next id.
 * <p>
 * This gets a block of numbers so that it doesn't need to update the database
 * table every time you get the nextId. The default is to get a block of 10
 * numbers at a time.
 * </p>
 */
public class TableSequence {

	private static final Logger logger = Logger.getLogger(TableSequence.class.getName());
	
    /**
     * The number of times to try and get the next block. Occasional concurrency
     * issues will occur in a clustered environment and so we should try a few
     * times before giving up.
     */
    int retryCount = 5;

    /**
     * The name of the sequence. Typically the table name.
     */
    String sequenceName;

    /**
     * The starting sequence value.
     */
    int initialNextId = 1;

    /**
     * The current id value.
     */
    int currentInt = 0;

    /**
     * The last values before we need to get another block.
     */
    int endInt = 0;

    /**
     * The number of sequences cached.
     */
    int stepInt = 10;

    /**
     * The implementation that holds the ebean_sequence table
     */
    EbeanServer ebeanServer;

    /**
     * The number of times the sequence update had a concurrency problem
     */
    int concurrencyErrorCount = 0;

    Object monitor = new Object();

    /**
     * Create the tableSequence.
     */
    public TableSequence(String sequenceName, EbeanServer ebeanServer) {
        this.ebeanServer = ebeanServer;
        this.sequenceName = sequenceName.toLowerCase();
        initialiseStep();
    }
    
    /**
     * Set the stepping block size.
     */
    public void setStep(int step){
        this.stepInt = step;
    }

    private void initialiseStep() {
        try {

            SequenceBean sb = (SequenceBean) Ebean.find(SequenceBean.class, sequenceName);
            if (sb != null && sb.getStep() != null){
                // set the stepping from the database table
                stepInt = sb.getStep().intValue();

            } else {
                // set the stepping from properties file
            	ConfigProperties properties = GlobalProperties.getConfigProperties();
                int stepProp = properties.getIntProperty("ebean.idgen." + sequenceName
                        + ".step", 0);
                if (stepProp == 0) {
                    stepProp = properties.getIntProperty("ebean.idgen.step", 10);
                }

                stepInt = stepProp;
            }
        } catch (Exception ex) {
            // just continue...
        	logger.log(Level.SEVERE, null, ex);
        }
    }

    public int next() {
        synchronized (monitor) {
            if (currentInt == 0) {
                moveBlock();
            } else if (currentInt > endInt) {
                moveBlock();
            } else {
                currentInt++;
            }
            return currentInt;
        }
    }

    private void moveBlock() {
        int s = getNextBlock(stepInt);
        this.currentInt = s;
        this.endInt = s + stepInt - 1;
    }

    /**
     * 
     */
    private int getNextBlock(int stepBlock) {

        for (int i = 0; i < retryCount; i++) {

            Transaction t = ebeanServer.createTransaction();
            try {

                SequenceBean sb = (SequenceBean) ebeanServer.find(SequenceBean.class, sequenceName, t);

                Integer nextId = null;

                if (sb == null) {
                    sb = new SequenceBean();
                    sb.setName(sequenceName);
                    nextId = Integer.valueOf(initialNextId);
                    sb.setNextId(nextId);

                } else {
                    Integer stepInteger = sb.getStep();
                    if (stepInteger != null) {
                        stepBlock = stepInteger.intValue();
                    }
                    nextId = sb.getNextId();
                }

                int saveNextId = nextId.intValue() + stepBlock;
                sb.setNextId(Integer.valueOf(saveNextId));

                ebeanServer.save(sb, t);

                t.commit();

                return nextId.intValue();

            } catch (OptimisticLockException e) {
                // expect this to occur occasionally...
                t.rollback();
                concurrencyErrorCount++;

            } catch (PersistenceException e) {
                // possibly multiple inserts at very similar times occuring
                // occasionally
                t.rollback();
            	logger.log(Level.SEVERE, null, e);
            }
        }
        throw new PersistenceException("Failed [" + retryCount + "] times to get nextId");
    }

}
