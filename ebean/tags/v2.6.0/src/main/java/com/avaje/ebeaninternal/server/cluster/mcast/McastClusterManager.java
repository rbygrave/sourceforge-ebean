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
import com.avaje.ebeaninternal.server.cluster.ClusterBroadcast;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.cluster.Packet;
import com.avaje.ebeaninternal.server.cluster.PacketWriter;
import com.avaje.ebeaninternal.server.transaction.RemoteTransactionEvent;

/**
 * Overall Manager of the Multicast Cluster communication for this instance.
 * <p>
 * McastListener, McastSender and McastPacketControl are the main helpers to
 * this object.
 * </p>
 * <p>
 * This Manager (thread) periodically processes the ACK, Re-send and Control
 * messages. The McastListener is handling all the incoming packets and informs
 * this manager when interesting packets need to be processed by the Manager.
 * </p>
 * <p>
 * Other threads call {@link #broadcast(RemoteTransactionEvent)} to send
 * transaction even information.
 * </p>
 * 
 * @author rbygrave
 */
public class McastClusterManager implements ClusterBroadcast, Runnable {

    private static final Logger logger = Logger.getLogger(McastClusterManager.class.getName());

    private ClusterManager clusterManager;

    private final Thread managerThread;

    /**
     * Helps co-ordinate packet information (Acks, Missing Packets etc).
     */
    private final McastPacketControl packageControl;

    /**
     * Listeners for incoming packets.
     */
    private final McastListener listener;

    /**
     * Sends packets out to the cluster.
     */
    private final McastSender localSender;

    /**
     * The localSenderHostPort is used to identify this instance in the cluster.
     */
    private final String localSenderHostPort;
    
    /**
     * Creates the Packets (byte[]) from Messages and RemoteTransactionEvent. 
     */
    private final PacketWriter packetWriter;
    
    /**
     * List of Re-send messages that the managerThread needs to process.
     */
    private final ArrayList<MessageResend> resendMessages = new ArrayList<MessageResend>();

    /**
     * List of Control messages (Ping,PingResponse,Join,Leave) that the managerThread needs to process.
     */
    private final ArrayList<MessageControl> controlMessages = new ArrayList<MessageControl>();
    
    /**
     * Cache of outgoing messages that have not been ACK'ed by the other cluster members yet.
     */
    private final OutgoingPacketsCache outgoingPacketsCache = new OutgoingPacketsCache();

    /**
     * The last ACK we sent out to other members of the cluster.
     */
    private final IncomingPacketsLastAck incomingPacketsLastAck = new IncomingPacketsLastAck();

    /**
     * A limit of the number of times we will try to send out a given packet.
     * Once this is exceeded we will just drop that packet. Hopefully this does
     * not happen but we don't want to keep trying forever producing network
     * traffic.
     */
    private final int maxResendOutgoing;
    
    /**
     * Instead of ACK'ing immediately we periodically wake up and in a single
     * packet (typically) ACK all members of the cluster everything we got since
     * the last sleep time. More frequent ACK's means less memory consumption as
     * Packets are cleared from the outgoingPacketsCache quicker at the cost of
     * sending more packets.
     */
    private long managerSleepMillis;

    /**
     * When true then packets are still sent out even when the cluster has no other online members.
     */
    private boolean sendWithNoMembers;

    /**
     * The current minAcked packetId processed by the managerThread.
     * All packets before this have been ACK'ed by everyone in the cluster.
     */
    private long minAcked;
    
    /**
     * The min packetId that has been ACKed by all the members of the cluster according
     * to the McastListener. This will increase as the Listener receives ACK's and means
     * we can trim out Packets from the sent cache.
     */
    private long minAckedFromListener;
    
    /**
     * Start the groupSize at -1 so we have to wait until the Listener times out or gets 
     * a control messages (Ping, PingResponse, Join, Leave etc) before we know how many
     * members of the group the listener knows about.
     * <p>
     * Generally speaking we only care if the groupSize == 0 meaning there are no other 
     * members of the cluster that are online. In this case we can potentially not send
     * the packets out (depending on sendWithNoMembers) and not cache them (for re-sending
     * if they where not ACK'ed).
     * </p>
     */
    private int currentGroupSize = -1;
    
    /**
     * The last time a packet was sent from this node.
     */
    private long lastSendTime;

    /**
     * The max time we go without sending any packets. 
     */
    private int lastSendTimeFreqMillis;

    /**
     * The last time the cluster status was logged.
     */
    private long lastStatusTime = System.currentTimeMillis();
    
    /**
     * The max time we go before logging the cluster status.
     */
    private int lastStatusTimeFreqMillis;
        
    
    private long totalTxnEventsSent;
    private long totalTxnEventsReceived;
    
    private long totalPacketsSent;
    private long totalBytesSent;
    
    private long totalPacketsResent;
    private long totalBytesResent;
    
    private long totalPacketsReceived;
    private long totalBytesReceived;


    public McastClusterManager() {

        this.managerSleepMillis = GlobalProperties.getInt("ebean.cluster.mcast.managerSleepMillis", 80);
        this.lastSendTimeFreqMillis = 1000*GlobalProperties.getInt("ebean.cluster.mcast.pingFrequencySecs", 300);//5mins
        this.lastStatusTimeFreqMillis = 1000*GlobalProperties.getInt("ebean.cluster.mcast.statusFrequencySecs", 600);//10mins
        
        // the maximum number of times we will try to re-send a given packet before giving up sending
        this.maxResendOutgoing = GlobalProperties.getInt("ebean.cluster.mcast.maxResendOutgoing", 200);        
        // the maximum number of times we will ask for a packet to be resent to us before giving up asking
        int maxResendIncoming = GlobalProperties.getInt("ebean.cluster.mcast.maxResendIncoming", 50);
            
        
        int port = GlobalProperties.getInt("ebean.cluster.mcast.listen.port", 0);
        String addr = GlobalProperties.get("ebean.cluster.mcast.listen.address", null);

        int sendPort = GlobalProperties.getInt("ebean.cluster.mcast.send.port", 0);
        String sendAddr = GlobalProperties.get("ebean.cluster.mcast.send.address", null);

        // Sender options
        // Note 1500 is Ethernet MTU and this must be less than UDP max packet size of 65507
        int maxSendPacketSize = GlobalProperties.getInt("ebean.cluster.mcast.send.maxPacketSize", 1500);
        // Whether to send packets even when there are no other members online
        this.sendWithNoMembers = GlobalProperties.getBoolean("ebean.cluster.mcast.send.sendWithNoMembers", true);
        
        // Listener options
        // When multiple instances are on same box you need to broadcast back locally
        boolean disableLoopback = GlobalProperties.getBoolean("ebean.cluster.mcast.listen.disableLoopback", false);
        int ttl = GlobalProperties.getInt("ebean.cluster.mcast.listen.ttl", -1);
        int timeout = GlobalProperties.getInt("ebean.cluster.mcast.listen.timeout", 1000);
        int bufferSize = GlobalProperties.getInt("ebean.cluster.mcast.listen.bufferSize", 65500);
        // For multihomed environment the address the listener should bind to
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

        this.packetWriter = new PacketWriter(maxSendPacketSize);
        this.localSender = new McastSender(port, addr, sendPort, sendAddr);
        this.localSenderHostPort = localSender.getSenderHostPort();

        this.packageControl = new McastPacketControl(this, localSenderHostPort, maxResendIncoming);
        
        this.listener = new McastListener(this, packageControl, port, addr, bufferSize, timeout, localSenderHostPort,
                disableLoopback, ttl, mcastAddress);
    }


    /**
     * The McastListener tells us there are no other members of the cluster that
     * are currently online.
     */
    protected void fromListenerTimeoutNoMembers() {
        synchronized (managerThread) {
            this.currentGroupSize = 0;
        }
    }
        
    /**
     * McastListener calls this method to get the manager to process messages.
     * 
     * @param newMinAcked
     *            the minAcked packetId according to the listener
     * @param msgControl
     *            a control message to process
     * @param msgResend
     *            a Please re-send message to process
     * @param groupSize
     *            the number of other online members
     */
    protected void fromListener(long newMinAcked, MessageControl msgControl, MessageResend msgResend, 
            int groupSize, long totalPacketsReceived, long totalBytesReceived, long totalTxnEventsReceived) {
        
        synchronized (managerThread) {
            if (newMinAcked > minAckedFromListener){
                minAckedFromListener = newMinAcked;
            }
            if (msgControl != null){
                controlMessages.add(msgControl);                
            }
            if (msgResend != null){
                resendMessages.add(msgResend);                
            }
            // mostly interested when groupSize hits 0 (we are the only instance online).
            this.currentGroupSize = groupSize;
            
            // and some stats so we know how busy the listener has been
            this.totalPacketsReceived = totalPacketsReceived;
            this.totalBytesReceived = totalBytesReceived;
            this.totalTxnEventsReceived = totalTxnEventsReceived;
        }
    }
    
    /**
     * Get the overall status and activity of this cluster node.
     */
    public McastStatus getStatus(boolean reset) {
        synchronized (managerThread) {
            
            long currentPacketId = packetWriter.currentPacketId();
            String lastAcks = incomingPacketsLastAck.toString();

            return new McastStatus(currentGroupSize, outgoingPacketsCache.size(), currentPacketId, minAcked, lastAcks,
                    totalTxnEventsSent, totalTxnEventsReceived, totalPacketsSent, totalPacketsResent, totalPacketsReceived,
                    totalBytesSent, totalBytesResent, totalBytesReceived);

        }
    }    
    
    /**
     * Periodically send out Ack, Re-send and Control messages. 
     */
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

                    if (currentGroupSize == 0){
                        // no members online so trim the entire outgoing packets cache
                        int trimmedCount = outgoingPacketsCache.trimAll();
                        if (trimmedCount > 0){
                            logger.fine("Cluster has no other members. Trimmed "+trimmedCount);
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
                    
                    if (ackResendMessages.size() > 0){
                        // send the ACK and RESEND messages for all members of the
                        // cluster typically in a single Packet
                        if (sendMessages(false, ackResendMessages.getMessages())) {
                            // update the last Ack position
                            incomingPacketsLastAck.updateLastAck(ackResendMessages);
                        }
                    }
                    
                    if (lastSendTime < System.currentTimeMillis() - lastSendTimeFreqMillis){
                        // been quite for too long - send a Ping out
                        sendPing();
                    }         
                    
                    if (lastStatusTimeFreqMillis > 0){
                        if (lastStatusTime < System.currentTimeMillis() - lastStatusTimeFreqMillis){
                            McastStatus status = getStatus(false);
                            logger.info("Cluster Status: "+status.getSummary());
                            lastStatusTime = System.currentTimeMillis();
                        }    
                    }
                    
                }
            } catch (Exception e){
                String msg = "Error with Cluster Mcast Manager thread";
                logger.log(Level.SEVERE, msg, e);
            }
        }
    }

    /**
     * We have been asked to Re-send some packets.
     */
    private void handleResendMessages() {

        if (resendMessages.size() > 0){
            
            TreeSet<Long> s = new TreeSet<Long>(); 
            for (int i = 0; i < resendMessages.size(); i++) {
                MessageResend resendMsg = resendMessages.get(i);
                s.addAll(resendMsg.getResendPacketIds());
            }
            
            totalPacketsResent += s.size();
            
            Iterator<Long> it = s.iterator();
            while (it.hasNext()) {
                Long resendPacketId = it.next();
                Packet packet = outgoingPacketsCache.getPacket(resendPacketId);
                if (packet == null){
                    String msg = "Cluster unable to resend packet["+resendPacketId+"] as it is no longer in the outgoingPacketsCache";
                    logger.log(Level.SEVERE, msg);
                } else {
                    int resendCount = packet.incrementResendCount();
                    if (resendCount <= maxResendOutgoing) {
                        resendPacket(packet);
                    } else {                        
                        String msg = "Cluster maxResendOutgoing ["+maxResendOutgoing+"] hit for packet "+resendPacketId
                            +". We will not try to send it anymore, removing it from the outgoingPacketsCache.";
                        logger.log(Level.SEVERE, msg);
                        outgoingPacketsCache.remove(packet);
                    }
                }
            }   
        }
    }
    
    /**
     * Re-send a packet that a member didn't seem to receive.
     */
    private void resendPacket(Packet packet) {
        try {
            ++totalPacketsResent;
             totalBytesResent += localSender.sendPacket(packet);
        } catch (IOException e) {
            String msg = "Error trying to resend packet "+packet.getPacketId();
            logger.log(Level.SEVERE, msg, e);
        }        
    }
        
    /**
     * Handle Control messages (Join, Leave, Ping).
     */
    private void handleControlMessages() {
        
        boolean pingReponse = false;
        boolean joinReponse = false;
        
        for (int i = 0; i < controlMessages.size(); i++) {
            MessageControl message = controlMessages.get(i);

            short type = message.getControlType();
            switch (type) {                
            case MessageControl.TYPE_JOIN:
                // a new member online, send back a Join Response
                logger.info("Cluster member Joined ["+message.getFromHostPort()+"]");
                joinReponse = true;                       
                break;

            case MessageControl.TYPE_JOINRESPONSE:
                logger.info("Cluster member Online ["+message.getFromHostPort()+"]");
                // do nothing                            
                break;
                
            case MessageControl.TYPE_PING:
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
        
        if (joinReponse){
            sendJoinResponse();
        }
        if (pingReponse){
            sendPingResponse();
        }
    }

    /**
     * Say 'Leaving' and shutdown.
     */
    public void shutdown() {
        sendLeave();
        listener.shutdown();
    }

    /**
     * Startup listeners and 'Join'.
     */
    public void startup(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        listener.startListening();

        this.managerThread.setDaemon(true);
        this.managerThread.start();

        sendJoin();
    }

    protected SpiEbeanServer getEbeanServer(String serverName) {
        return (SpiEbeanServer) clusterManager.getServer(serverName);
    }

    private void sendJoin() {
        sendControlMessage(true, MessageControl.TYPE_JOIN);
    }

    private void sendLeave() {
        sendControlMessage(false, MessageControl.TYPE_LEAVE);
    }

    private void sendJoinResponse() {
        sendControlMessage(true, MessageControl.TYPE_JOINRESPONSE);
    }

    private void sendPingResponse() {
        sendControlMessage(true, MessageControl.TYPE_PINGRESPONSE);
    }
    
    private void sendPing() {
        sendControlMessage(true, MessageControl.TYPE_PING);
    }

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

                List<Packet> packets = packetWriter.write(requiresAck, messages);
                sendPackets(requiresAck, packets);
                return true;
                
            } catch (IOException e) {
                String msg = "Error sending Messages " + messages;
                logger.log(Level.SEVERE, msg, e);
                return false;
            }
        }
    }
        
    private boolean sendPackets(boolean requiresAck, List<Packet> packets) throws IOException {
        if (currentGroupSize == 0 && !sendWithNoMembers) {
            // no other members online so not sending packets
            return false;
            
        } else {
            if (requiresAck){
                // cache them until they have been ACK'ed
                outgoingPacketsCache.registerPackets(packets);
            }
            totalPacketsSent += packets.size();
            totalBytesSent += localSender.sendPackets(packets);
            
            lastSendTime = System.currentTimeMillis();
            
            return true;
        } 
    }
         
    /**
     * Send the remoteTransEvent to all the other members of the cluster.
     */
    public void broadcast(RemoteTransactionEvent remoteTransEvent) {

        synchronized (managerThread) {
            try {
                List<Packet> packets = packetWriter.write(remoteTransEvent);
                if (sendPackets(true, packets)){
                    ++totalTxnEventsSent;                    
                }
            } catch (IOException e) {
                String msg = "Error sending RemoteTransactionEvent " + remoteTransEvent;
                logger.log(Level.SEVERE, msg, e);
            }
        }
    }
    
    /**
     * Effectively set the frequency by which this manager will send out ACKs.
     */
    public void setManagerSleepMillis(long managerSleepMillis) {
        synchronized (managerThread) {
            this.managerSleepMillis = managerSleepMillis;
        }
    }

    /**
     * Return the frequency by which this manager will send out ACKs.
     */
    public long getManagerSleepMillis() {
        synchronized (managerThread) {
            return managerSleepMillis;
        }
    }
    
}
