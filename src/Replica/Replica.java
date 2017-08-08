package Replica;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import Config.PublicParamters;
import Config.PublicParamters.Location;
import Database.Database;
import FailureDetect.FailureChecker;
import FailureDetect.Heartbeater;
import MiddleWare.FrontEnd;
import Msg.AliveMessage;
import Msg.ConfirmShutDownMessage;
import Msg.ElectionMessage;
import Msg.HeartbeatMessage;
import Msg.Message;
import Msg.ShutDownAcknowledge;
import Msg.VictoryMessage;
import util.DatagramHelper;

public class Replica {

	private ArrayList<Database> databaseList ;
	private Database mtl, lvl, ddo;
	private int id, FEport, SCport;
	public HashMap<Integer, Integer> receiveCount;
	private int countDif;
	private int resetPort = 0;
	private int talkPort= 0 ;
	private int leaderFEPort = 0;
	private int leaderSCPort = 0;
    private InetAddress    addr  = null ; 
    private DatagramSocket psock;
    private MulticastSocket sock;

	public Replica(int id, int FrontEndPort, int betweenPort) throws IOException{
		this.id = id;
		this.FEport = FrontEndPort;
		this.SCport = betweenPort;
		mtl = new Database(Location.MTL, id, this);
		lvl = new Database(Location.LVL, id, this);
		ddo = new Database(Location.DDO, id, this); 
		
		databaseList = new ArrayList<Database>();
		databaseList.add(mtl);
		databaseList.add(lvl);
		databaseList.add(ddo);
		
		receiveCount = new HashMap<Integer, Integer>();
		for(int i : PublicParamters.SERVER_PORT_ARR){
			receiveCount.put(i, 0);
		}
		countDif = 0;
		addr = InetAddress.getByName("224.0.0.4");
		psock = new DatagramSocket(SCport);
        sock = new MulticastSocket(PublicParamters.GROUP_PORT);
		leaderFEPort = PublicParamters.SERVER_PORT_FEND2;
		leaderSCPort = PublicParamters.SERVER_PORT_FEND2;

	}
	


	public void launch() throws IOException, ClassNotFoundException {
		// multi -cast 

        sock.joinGroup(addr);
        
        // uni cast

		this.sendHeartBeat(sock, psock, addr);
 
		//this.logFailureFlag();
				
		class FailureChecker extends TimerTask {
		    public void run() {
		    	countDif = 0;
		    	boolean isPlus = true;
		    	for(Map.Entry<Integer, Integer>pair : receiveCount.entrySet()){
	            	if(pair.getKey() != SCport){
	            		if(isPlus){
	            			countDif += pair.getValue();
	            			talkPort = pair.getKey();
	            			isPlus = false;
	            		}else{
	            			countDif -= pair.getValue();
	            			resetPort = pair.getKey();
	            			isPlus = true;
	            		}
	            	}
	            }
		    	System.out.println(talkPort + " - " +resetPort + " = "+countDif);
//	            for(Map.Entry<Integer, Integer>pair : receiveCount.entrySet()){
//	            	System.out.print(pair.getKey()+" : "+pair.getValue()+"; ");
//	            }
//	            System.out.println();
		    	if(countDif > 5){
		    		DatagramSocket aSocket = null; 
		            try {
		            	aSocket = new DatagramSocket();
		                DatagramPacket pkt = DatagramHelper.encodeMessage(new ConfirmShutDownMessage(resetPort), addr, talkPort);
						aSocket.send(pkt);
					
			            DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
						aSocket.receive(packet);
		                Message message      = DatagramHelper.decodeMessage(packet);
		                if(message instanceof ShutDownAcknowledge){
		                	resetReceiveCount();
		                	System.out.println("reset ReceiveCount");
		                }
		            } catch (IOException | ClassCastException | ClassNotFoundException e) {
		                e.printStackTrace();
		            } finally{
						if(aSocket != null ) 
							aSocket.close();
		            }
		    	}
		    	else if( countDif < -5){
		    		DatagramSocket aSocket = null; 
		            try {
		            	aSocket = new DatagramSocket();
		                DatagramPacket pkt = DatagramHelper.encodeMessage(new ConfirmShutDownMessage(talkPort), addr, resetPort);
						aSocket.send(pkt);
					
			            DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
						aSocket.receive(packet);
		                Message message      = DatagramHelper.decodeMessage(packet);
		                if(message instanceof ShutDownAcknowledge){
		                	resetReceiveCount();
		                	System.out.println("acknowledged , reset ReceiveCount");
		                }
		            } catch (IOException | ClassCastException | ClassNotFoundException e) {
		                e.printStackTrace();
		            } finally{
						if(aSocket != null ) 
							aSocket.close();
		            }
		    	}

		    }

		}
		
		Timer timer = new Timer();
		timer.schedule(new FailureChecker(), 0, 5000);
    	
		receiveMulticastMsg(sock, psock, addr, this.SCport);
		receiveUnicastMsg(sock, psock, addr, this.SCport);

        //sock.leaveGroup(addr);
    }



    public void logFailureFlag() {
        Timer timer = new Timer();
        FailureChecker checker = new FailureChecker(receiveCount);
        timer.schedule(checker, 2000, 4000);
    }

    
    public void sendHeartBeat(MulticastSocket mSock, DatagramSocket dSock, InetAddress groupAddr) throws IOException, ClassCastException, ClassNotFoundException {
        Timer timer = new Timer();
        Heartbeater beater = new Heartbeater(dSock, groupAddr, receiveCount);
        timer.schedule(beater, 1000, 5000);
    }
    
    public void receiveMulticastMsg(MulticastSocket mSock, DatagramSocket dSock, InetAddress groupAddr, int portNum) throws IOException, ClassNotFoundException{
    	new Thread(new Runnable(){

			@Override
			public void run() {

		        while (true) {
		        	try{
			            DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
			            mSock.receive(packet);
			            Message message       = DatagramHelper.decodeMessage(packet);
			            if (message instanceof HeartbeatMessage) {
			                //System.out.printf("receive %s: %s\n", packet.getSocketAddress(), message);
			            	int portTmp = packet.getPort();
			                if(portTmp != portNum){
			                	int count = receiveCount.get(portTmp);
			                	receiveCount.put(packet.getPort(), ++count);
			                }
			            } 
			            else if (message instanceof VictoryMessage) {
			            	// if current replica was leader, swap port
			            	if(SCport == leaderSCPort){
			            		SCport = packet.getPort();
			            		FEport = SCport - 1000;
			            	}
			            }
	                	// if after certain time, no victory message, start election again
		        	} catch(Exception e){
		        		e.printStackTrace();
		        	}
		        }
			}
    		
    	}).start();

    }
    
    public void receiveUnicastMsg(MulticastSocket mSock, DatagramSocket sock, InetAddress groupAddr, int portNum) throws IOException, ClassNotFoundException{
    	new Thread(new Runnable(){
			@Override
			public void run() {

		        while (true) {
		        	try{
			            DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
			            psock.receive(packet);
		                Message message = DatagramHelper.decodeMessage(packet);
		                if( message instanceof ConfirmShutDownMessage){
		                	int talkPort = packet.getPort();
		                	int talkPortCount = receiveCount.get(talkPort);
		                	int diff = receiveCount.get(message.num) - talkPortCount;
		                	if(diff >5 || diff < -5){
		                		System.out.println("Confirm shut down of " + message.num);
			                	resetReceiveCount();
				                DatagramPacket reply = DatagramHelper.encodeMessage(new ShutDownAcknowledge((int) message.num), addr, packet.getPort());
				                psock.send(reply);	
								if((int) message.num == leaderSCPort){
									initialSelection(portNum);
								}
		                	}
		                	else{
				                DatagramPacket reply = DatagramHelper.encodeMessage(new Message(), addr, packet.getPort());
				                psock.send(reply);	
		                	}
		                }
		                else if( message instanceof ElectionMessage){
			                DatagramPacket reply = DatagramHelper.encodeMessage(new AliveMessage(portNum), addr, packet.getPort());
			                psock.send(reply);	
		                	initialSelection(portNum);
		                }
		                else if( message instanceof AliveMessage){
		                	// wait, do not send election message again

		                }
		                
		                // wait certain time , if no alive message, send victory message.
		        	}catch(Exception e){
		        		e.printStackTrace();
		        	}
		        }
			}
    		
			}).start();
    }
    

	public void initialSelection(int portNum) throws IOException {
		if(this.SCport == PublicParamters.SERVER_PORT_REPLICA2 -1){
			System.out.println(this.SCport + " is victory");
	        DatagramPacket pkt = DatagramHelper.encodeMessage(new VictoryMessage(this.SCport), addr, PublicParamters.GROUP_PORT);
	        sock.send(pkt);
	        SCport = PublicParamters.SERVER_PORT_REPLICA2;
	        FEport = PublicParamters.SERVER_PORT_FEND2;
		}
		else{
			for (int i = this.SCport; i < leaderSCPort; i++){
	            DatagramPacket pkt = DatagramHelper.encodeMessage(new ElectionMessage(this.SCport), addr, i);
	            psock.send(pkt);
			}

		}

		
	}


	public void resetReceiveCount(){
		for(int i : PublicParamters.SERVER_PORT_ARR){
			receiveCount.put(i, 0);
		}
	}
	
	// create new thread wrapper class
	public void openUDPListener(){

		new UDPListenerThread(this){

		}.start();
		
		System.out.println("server @" +FEport +" is up.");
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
				aSocket  = new DatagramSocket(FEport);
				byte[] buffer = new byte[1000];
				
				// 5 types of reply, create student, create teacher, getRecordCount, edit record, move teacher record among server
				while(true){
					// server leader
					if(server.FEport == leaderFEPort){
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
								for(int port : PublicParamters.SERVER_PORT_FEND_ARR){
								if(port != server.FEport){
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



	public int getFEport() {
		return FEport;
	}



	public void setFEport(int fEport) {
		FEport = fEport;
	}



	public int getSCport() {
		return SCport;
	}



	public void setSCport(int sCport) {
		SCport = sCport;
	}




	
}
