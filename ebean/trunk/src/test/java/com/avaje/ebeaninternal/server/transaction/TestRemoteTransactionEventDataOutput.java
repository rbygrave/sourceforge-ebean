package com.avaje.ebeaninternal.server.transaction;

import java.io.DataInput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.avaje.ebean.Ebean;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;
import com.avaje.ebeaninternal.server.cluster.BinaryMessage;
import com.avaje.ebeaninternal.server.cluster.BinaryPacketReader;
import com.avaje.ebeaninternal.server.cluster.PacketWriterRemoteTransactionEvent;
import com.avaje.ebeaninternal.server.cluster.mcast.Packet;
import com.avaje.ebeaninternal.server.core.PersistRequest.Type;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.tests.model.basic.Customer;

public class TestRemoteTransactionEventDataOutput extends TestCase {

    public void test() {

        try {
            SpiEbeanServer server = (SpiEbeanServer) Ebean.getServer(null);

            BeanDescriptor<Customer> desc = server.getBeanDescriptor(Customer.class);
            //String descriptorId = desc.getDescriptorId();


            RemoteTransactionEvent transEvent = new RemoteTransactionEvent("");
            transEvent.add(desc, Type.UPDATE, 5);
            transEvent.add(desc,Type.UPDATE, 6);
            transEvent.add(desc,Type.UPDATE, 7);
            //transEvent.add(desc,Type.INSERT, 100);
            //transEvent.add(desc,Type.INSERT, 101);

            WriterTest writerTest = new WriterTest();
            List<Packet> packets = writerTest.write(transEvent);
            
            assertEquals(1, packets.size());
            
            Packet p = packets.get(0);
            byte[] byteArray = p.getBytes();

            System.out.println("Packet Size: "+byteArray.length);
            
            ReaderTest readerTest = new ReaderTest();
            List<Object> objects = readerTest.read(byteArray);
            
            RemoteBeanPersist b = (RemoteBeanPersist)objects.get(0);
            ArrayList<Serializable> updateIds = b.getUpdateIds();
            assertNotNull(updateIds);
            assertEquals(3, updateIds.size());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }
    
    static class WriterTest extends PacketWriterRemoteTransactionEvent {

        long packetId;
        
        @Override
        public long nextPacketId() {
            return ++packetId;
        }
        
    }
    
    static class ReaderTest extends BinaryPacketReader {

        @Override
        public SpiEbeanServer getEbeanServer(String serverName) {
            return (SpiEbeanServer)Ebean.getServer(serverName);
        }

        @Override
        public Object readMessage(DataInput dataInput, int msgType) throws IOException {
            
            if (msgType == BinaryMessage.TYPE_BEANIUD){
                return RemoteBeanPersist.readBinaryMessage(server, dataInput);
            }
            if (msgType == BinaryMessage.TYPE_TABLEIUD){
                return TableIUD.readBinaryMessage(dataInput);
            }
            
            // TODO Auto-generated method stub
            return null;
        }

        
    }
}
