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
package com.avaje.ebeaninternal.server.transaction;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import com.avaje.ebean.event.BeanPersistListener;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.cluster.BinaryMessage;
import com.avaje.ebeaninternal.server.cluster.BinaryMessageList;
import com.avaje.ebeaninternal.server.core.PersistRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.id.IdBinder;

/**
 * Wraps the information representing a Inserted Updated or Deleted Bean.
 * <p>
 * This information is broadcast across the cluster so that remote BeanListeners
 * are notified of the inserts updates and deletes that occured.
 * </p>
 * <p>
 * You control it the data is broadcast and what data is broadcast by the
 * BeanListener.getClusterData() method. It is guessed that often just the Id
 * property or perhaps a few properties in a Map will be broadcast to reduce the
 * size of data sent around the network.
 * </p>
 */
public class RemoteBeanPersist implements Serializable {

    private static final long serialVersionUID = 8389469180931531409L;

    private final transient BeanDescriptor<?> beanDescriptor;

    private final String descriptorId;

    private ArrayList<Serializable> insertIds;
    private ArrayList<Serializable> updateIds;
    private ArrayList<Serializable> deleteIds;

    /**
     * Create the payload.
     */
    public RemoteBeanPersist(BeanDescriptor<?> desc) {
        this.beanDescriptor = desc;
        this.descriptorId = desc.getDescriptorId();
    }

    public static RemoteBeanPersist readBinaryMessage(SpiEbeanServer server, DataInput dataInput) throws IOException {

        String descriptorId = dataInput.readUTF();
        BeanDescriptor<?> desc = server.getBeanDescriptorById(descriptorId);
        RemoteBeanPersist bp = new RemoteBeanPersist(desc);
        bp.read(dataInput);
        return bp;
    }

    private void read(DataInput dataInput) throws IOException {

        IdBinder idBinder = beanDescriptor.getIdBinder();
        
        int iudType = dataInput.readInt();
        ArrayList<Serializable> idList = readIdList(dataInput, idBinder);
        
        switch (iudType) {
        case 0:
            insertIds = idList;
            break;
        case 1:
            updateIds = idList;
            break;
        case 2:
            deleteIds = idList;
            break;

        default:
            throw new RuntimeException("Invalid iudType "+iudType);
        }
    }

    public void writeBinaryMessage(BinaryMessageList msgList) throws IOException {

        writeIdList(beanDescriptor, 0, insertIds, msgList);
        writeIdList(beanDescriptor, 1, updateIds, msgList);
        writeIdList(beanDescriptor, 2, deleteIds, msgList);
        
    }

//    public void write(DataOutput dataOutput) throws IOException {
//
//        IdBinder idBinder = beanDescriptor.getIdBinder();
//
//        dataOutput.writeUTF(descriptorId);
//
//        writeIdList(dataOutput, idBinder, insertIds);
//        writeIdList(dataOutput, idBinder, updateIds);
//        writeIdList(dataOutput, idBinder, deleteIds);
//    }

    private ArrayList<Serializable> readIdList(DataInput dataInput, IdBinder idBinder) throws IOException {

        int count = dataInput.readInt();
        if (count < 1) {
            return null;
        }
        ArrayList<Serializable> idList = new ArrayList<Serializable>(count);
        for (int i = 0; i < count; i++) {
            Object id = idBinder.readData(dataInput);
            idList.add((Serializable) id);
        }
        return idList;
    }

    private void writeIdList(BeanDescriptor<?> desc, int iudType, ArrayList<Serializable> idList,
            BinaryMessageList msgList) throws IOException {

        IdBinder idBinder = desc.getIdBinder();

        int count = idList == null ? 0 : idList.size();
        if (count > 0) {
            int loop = 0;
            int i = 0;
            int eof = idList.size();
            do {
                ++loop;
                int endOfLoop = Math.min(eof, loop * 100);

                BinaryMessage m = new BinaryMessage(endOfLoop * 4 + 20);
                
                DataOutputStream os = m.getOs();
                os.writeInt(BinaryMessage.TYPE_BEANIUD);
                os.writeUTF(descriptorId);
                os.writeInt(iudType);
                os.writeInt(count);

                for (; i < endOfLoop; i++) {
                    Serializable idValue = idList.get(i);
                    idBinder.writeData(os, idValue);
                }

                os.flush();
                msgList.add(m);

            } while (i < eof);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (beanDescriptor != null) {
            sb.append(beanDescriptor.getFullName());
        }
        sb.append("descriptorId").append(descriptorId);
        if (insertIds != null) {
            sb.append(" insertIds:").append(insertIds);
        }
        if (updateIds != null) {
            sb.append(" updateIds:").append(updateIds);
        }
        if (deleteIds != null) {
            sb.append(" deleteIds:").append(deleteIds);
        }
        return sb.toString();
    }

    public void addId(PersistRequest.Type type, Serializable id) {
        switch (type) {
        case INSERT:
            addInsertId(id);
            break;
        case UPDATE:
            addUpdateId(id);
            break;
        case DELETE:
            addDeleteId(id);
            break;

        default:
            break;
        }
    }

    private void addInsertId(Serializable id) {
        if (insertIds == null) {
            insertIds = new ArrayList<Serializable>();
        }
        insertIds.add(id);
    }

    private void addUpdateId(Serializable id) {
        if (updateIds == null) {
            updateIds = new ArrayList<Serializable>();
        }
        updateIds.add(id);
    }

    private void addDeleteId(Serializable id) {
        if (deleteIds == null) {
            deleteIds = new ArrayList<Serializable>();
        }
        deleteIds.add(id);
    }

    /**
     * Return the Descriptor Id. A more compact alternative to using the
     * beanType.
     */
    public String getDescriptorId() {
        return descriptorId;
    }

    protected ArrayList<Serializable> getInsertIds() {
        return insertIds;
    }

    protected ArrayList<Serializable> getUpdateIds() {
        return updateIds;
    }

    protected ArrayList<Serializable> getDeleteIds() {
        return deleteIds;
    }

    /**
     * Notify the cache and local BeanPersistListener of this event that came
     * from another server in the cluster.
     */
    public void notifyCacheAndListener() {

        BeanPersistListener<?> listener = beanDescriptor.getPersistListener();

        if (insertIds != null) {
            // any insert invalidates the query cache
            beanDescriptor.queryCacheClear();
            if (listener != null) {
                // notify listener
                for (int i = 0; i < insertIds.size(); i++) {
                    listener.remoteInsert(insertIds.get(i));
                }
            }
        }
        if (updateIds != null) {
            for (int i = 0; i < updateIds.size(); i++) {
                Serializable id = updateIds.get(i);

                // remove from cache
                beanDescriptor.cacheRemove(id);
                if (listener != null) {
                    // notify listener
                    listener.remoteInsert(id);
                }
            }
        }
        if (deleteIds != null) {
            for (int i = 0; i < deleteIds.size(); i++) {
                Serializable id = deleteIds.get(i);

                // remove from cache
                beanDescriptor.cacheRemove(id);
                if (listener != null) {
                    // notify listener
                    listener.remoteInsert(id);
                }
            }
        }

    }
}
