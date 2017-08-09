package Replica;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
import Msg.PsIdMessage;
import Msg.ShutDownAcknowledge;
import Msg.VictoryMessage;
import net.rudp.ReliableServerSocket;
import net.rudp.ReliableSocket;
import net.rudp.ReliableSocketOutputStream;
import util.DatagramHelper;
import util.Operation;

public class Replica {

	public final int leaderFEPort = PublicParamters.SERVER_PORT_FEND2;
	public final int leaderSCPort = PublicParamters.SERVER_PORT_REPLICA2;
	
	private ArrayList<Database> databaseList ;
	private Database mtl, lvl, ddo;
	private int id, FEport, SCport;
	private HashMap<Integer, Integer> receiveCount;
	private HashMap<Integer, String> psIdMap;
	private int countDif;
	private int resetPort = 0;
	private int talkPort= 0 ;
    private InetAddress addr  = null ; 
    private InetAddress Uaddr  = null ; 
    //private DatagramSocket psock;
    private MulticastSocket sock;
    private Timer timer;
    private String psId;

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
		psIdMap = new HashMap<Integer, String>();
		for(int i : PublicParamters.SERVER_PORT_ARR){
			receiveCount.put(i, 0);
			psIdMap.put(i, "");
		}
		countDif = 0;
		addr = InetAddress.getByName("224.0.0.4"); //multicast
		Uaddr = InetAddress.getByName("localhost"); //uni cast address
		//psock = new DatagramSocket(SCport);
        sock = new MulticastSocket(PublicParamters.GROUP_PORT);

		timer = new Timer();
	}
	


	public void launch() throws IOException, ClassNotFoundException {
		// multi -cast 

        sock.joinGroup(addr);
        
        // uni cast
		receiveMulticastMsg(sock, addr, this.SCport);

		//this.logFailureFlag();
		// start heart beat scheduler, and failure detect scheduler.
        startScheduler();
		
		//receiveUnicastMsg
		DatagramSocket pSock = null;

    	try{
    		pSock =new DatagramSocket(SCport);
        	while (true) {
	            DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
	            pSock.receive(packet);
                Message message      = DatagramHelper.decodeMessage(packet);
                if( message instanceof ConfirmShutDownMessage){
            		System.out.println("received lost connect warning from scheduler");
                	int resetPort = message.num;
                	int resetPortCount = receiveCount.get(resetPort);
                	int talkPort = 0;
                	for(int i : receiveCount.keySet()){
                		if(i != resetPort && i != SCport)
                			talkPort = i;
                	}
                	int diff = receiveCount.get(talkPort) - resetPortCount;
                	if(diff >1 || diff < -1){
                		System.out.println("Confirm shut down of " + message.num);
	        	        DatagramPacket pkt = DatagramHelper.encodeMessage(new ShutDownAcknowledge(this.SCport), addr, PublicParamters.GROUP_PORT);
                		System.out.println("broad cast my shut down action ");
	        	        pSock.send(pkt);
                		System.out.println("shutdown my scheduler");
	                	timer.cancel();
	                	resetReceiveCount();
	                	// shut down target machine
						if((int) message.num == leaderSCPort){
	                		System.out.println("start election, since I shut down the leader ");
							initialSelection(SCport, pSock);
						}
						else{
							int shutPort = message.num;
					        // kill leader replica
					        if( !this.psIdMap.get(shutPort).equals("") ){
					            killReplica(shutPort);
					        }
					        int runnerId = shutPort - 8000;
					        Operation.copyDatabase(leaderSCPort - 8000 , runnerId);
					        startReplica(runnerId, shutPort);
					        startScheduler();
						}
                	}
                	else{
		                DatagramPacket reply = DatagramHelper.encodeMessage(new Message(), addr, packet.getPort());
		                pSock.send(reply);	
                	}
                }
                else if( message instanceof ElectionMessage){
            		System.out.println("received election message");
	                DatagramPacket reply = DatagramHelper.encodeMessage(new AliveMessage(SCport), addr, packet.getPort());
	                pSock.send(reply);	
                	initialSelection(SCport, pSock);
                }
                else if( message instanceof AliveMessage){
            		System.out.println("received alive message");
                	// wait, do not send election message again
                }
        	}
                // wait certain time , if no alive message, send victory message.
    	}catch(Exception e){
    		e.printStackTrace();
        }finally{
        	pSock.close();
        }
	
        //sock.leaveGroup(addr);
    }


	class FailureCheckerTask extends TimerTask {
	    public void run() {
			// broad cast my psid, for other process to close mine
	    	DatagramSocket pIdSocket = null; 
			try {
				pIdSocket = new DatagramSocket();
				DatagramPacket packt = DatagramHelper.encodeMessage(new PsIdMessage(psId, SCport), addr, PublicParamters.GROUP_PORT);
				pIdSocket.send(packt);
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally{
				pIdSocket.close();
			}
	        
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
//            for(Map.Entry<Integer, Integer>pair : receiveCount.entrySet()){
//            	System.out.print(pair.getKey()+" : "+pair.getValue()+"; ");
//            }
//            System.out.println();
	    	if(countDif > 3){
	    		DatagramSocket aSocket = null; 
	            try {
	            	aSocket = new DatagramSocket();
	        	    //InetAddress IPAddress = InetAddress.getByName("localhost");
	                DatagramPacket pkt = DatagramHelper.encodeMessage(new ConfirmShutDownMessage(resetPort), Uaddr, talkPort);
            		System.out.println("scheduler send confirmshutdown message");
	                aSocket.send(pkt);
				
	            } catch (IOException | ClassCastException e) {
	                e.printStackTrace();
	            } finally{
					if(aSocket != null ) 
						aSocket.close();
	            }
	    	}
	    	else if( countDif <-3){
	    		DatagramSocket aSocket = null; 
	            try {
	            	aSocket = new DatagramSocket();
	        	    //InetAddress IPAddress = InetAddress.getByName("localhost");
	                DatagramPacket pkt = DatagramHelper.encodeMessage(new ConfirmShutDownMessage(talkPort), Uaddr, resetPort);
					aSocket.send(pkt);

	            } catch (IOException | ClassCastException e) {
	                e.printStackTrace();
	            } finally{
					if(aSocket != null ) 
						aSocket.close();
	            }
	    	}
	    }
	}
	
	private void startScheduler() throws ClassCastException, ClassNotFoundException, IOException{
		this.sendHeartBeat(sock, addr);
		this.setFailureCheck();
	}

	
//    public void logFailureFlag() {
//        FailureChecker checker = new FailureChecker(receiveCount);
//        timer.schedule(checker, 2000, 4000);
//    }

    
    public void sendHeartBeat(MulticastSocket mSock, InetAddress groupAddr) throws IOException, ClassCastException, ClassNotFoundException {
        DatagramSocket sock = DatagramHelper.getDatagramSocket();
        Heartbeater beater = new Heartbeater(sock, groupAddr, this.SCport);
        timer.schedule(beater, 1000, 5000);
    }
    
    public void setFailureCheck(){
    	FailureCheckerTask fcheck = new FailureCheckerTask();
		timer.schedule(fcheck, 0, 5000);
    }
    
	
    public void receiveMulticastMsg(MulticastSocket mSock, InetAddress groupAddr, int portNum) throws IOException, ClassNotFoundException{
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
			            	int portTmp = message.num;
			                if(message.num != portNum){
			                	int count = receiveCount.get(portTmp);
			                	receiveCount.put(portTmp, ++count);
			                }
			            } 
			            else if (message instanceof VictoryMessage) {
			            	if(message.num != SCport){
			            		System.out.println("received victory message");
				            	// if current replica was leader, swap port
				            	if(SCport == leaderSCPort){
				            		SCport = message.num;
				            		FEport = SCport - 1000;
				            	}else{
				            		System.out.println("new leader selected, not me. I am starting new scheduler");
				                	resetReceiveCount();
				            		startScheduler();
				            	}
			            	}
			            }
			            else if (message instanceof ShutDownAcknowledge) {
		            		if(SCport != message.num){
			            		System.out.println("received shutdown notify message. I cancel my scheduler, and reset counter");
			            		timer.cancel();
			                	resetReceiveCount();
		            		}
			            }
		                else if ( message instanceof PsIdMessage){
		                	if(message.num != SCport){
		                    	psIdMap.put(message.num, message.msg);
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
//    
//    public void receiveUnicastMsg(InetAddress groupAddr, int portNum) throws IOException, ClassNotFoundException{
//    	new Thread(new Runnable(){
//			@Override
//			public void run() {
//        		DatagramSocket pSock = null;
//
//	        	try{
//	        		pSock =new DatagramSocket(SCport);
//		        	while (true) {
//			            DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
//			            pSock.receive(packet);
//		                Message message      = DatagramHelper.decodeMessage(packet);
//		                if( message instanceof ConfirmShutDownMessage){
//		                	int talkPort = packet.getPort();
//		                	int talkPortCount = receiveCount.get(talkPort);
//		                	int diff = receiveCount.get(message.num) - talkPortCount;
//		                	if(diff >2 || diff < -2){
//		                		System.out.println("Confirm shut down of " + message.num);
//			                	resetReceiveCount();
//				                DatagramPacket reply = DatagramHelper.encodeMessage(new ShutDownAcknowledge((int) message.num), addr, packet.getPort());
//				                pSock.send(reply);	
//								if((int) message.num == leaderSCPort){
//									initialSelection(portNum, pSock);
//								}
//		                	}
//		                	else{
//				                DatagramPacket reply = DatagramHelper.encodeMessage(new Message(), addr, packet.getPort());
//				                pSock.send(reply);	
//		                	}
//		                }
//		                else if( message instanceof ElectionMessage){
//			                DatagramPacket reply = DatagramHelper.encodeMessage(new AliveMessage(portNum), addr, packet.getPort());
//			                pSock.send(reply);	
//		                	initialSelection(portNum, pSock);
//		                }
//		                else if( message instanceof AliveMessage){
//		                	// wait, do not send election message again
//
//		                }
//		        	}
//		                // wait certain time , if no alive message, send victory message.
//	        	}catch(Exception e){
//	        		e.printStackTrace();
//		        }finally{
//		        	pSock.close();
//		        }
//			}
//    		
//			}).start();
//    }
//    

	public void initialSelection(int portNum, DatagramSocket sock) throws IOException, ClassCastException, ClassNotFoundException {
		if(this.SCport == PublicParamters.SERVER_PORT_REPLICA2 -1){
			System.out.println(this.SCport + " is victory");
	        DatagramPacket pkt = DatagramHelper.encodeMessage(new VictoryMessage(this.SCport), addr, PublicParamters.GROUP_PORT);
	        sock.send(pkt);
	        int myOldPort = this.SCport;
	        // kill leader replica
	        if( !this.psIdMap.get(leaderSCPort).equals("") ){
	            killReplica(leaderSCPort);
	        }
	        SCport = leaderSCPort;
	        FEport = leaderFEPort;
	        // relunch leader server with argument of myOldPort;
	        int runnerId;
	        if(this.id == 1 )
	        	runnerId =2;
	        else
	        	runnerId =1;
	        startReplica(runnerId, myOldPort);
	        // restart my scheduler.
	        startScheduler();
		}
		else{
			for (int i = this.SCport+1; i < leaderSCPort; i++){
	            DatagramPacket pkt = DatagramHelper.encodeMessage(new ElectionMessage(this.SCport), Uaddr, i);
	            System.out.println("send out election message to" + i);
	            sock.send(pkt);
			}

		}

	}
	

    public void startReplica(int id, int oldPort){
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-jar",
                    PublicParamters.JAR_FILE_ROOT_PATH + "\\" +  PublicParamters.JAR_FILE_NAME + id + ".jar",
                    Integer.toString(oldPort)
            );
            Process p = pb.start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));
                    String line;
                    try {
                        while ((line = input.readLine()) != null) {
                            System.out.println(line);
                        }
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            p.getErrorStream()));
                    String line;
                    try {
                        while ((line = input.readLine()) != null) {
                            System.out.println(line);
                        }
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


   public void killReplica(int portNum){
        String cmd = "taskkill /F /PID " + this.psIdMap.get(portNum);
        System.out.println("kill " + portNum + " via pid " + this.psIdMap.get(portNum));
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));
                    String line;
                    try {
                        while ((line = input.readLine()) != null) {
                            System.out.println(line);
                        }
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            p.getErrorStream()));
                    String line;
                    try {
                        while ((line = input.readLine()) != null) {
                            System.out.println(line);
                        }
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
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
		
		System.out.println("server @" +SCport +" is up.");
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
									if(areAllServerGood == false)
										System.out.println("server " + port +" is down.");
									}
								}
								// member server are all reply

								if(msgArr[0].substring(0, 3).equalsIgnoreCase("mtl"))
									replyStr = executeRequest(msgArr, mtl);
								else if(msgArr[0].substring(0, 3).equalsIgnoreCase("lvl"))
									replyStr = executeRequest(msgArr, lvl);
								else if(msgArr[0].substring(0, 3).equalsIgnoreCase("ddo"))
									replyStr = executeRequest(msgArr, ddo);

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
				//InetAddress aHost = InetAddress.getByName("localhost");  // since all servers on same machine
				DatagramPacket request = new DatagramPacket(message, message.length, Uaddr , port);
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



	public String getPsId() {
		return psId;
	}



	public void setPsId(String psId) {
		this.psId = psId;
	}
	
	private class UDPListenThread extends Thread{

		private Replica server = null;
		
		private String recordCount ;
		
		public UDPListenThread(Replica threadServer) {
			server = threadServer;
		}
		
		@Override
		public void run() {
			DatagramSocket aSocket = null;
			try {
			
				// 5 types of reply, create student, create teacher, getRecordCount, edit record, move teacher record among server
				while(true){
					ReliableServerSocket serverSocket = new ReliableServerSocket(FEport);
				    ReliableSocket connectionSocket = (ReliableSocket)serverSocket.accept();
				    InputStreamReader inputStream = new InputStreamReader(connectionSocket.getInputStream());
				    BufferedReader buffReader = new BufferedReader(inputStream);
					String requestStr;
					requestStr = buffReader.readLine();
					// server leader
					if(server.FEport == leaderFEPort){
						if(!requestStr.isEmpty()){
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
									if(areAllServerGood == false)
										System.out.println("server " + port +" is down.");
									}
								}
								// member server are all reply

								if(msgArr[0].substring(0, 3).equalsIgnoreCase("mtl"))
									replyStr = executeRequest(msgArr, mtl);
								else if(msgArr[0].substring(0, 3).equalsIgnoreCase("lvl"))
									replyStr = executeRequest(msgArr, lvl);
								else if(msgArr[0].substring(0, 3).equalsIgnoreCase("ddo"))
									replyStr = executeRequest(msgArr, ddo);

							    ReliableSocket replySocket = new ReliableSocket("localhost", FEport);
							    ReliableSocketOutputStream outputStream = (ReliableSocketOutputStream)replySocket.getOutputStream();
							    OutputStreamWriter outputBuffer = new OutputStreamWriter(outputStream);
							
							    outputBuffer.write(replyStr);
							    outputBuffer.flush();							
							} 
							// non-leader reply udp to leader server
							else{
								
							}
						}
					}
					// other non leader server
					else{
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
							
						    ReliableSocket replySocket = new ReliableSocket("localhost", FEport);
						    ReliableSocketOutputStream outputStream = (ReliableSocketOutputStream)replySocket.getOutputStream();
						    OutputStreamWriter outputBuffer = new OutputStreamWriter(outputStream);
						
						    outputBuffer.write(replyStr);
						    outputBuffer.flush();
						}						
					}
					connectionSocket.close();
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
			    ReliableSocket replySocket = new ReliableSocket("localhost", FEport);
			    ReliableSocketOutputStream outputStream = (ReliableSocketOutputStream)replySocket.getOutputStream();
			    OutputStreamWriter outputBuffer = new OutputStreamWriter(outputStream);
			
			    outputBuffer.write(msg);
			    outputBuffer.flush();

			    InputStreamReader inputStream = new InputStreamReader(replySocket.getInputStream());
			    BufferedReader buffReader = new BufferedReader(inputStream);
				String str =  buffReader.readLine();
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


}