package FailureDetect;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.TimerTask;

import Config.PublicParamters;
import Msg.HeartbeatMessage;
import util.DatagramHelper;

public class Heartbeater extends TimerTask {
    private DatagramSocket sock;
    private InetAddress addr;
    private long lastSentMessage = 0;
    private HashMap<Integer, Integer> ptr;
    
    public Heartbeater(DatagramSocket dSock, InetAddress groupAddr, HashMap<Integer, Integer> flag) {
        this.sock = dSock;
        this.addr = groupAddr;
        this.ptr = flag;
    }

    public void run() {
        //System.out.println("Sending out heartbeat");
        HeartbeatMessage msg = new HeartbeatMessage();
        msg.num = lastSentMessage++;
        msg.msg = "HEY";
        try {
            DatagramPacket pkt = DatagramHelper.encodeMessage(msg, addr, PublicParamters.GROUP_PORT);
            sock.send(pkt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
}
