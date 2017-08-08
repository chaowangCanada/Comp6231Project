package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import Msg.Message;
 

public class DatagramHelper {


    public static DatagramPacket encodeMessage(Message message, InetAddress addr, int port) throws IOException {
       ByteArrayOutputStream stream = new ByteArrayOutputStream(8192);
       ObjectOutputStream objstream = new ObjectOutputStream(stream);
       objstream.writeObject(message);
       objstream.close();
       return new DatagramPacket(stream.toByteArray(), stream.size(), addr, port);
    }

    public static Message decodeMessage(DatagramPacket packet) throws IOException, ClassCastException, ClassNotFoundException {
        ByteArrayInputStream stream = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
        ObjectInputStream objstream = new ObjectInputStream(stream);
        Message message = (Message)objstream.readObject();
        return message;
    }

    public static DatagramSocket getDatagramSocket() throws SocketException {
        for (int i = 0; i < 100; i++) {
            try {
                return new DatagramSocket(9000 + i);
            } catch (BindException e) {
                continue;
            }
        }
        System.err.println("Exhausted options.");
        System.exit(-1);
        return null;
    }

}
