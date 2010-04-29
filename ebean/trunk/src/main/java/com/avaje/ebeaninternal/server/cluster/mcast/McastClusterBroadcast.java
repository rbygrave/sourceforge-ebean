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
package com.avaje.ebeaninternal.server.cluster.mcast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.cluster.BinaryMessageList;
import com.avaje.ebeaninternal.server.cluster.ClusterBroadcast;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.cluster.PacketWriterPingMessage;
import com.avaje.ebeaninternal.server.cluster.PacketWriterRemoteTransactionEvent;
import com.avaje.ebeaninternal.server.transaction.RemoteTransactionEvent;

public class McastClusterBroadcast implements ClusterBroadcast, Runnable {

    private static final Logger logger = Logger.getLogger(McastClusterBroadcast.class.getName());

    private ClusterManager clusterManager;

    private final Thread managerThread;

    private final McastPacketControl packageControl;

    private final McastListener listener;

    private final McastSender localSender;

    private final String localSenderHostPort;

    private final TransEventWriter transEventWriter;
    
    private final PingMessageWriter pingMessageWriter;

    private boolean sendWithNoMembers = true;

    private long packetId;

    private long minAcked;
    
    private long minAckedFromListener;
    
    /**
     * Start the groupSize at -1 so we have to wait until the Listener times out or gets 
     * a control messages (Ping, PingResponse, Join, Leave etc) before we know how many
     * members of the group the listener knows about.
     * <p>
     * Generally speaking we only care if the groupSize == 0 meaning there are no other 
     * members of the cluster that are online. In this case we can potentially not send
     * the packets out (depending on sendWithNoMembers) and not cache them (for resending
     * if they where not ACK'ed).
     * </p>
     */
    private int groupSize = -1;
       
    private long managerSleepMillis = 20;

    int loop;
    
    private int maxResendCount = 100;

    private ArrayList<MessageResend> resendMessages = new ArrayList<MessageResend>();

    private ArrayList<MessageControl> controlMessages = new ArrayList<MessageControl>();
    
    private OutgoingPacketsCache outgoingPacketsCache = new OutgoingPacketsCache();

    private IncomingPacketsLastAck incomingPacketsLastAck = new IncomingPacketsLastAck();
    
    public McastClusterBroadcast() {

        int port = GlobalProperties.getInt("ebean.cluster.mcast.listen.port", 0);
        String addr = GlobalProperties.get("ebean.cluster.mcast.listen.address", null);

        int sendPort = GlobalProperties.getInt("ebean.cluster.mcast.send.port", 0);
        String sendAddr = GlobalProperties.get("ebean.cluster.mcast.send.address", null);

        boolean disableLoopback = GlobalProperties.getBoolean("ebean.cluster.mcast.listen.disableLoopback", true);
        int ttl = GlobalProperties.getInt("ebean.cluster.mcast.listen.ttl", -1);
        String mcastAddr = GlobalProperties.get("ebean.cluster.mcast.listen.mcastAddress", null);

        InetAddress mcastAddress = null;
        if (mcastAddr != null) {
            try {
                mcastAddress = InetAddress.getByName(mcastAddr);
            } catch (UnknownHostException e) {
                String msg = "Error getting Multicast InetAddress for " + mcastAddr;
                throw new RuntimeException(msg, e);
            }
        }

        if (port == 0 || addr == null) {
            String msg = "One of these Multicast settings has not been set. " + "ebean.cluster.mcast.listen.port="
                    + port + ", ebean.cluster.mcast.listen.address=" + addr;

            throw new IllegalArgumentException(msg);
        }

        this.managerThread = new Thread(this, "EbeanClusterMcastManager");

        this.transEventWriter = new TransEventWriter();
        this.pingMessageWriter = new PingMessageWriter();

        this.localSender = new McastSender(port, addr, sendPort, sendAddr);
        this.localSenderHostPort = localSender.getSenderHostPort();

        this.packageControl = new McastPacketControl(this, localSenderHostPort);
        
        // although we make the listener buffer 65500 we should probably 
        // be sending much smaller packets and try to stay under 1000
        int bufferSize = 65500;
        int timeout = 500;

        this.listener = new McastListener(this, packageControl, port, addr, bufferSize, timeout, localSenderHostPort,
                disableLoopback, ttl, mcastAddress);
    }
    
    private void handleResendMessages() {

        if (resendMessages.size() > 0){
            
            TreeSet<Long> s = new TreeSet<Long>(); 
            for (int i = 0; i < resendMessages.size(); i++) {
                MessageResend resendMsg = resendMessages.get(i);
                s.addAll(resendMsg.getResendPacketIds());
            }
            
            Iterator<Long> it = s.iterator();
            while (it.hasNext()) {
                Long resendPacketId = it.next();
                Packet packet = outgoingPacketsCache.getPacket(resendPacketId);
                if (packet == null){
                    
                } else {
                    int resendCount = packet.incrementResendCount();
                    if (resendCount > maxResendCount) {
                        // TODO: Handle maxResendCount
                    } else {
                        try {
                            localSender.sendPacket(packet);
                        } catch (IOException e) {
                            String msg = "Error trying to resend packet "+packet.getPacketId();
                            logger.log(Level.SEVERE, msg, e);
                        }
                    }
                }
            }
            
        }
    }
    
//    Packet createIgnorePacket(Packet originalPacket) {
//        
//        try {
//            BinaryMessageList binaryMsgList = new BinaryMessageList();
//            MessageControl msg = new MessageControl(MessageControl.TYPE_PINGRESPONSE,localSenderHostPort);
//            msg.writeBinaryMessage(binaryMsgList);
//            
//            PacketMessages ignorePacket = PacketMessages.forWrite(originalPacket.getPacketId(), System.currentTimeMillis(), "");
//            ignorePacket.writeBinaryMessage(binaryMsgList.getList().get(0));
//            
//            return ignorePacket;
//            
//        } catch (IOException e){
//            String msg = "Error creating Ignore Packet message";
//            logger.log(Level.SEVERE, msg, e);
//            
//            return null;
//        }
//    }
    
    private void handleControlMessages() {
        
        boolean pingReponse = false;
        
        for (int i = 0; i < controlMessages.size(); i++) {
            MessageControl message = controlMessages.get(i);

            short type = message.getControlType();
            switch (type) {
            case MessageControl.TYPE_PING:
                pingReponse = true;                       
                break;
                
            case MessageControl.TYPE_JOIN:
                pingReponse = true;                       
                break;
                
            case MessageControl.TYPE_PINGRESPONSE:
                // do nothing                            
                break;

            case MessageControl.TYPE_LEAVE:
                // remove member. If/When that member comes back its 
                // packetIds will have been reset 
                incomingPacketsLastAck.remove(message.getFromHostPort());
                break;

            default:
                break;
            }
        }
        controlMessages.clear();
        
        if (pingReponse){
            sendPingResponseMessage();
        }
    }
     
    protected void fromListenerTimeoutNoMembers() {
        synchronized (managerThread) {
            groupSize = 0;
        }
    }
    
    protected void fromListener(long newMin, MessageControl msgControl, MessageResend msgResend, int groupSize) {
        synchronized (managerThread) {
            if (msgControl != null){
                controlMessages.add(msgControl);                
            }
            if (msgResend != null){
                resendMessages.add(msgResend);                
            }
            if (newMin > minAckedFromListener){
                minAckedFromListener = newMin;
            }
            this.groupSize = groupSize;
        }
    }
    
    public void run() {
        while (true) {
            try {
                // sleep for a little bit as we ACK packets periodically
                // rather than immediately. We will typically ACK many 
                // messages from all cluster members in a single Packet
                Thread.sleep(managerSleepMillis);
                
                synchronized (managerThread) {
                                                     
                    handleControlMessages();

                    handleResendMessages();

                    if (groupSize == 0){
                        // no members online so trim the entire outgoing packets cache
                        int trimmedCount = outgoingPacketsCache.trimAll();
                        if (trimmedCount > 0){
                            logger.info("Cluster has no other members. Trimmed "+trimmedCount);
                        }
                        
                    } else if (minAckedFromListener > minAcked){
                        // ACKs have come back so trim send packets cache
                        outgoingPacketsCache.trimAcknowledgedMessages(minAckedFromListener);
                        minAcked = minAckedFromListener;
                    }
                    
                    // Get list of all the ACK messages required to sent since the last time.
                    // This is effectively one ACK message per member of the cluster. The ACK 
                    // message covers all the packets received from the member up to 
                    // the gotAllPoint. 
                    // Also get any RESEND messages asking for packets that we have not
                    // received between the gotAllPoint and the gotMaxPoint.
                    AckResendMessages ackResendMessages = packageControl.getAckResendMessages(incomingPacketsLastAck);

                    if (++loop % 200 == 0){
                        System.out.println(" -- MANAGER "//+managerGroupSize
                            +" ackMessages:"+ackResendMessages+" packetCache:"+outgoingPacketsCache+"  lastAcks: "+incomingPacketsLastAck);
                    }
                    
                    if (ackResendMessages.size() > 0){
                        // send the ACK and RESEND messages for all members of the
                        // cluster typically in a single Packet
                        if (sendMessages(false, ackResendMessages.getMessages())) {
                            // update the last Ack position
                            incomingPacketsLastAck.updateLastAck(ackResendMessages);
                        }
                    }
                }
            } catch (Exception e){
                String msg = "Error with Cluster Mcast Manager thread";
                logger.log(Level.SEVERE, msg, e);
            }
        }
    }
        
    public void shutdown() {
        sendLeaveMessage();
        listener.shutdown();
    }

    public void startup(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        listener.startListening();

        this.managerThread.setDaemon(true);
        this.managerThread.start();

        sendJoinMessage();
    }

    protected SpiEbeanServer getEbeanServer(String serverName) {
        return (SpiEbeanServer) clusterManager.getServer(serverName);
    }

    private void sendJoinMessage() {
        sendControlMessage(true, MessageControl.TYPE_JOIN);
    }

    private void sendLeaveMessage() {
        sendControlMessage(false, MessageControl.TYPE_LEAVE);
    }

    private void sendPingResponseMessage() {
        sendControlMessage(true, MessageControl.TYPE_PINGRESPONSE);
    }
    
//    private void sendPingMessage() {
//        sendControlMessage(true, MessageControl.TYPE_PING);
//    }

    private void sendControlMessage(boolean requiresAck, short controlType) {
        sendMessage(requiresAck, new MessageControl(controlType, localSenderHostPort));
    }

    private void sendMessage(boolean requiresAck, Message msg) {
        ArrayList<Message> messages = new ArrayList<Message>(1);
        messages.add(msg);
        sendMessages(requiresAck, messages);
    }
    
    private boolean sendMessages(boolean requiresAck, List<? extends Message> messages) {

        synchronized (managerThread) {
            try {
                BinaryMessageList binaryMsgList = new BinaryMessageList();
                for (int i = 0; i < messages.size(); i++) {
                    Message message = messages.get(i);
                    message.writeBinaryMessage(binaryMsgList);
                }

                List<Packet> packets = pingMessageWriter.write(requiresAck, binaryMsgList, "");

                if (groupSize != 0 || sendWithNoMembers) {
                    if (requiresAck){
                        outgoingPacketsCache.registerPackets(packets);
                    }
                    localSender.sendPackets(packets);
                }
                return true;
                
            } catch (IOException e) {
                String msg = "Error sending Messages " + messages;
                logger.log(Level.SEVERE, msg, e);
                return false;
            }
        }
    }
        
    public void broadcast(RemoteTransactionEvent remoteTransEvent) {

        synchronized (managerThread) {
            try {
                List<Packet> packets = transEventWriter.write(remoteTransEvent);
                
                if (groupSize != 0 || sendWithNoMembers) {
                    outgoingPacketsCache.registerPackets(packets);
                    localSender.sendPackets(packets);
                }
            } catch (IOException e) {
                String msg = "Error sending RemoteTransactionEvent " + remoteTransEvent;
                logger.log(Level.SEVERE, msg, e);
            }
        }
    }
    
    private long nextId() {
        return ++packetId;
    }

    private class TransEventWriter extends PacketWriterRemoteTransactionEvent {

        @Override
        public long nextPacketId() {
            return nextId();
        }
    }

    private class PingMessageWriter extends PacketWriterPingMessage {

        @Override
        public long nextPacketId() {
            return nextId();
        }
    }
}
