package Replica;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

import Config.PublicParamters.Location;
import Database.Database;

public class Replica {

	private ArrayList<Database> databaseList ;
	private Database mtl, lvl, ddo;
	private int id;

	public Replica(int id) throws IOException{
		this.id = id;
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

//			try {
//				aSocket  = new DatagramSocket(server.location.getPort());
//				byte[] buffer = new byte[1000];
//				
//				// 3 types of reply, getRecordCount, move Student Record among server, move teacher record among server
//				while(true){
//					DatagramPacket request = new DatagramPacket(buffer, buffer.length);
//					aSocket.receive(request);
//					if(request.getData() != null){
//						String requestStr = new String(request.getData(), request.getOffset(),request.getLength());
//						if(requestStr.equalsIgnoreCase("RecordCounts")){ 
//							server.writeToLog("Receive UDP message for : "+ requestStr );
//							recordCount = Integer.toString(server.recordCount);
//							DatagramPacket reply = new DatagramPacket(recordCount.getBytes(),recordCount.getBytes().length, request.getAddress(), request.getPort()); 
//							aSocket.send(reply);
//						}
//						else if (requestStr.substring(0, 13).equalsIgnoreCase("TeacherRecord")){
//							server.writeToLog("Receive UDP message for creating : "+ requestStr.substring(0, 13));
//							String[] info = requestStr.split("&");
//							server.createTRecord(info[1], info[2], info[3], info[4], info[5], info[6], info[7]);
//							String replyStr = "Successfully create Teatcher Record";
//							DatagramPacket reply = new DatagramPacket(replyStr.getBytes(),replyStr.getBytes().length, request.getAddress(), request.getPort()); 
//							aSocket.send(reply);
//						}
//						else if (requestStr.substring(0, 13).equalsIgnoreCase("StudentRecord")){
//							server.writeToLog("Receive UDP message for creating : "+ requestStr.substring(0, 13));
//							String[] info = requestStr.split("&");
//							server.createSRecord(info[1], info[2], info[3], info[4].replaceAll("\\[", "").replaceAll("\\]",""), info[5], info[6]);
//							String replyStr = "Successfully create Student Record";
//							DatagramPacket reply = new DatagramPacket(replyStr.getBytes(),replyStr.getBytes().length, request.getAddress(), request.getPort()); 
//							aSocket.send(reply);
//						}
//					}
//				}
//			}catch (SocketException e ){System.out.println("Socket"+ e.getMessage());
//			}catch (IOException e) {System.out.println("IO"+e.getMessage());
//			}finally { if (aSocket !=null ) aSocket.close();}
		}
	}

	public ArrayList<Database> getDatabaseList() {
		return databaseList;
	}


	public void setDatabaseList(ArrayList<Database> databaseList) {
		this.databaseList = databaseList;
	}

	
}
