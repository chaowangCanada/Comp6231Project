package FailureDetect;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.TimerTask;

import Config.PublicParamters;
import Msg.HeartbeatMessage;
import util.DatagramHelper;

public class Heartbeater extends TimerTask {
    private InetAddress addr;
    private int portNum = 0;
    private DatagramSocket sock;
    
    public Heartbeater(DatagramSocket dSock, InetAddress groupAddr, int sCport) throws SocketException {
        this.sock = dSock;
        this.addr = groupAddr;
        portNum = sCport;
    }

	public void run() {
        //System.out.println("Sending out heartbeat");
        HeartbeatMessage msg = new HeartbeatMessage();
        msg.num = portNum;
        msg.msg = "HEY";
        try {
            DatagramPacket pkt = DatagramHelper.encodeMessage(msg, addr, PublicParamters.GROUP_PORT);
            sock.send(pkt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
}
