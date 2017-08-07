package Replica;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Stack;

import Config.PublicParamters;
import Config.PublicParamters.Location;
import Database.Database;
import MiddleWare.FrontEnd;

public class Replica {

	private ArrayList<Database> databaseList ;
	private Database mtl, lvl, ddo;
	private int id, port;

	public Replica(int id, int port) throws IOException{
		this.id = id;
		this.port = port;
		mtl = new Database(Location.MTL, id, this);
		lvl = new Database(Location.LVL, id, this);
		ddo = new Database(Location.DDO, id, this); 
		
		databaseList = new ArrayList<Database>();
		databaseList.add(mtl);
		databaseList.add(lvl);
		databaseList.add(ddo);
	}
	

	// create new thread wrapper class
	public void openUDPListener(){

		new UDPListenerThread(this){

		}.start();
		
		System.out.println("server @" +port +" is up.");
	}
	
	// thread for while(true) loop, waiting for reply
	private class UDPListenerThread extends Thread{

		private Replica server = null;
		
		private String recordCount ;
		
		public UDPListenerThread(Replica threadServer) {
			server = threadServer;
		}
		
		@Override
		public void run() {
			DatagramSocket aSocket = null;
			try {
				aSocket  = new DatagramSocket(port);
				byte[] buffer = new byte[1000];
				
				// 5 types of reply, create student, create teacher, getRecordCount, edit record, move teacher record among server
				while(true){
					// server leader
					if(server.port == PublicParamters.SERVER_PORT_REPLICA0){
						DatagramPacket request = new DatagramPacket(buffer, buffer.length);
						aSocket.receive(request);
						if(request.getData() != null){
							String requestStr = new String(request.getData(), request.getOffset(),request.getLength());
							// front end udp to leader server
							if(requestStr.contains("|")){
								String[] msgArr = requestStr.split("\\|");
								String replyStr = "";
								boolean areAllServerGood = true;
								// send msg to member server first
//								for(Replica rplic : FrontEnd.replicaList){
//									if(rplic.getPort() != server.port){
//										areAllServerGood = sendToMember(requestStr, rplic.getPort());
//									}
//								}
								for(int port : PublicParamters.SERVER_PORT_ARR){
								if(port != server.port){
									areAllServerGood = sendToMember(requestStr, port);
									}
								}
								// member server are all reply
								if(areAllServerGood){
									if(msgArr[0].substring(0, 3).equalsIgnoreCase("mtl"))
										replyStr = executeRequest(msgArr, mtl);
									else if(msgArr[0].substring(0, 3).equalsIgnoreCase("lvl"))
										replyStr = executeRequest(msgArr, lvl);
									else if(msgArr[0].substring(0, 3).equalsIgnoreCase("ddo"))
										replyStr = executeRequest(msgArr, ddo);
								}
								else{
									replyStr = "One of the replica is down.";
								}
								DatagramPacket reply = new DatagramPacket(replyStr.getBytes(),replyStr.getBytes().length, request.getAddress(), request.getPort()); 
								aSocket.send(reply);								
							} 
							// non-leader reply udp to leader server
							else{
								
							}
						}
					}
					// other non leader server
					else{
						DatagramPacket request = new DatagramPacket(buffer, buffer.length);
						aSocket.receive(request);		
						if(request.getData() != null){
							String requestStr = new String(request.getData(), request.getOffset(),request.getLength());
							if(requestStr.contains("|")){

								String[] msgArr = requestStr.split("\\|");
								String replyStr = "";
								if(msgArr[0].substring(0, 3).equalsIgnoreCase("mtl")){
									replyStr = executeRequest(msgArr, mtl);
								}
								else if(msgArr[0].substring(0, 3).equalsIgnoreCase("lvl")){
									replyStr = executeRequest(msgArr, lvl);
								}
								else if(msgArr[0].substring(0, 3).equalsIgnoreCase("ddo")){
									replyStr = executeRequest(msgArr, ddo);
								}
								DatagramPacket reply = new DatagramPacket(replyStr.getBytes(),replyStr.getBytes().length, request.getAddress(), request.getPort()); 
								aSocket.send(reply);
	
							}						
						}
					}
				}
			}catch (IOException e ){System.out.println("Socket"+ e.getMessage());
			}finally { if (aSocket !=null ) aSocket.close();}
		}

		private String executeRequest(String[] msgArr, Database db) throws IOException {
			if(msgArr[1].equalsIgnoreCase("CT")){
				return db.createTRecord(msgArr[0], msgArr[2], msgArr[3], msgArr[4], msgArr[5], msgArr[6], msgArr[7]);
			}
			else if(msgArr[1].equalsIgnoreCase("CS")){
				return db.createSRecord(msgArr[0], msgArr[2], msgArr[3], msgArr[4], msgArr[5], msgArr[6]);
			}
			else if(msgArr[1].equalsIgnoreCase("RC")){
				return db.getRecordCounts(msgArr[0]);
			}
			else if(msgArr[1].equalsIgnoreCase("ER")){
				return db.editRecord(msgArr[0], msgArr[2], msgArr[3], msgArr[4]);
			}
			else if(msgArr[1].equalsIgnoreCase("TR")){
				return db.transferRecord(msgArr[0], msgArr[2], msgArr[3]);
			}
			return "error of request command";
		}
		
		private boolean sendToMember(String msg, int port){
			DatagramSocket aSocket = null;
			
			try{
				aSocket = new DatagramSocket();
				byte[] message = msg.getBytes();
				InetAddress aHost = InetAddress.getByName("localhost");  // since all servers on same machine
				DatagramPacket request = new DatagramPacket(message, message.length, aHost , port);
				aSocket.send(request);
				
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(reply);
				String str = new String(reply.getData(), reply.getOffset(),reply.getLength());
				if(str.isEmpty())
					return false;
				return true;
			}
			catch (SocketException e){
				System.out.println("Socket"+ e.getMessage());
			}
			catch (IOException e){
				System.out.println("IO: "+e.getMessage());
			}
			finally {
				if(aSocket != null ) 
					aSocket.close();
			}
			return false;
			
		}
	}

	public ArrayList<Database> getDatabaseList() {
		return databaseList;
	}


	public void setDatabaseList(ArrayList<Database> databaseList) {
		this.databaseList = databaseList;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public int getPort() {
		return port;
	}


	public void setPort(int port) {
		this.port = port;
	}

	
}
