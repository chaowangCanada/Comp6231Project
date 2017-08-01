package Server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Config.*;
import Config.PublicParamters.*;
import Record.*;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import DCMS_CORBA.*;

/**
 * Server class, using CORBA, and UDP, for server-server communication
 * @author Chao
 *
 */

public class CenterServer {	
	
	private File logFile = null;
	private HashMap<Character, LinkedList<Record>> recordData;  // store Student Record and Teacher Record. Servers doen't share record
	private Location location;
	private int recordCount = 0; 
	private ORB orb;
	
	public CenterServer(Location loc)throws IOException{
		super();
		location = loc;
		logFile = new File(location+"_log.txt");
		if(! logFile.exists())
			logFile.createNewFile();
		else{
			if(logFile.delete())
				logFile.createNewFile();
		}
		recordData = new HashMap<Character, LinkedList<Record>>();
	}
	
	/**
	 * Set ORB function
	 * @param orb_val
	 */
	public void setORB(ORB orb_val) {
	    this.orb = orb_val;
	}
	
	/**
	 *  create registry, corba binding with registry
	 * @throws Exception
	 */
	public void exportServer(String[] args) throws Exception {

		try {
			//initial the port number of 1050;
			Properties props = new Properties();
	        props.put("org.omg.CORBA.ORBInitialPort", PublicParamters.ORB_INITIAL_PORT);
	        
			// create and initialize the ORB
			ORB orb = ORB.init(args, props);

			// get reference to rootpoa & activate the POAManager
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();

			// create servant and register it with the ORB
			this.setORB(orb); 

			// get object reference from the servant
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(this);
			DCMS href = DCMSHelper.narrow(ref);
			    
			// get the root naming context
			// NameService invokes the name service
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			
			// Use NamingContextExt which is part of the Interoperable
			// Naming Service (INS) specification.
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

			// bind the Object Reference in Naming
			String name = location.toString();
			NameComponent path[] = ncRef.to_name(name);
			ncRef.rebind(path, href);

			System.out.println("Center Server " +location.toString()+" is up ...");

			// wait for invocations from clients
			orb.run();

		} catch (Exception e) {
			System.err.println("ERROR: " + e);
	        e.printStackTrace(System.out);
		}

		System.out.println("Center Server " +location.toString()+" Exiting ...");
	}
	

	// create new thread wrapper class
	public void openUDPListener(){

		new UDPListenerThread(this){

		}.start();

	}
	
	// thread for while(true) loop, waiting for reply
	private class UDPListenerThread extends Thread{

		private CenterServer server = null;
		
		private String recordCount ;
		
		public UDPListenerThread(CenterServer threadServer) {
			server = threadServer;
		}
		
		@Override
		public void run() {
			DatagramSocket aSocket = null;

			try {
				aSocket  = new DatagramSocket(server.location.getPort());
				byte[] buffer = new byte[1000];
				
				// 3 types of reply, getRecordCount, move Student Record among server, move teacher record among server
				while(true){
					DatagramPacket request = new DatagramPacket(buffer, buffer.length);
					aSocket.receive(request);
					if(request.getData() != null){
						String requestStr = new String(request.getData(), request.getOffset(),request.getLength());
						if(requestStr.equalsIgnoreCase("RecordCounts")){ 
							server.writeToLog("Receive UDP message for : "+ requestStr );
							recordCount = Integer.toString(server.recordCount);
							DatagramPacket reply = new DatagramPacket(recordCount.getBytes(),recordCount.getBytes().length, request.getAddress(), request.getPort()); 
							aSocket.send(reply);
						}
						else if (requestStr.substring(0, 13).equalsIgnoreCase("TeacherRecord")){
							server.writeToLog("Receive UDP message for creating : "+ requestStr.substring(0, 13));
							String[] info = requestStr.split("&");
							server.createTRecord(info[1], info[2], info[3], info[4], info[5], info[6], info[7]);
							String replyStr = "Successfully create Teatcher Record";
							DatagramPacket reply = new DatagramPacket(replyStr.getBytes(),replyStr.getBytes().length, request.getAddress(), request.getPort()); 
							aSocket.send(reply);
						}
						else if (requestStr.substring(0, 13).equalsIgnoreCase("StudentRecord")){
							server.writeToLog("Receive UDP message for creating : "+ requestStr.substring(0, 13));
							String[] info = requestStr.split("&");
							server.createSRecord(info[1], info[2], info[3], info[4].replaceAll("\\[", "").replaceAll("\\]",""), info[5], info[6]);
							String replyStr = "Successfully create Student Record";
							DatagramPacket reply = new DatagramPacket(replyStr.getBytes(),replyStr.getBytes().length, request.getAddress(), request.getPort()); 
							aSocket.send(reply);
						}
					}
				}
			}catch (SocketException e ){System.out.println("Socket"+ e.getMessage());
			}catch (IOException e) {System.out.println("IO"+e.getMessage());
			}finally { if (aSocket !=null ) aSocket.close();}
		}
	}
		
	/*
	 * (non-Javadoc)
	 * @see Assignment2.DCMSOperation#createTRecord(java.lang.String, java.lang.String, java.lang.String, java.lang.String, Assignment1.PublicParamters.Specialization, Assignment1.PublicParamters.Location)
	 */
	public String createTRecord(String managerId, String firstName, String lastName, String address, 
							  String phone, String specialization, String location) throws IOException {
		if(location.equalsIgnoreCase(this.getLocation().toString()) ){
			this.writeToLog("Manager: "+ managerId + " "+location + " creates Teacher record.");
			Record tchrRecord = new TeacherRecord(firstName, lastName, address, phone, Specialization.valueOf(specialization), Location.valueOf(location));
			if(recordData.get(lastName.charAt(0)) == null){
				recordData.put(lastName.charAt(0), new LinkedList<Record>());
			}
			synchronized(recordData.get(lastName.charAt(0))){ // linked list is not thread safe, need to lock avoid race condition
				if(recordData.get(lastName.charAt(0)).add(tchrRecord)){
					String output = "Manager: "+ managerId + " sucessfully write Teacher record. Record ID: "+tchrRecord.getRecordID();
					this.writeToLog(output);
					recordCount++;
					return output;
				}
			}
		}
		this.writeToLog("failed to write Teacher Record");
		System.out.println("failed to write Teacher Record");
		return "failed to write Teacher Record";
	}
	
	/*
	 * 
	 * @see Assignment2.DCMSInterface#createSRecord(java.lang.String, java.lang.String, Assignment1.PublicParamters.Course, Assignment1.PublicParamters.Status, java.lang.String)
	 */
	public String createSRecord(String managerId, String firstName, String lastName, String courseRegistered, 
								String status, String statusdate) throws IOException{
		this.writeToLog("Manager: "+ managerId + " "+ location.toString() + " creates Student record.");
		Record studntRecord = new StudentRecord(firstName, lastName, Course.valueOf(courseRegistered), Status.valueOf(status), statusdate);
		if(recordData.get(lastName.charAt(0)) == null){
			recordData.put(lastName.charAt(0), new LinkedList<Record>());
		}
		synchronized(recordData.get(lastName.charAt(0))){ // linked list is not thread safe, need to lock avoid race condition
			if(recordData.get(lastName.charAt(0)).add(studntRecord)){
				String output = "Manager: "+ managerId + " Sucessfully write Student record. Record ID: "+studntRecord.getRecordID();
				this.writeToLog(output);
				recordCount++;
				return output;
			}
		}
		this.writeToLog("failed to write Student Record");
		System.out.println("failed to write Student Record");
		return "failed to write Student Record";
	}
	
	
	/*
	 * @return message for manager log
	 * @see Assignment2.DCMSOperation#getRecordCounts()
	 */
	public String getRecordCounts(String managerId) throws IOException{
		this.writeToLog("try to count all record at "+ location.toString());
		String output = this.location.toString() + " " + recordCount + ", ";
		if(ServerRunner.serverList.size() ==1 ){
			return output;
		}
		// send request using multi threading
//		else{
//			ExecutorService pool = Executors.newFixedThreadPool(ServerRunner.serverList.size()-1);
//			List<Future<String>> requestArr = new ArrayList<Future<String>>();
//			for(CenterServer server : ServerRunner.serverList){
//				if(server.getLocation() !=this.getLocation()){
//					Future<String> request = pool.submit(new RecordCountRequest(this));
//					requestArr.add(request);
//				}
//			
//			}
//			
//			for(int i = 0 ; i < requestArr.size(); i++){
//				try {
//					output += requestArr.get(i).get();
//				} catch (InterruptedException | ExecutionException e) {
//					e.printStackTrace();
//				}
//			}
//			pool.shutdown();
//		}
		// send request one by one, no threading
		for(CenterServer server : ServerRunner.serverList){
			if(server.getLocation() !=this.getLocation()){
				output += server.getLocation().toString() + " " + requestRecordCounts(server) + ",";
			}
		}
//		
		return output;
	}



	private class RecordCountRequest implements Callable<String>{
		
		private CenterServer server;
		private String output;

		public RecordCountRequest(CenterServer server){
			this.server = server;
		}
		
		@Override
		public String call() throws Exception {
			DatagramSocket aSocket = null;
			
			try{
				aSocket = new DatagramSocket();
				byte[] message = "RecordCounts".getBytes();
				InetAddress aHost = InetAddress.getByName("localhost");  // since all servers on same machine
				int serverPort = server.getLocation().getPort();
				DatagramPacket request = new DatagramPacket(message, message.length, aHost , serverPort);
				server.writeToLog("UDP message to "+ server.getLocation().toString());
				aSocket.send(request);
				
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(reply);
				server.writeToLog("Receive UDP reply from "+ server.getLocation().toString());
				String str = new String(reply.getData(), reply.getOffset(),reply.getLength());

				return str;
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
			return null;
			
		}
	}
	
	/**
	 * socket programming send message to other server
	 * @param server
	 * @return message to manager log
	 */
	public String requestRecordCounts(CenterServer server){
		DatagramSocket aSocket = null;
		
		try{
			aSocket = new DatagramSocket();
			byte[] message = "RecordCounts".getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");  // since all servers on same machine
			int serverPort = server.getLocation().getPort();
			DatagramPacket request = new DatagramPacket(message, message.length, aHost , serverPort);
			this.writeToLog("UDP message to "+ server.getLocation().toString());
			aSocket.send(request);
			
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			this.writeToLog("Receive UDP reply from "+ server.getLocation().toString());
			String str = new String(reply.getData(), reply.getOffset(),reply.getLength());

			return str;
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
		return null;
		
	}
	
	
	@Override
	public String editRecord(String managerId, String recordID, String fieldName, String newValue) throws IOException{
		this.writeToLog("try to edit record for "+recordID);
		String output = new String();

		if(recordID.substring(0,2).equalsIgnoreCase("TR")){
			if(fieldName.equalsIgnoreCase("address")||
					fieldName.equalsIgnoreCase("phone")||
					fieldName.equalsIgnoreCase("location")){
				output= traverseToEdit(recordID, fieldName, newValue, 't', managerId); // t means teacher record
				this.writeToLog(output);
			} else{
				output ="wrong fieldName";
			}
		} 
		else if(recordID.substring(0,2).equalsIgnoreCase("SR")){
			if(fieldName.equalsIgnoreCase("course")||
					fieldName.equalsIgnoreCase("status")||
					fieldName.equalsIgnoreCase("status Date")){
				output = traverseToEdit(recordID, fieldName, newValue, 's', managerId); // s means student record
				this.writeToLog(output);
			}
			else{
				output ="wrong fieldName";
			}
		}
		else{
			output ="wrong recordID";
			this.writeToLog(output);
		}
		
		return output;

	}
	

	@Override
	public String transferRecord(String managerId, String recordID, String remoteClinicServerName) {

		Iterator it = recordData.entrySet().iterator();
		while(it.hasNext()){
			   Entry entry = (Entry) it.next();
			   LinkedList<Record> recordList = (LinkedList<Record>) entry.getValue();
			   
			   synchronized(recordList){
				   Iterator listIt = recordList.iterator();
				   
				   while(listIt.hasNext()){
					   Record record = (Record) listIt.next();
					   if(record.getRecordID().equalsIgnoreCase(recordID) &&
							   (remoteClinicServerName.equalsIgnoreCase(Location.MTL.toString()) ||
							    remoteClinicServerName.equalsIgnoreCase(Location.LVL.toString()) ||
							    remoteClinicServerName.equalsIgnoreCase(Location.DDO.toString()))){
						   if(! remoteClinicServerName.equalsIgnoreCase(this.location.toString())){
								String output = "Manager: "+ managerId + " change " + recordID +" locaiton to "+ remoteClinicServerName;
								for(CenterServer server : ServerRunner.serverList){
									if(server.getLocation() == Location.valueOf(remoteClinicServerName)){
										if(record instanceof TeacherRecord) 
											((TeacherRecord)record).setLocation(remoteClinicServerName);
								  		requestCreateRecord(server, record, managerId);
								  		listIt.remove();
								  		recordCount --;
									}
								}
								return output;
						   }
						   else{
							   return "cannot transfer record to itself server";
						   }
					   }
				   }
					   
			   }
		}
		return "error while processing record transfer";

	}
	
	/**
	 * since don't have the key, need to traverse hashmap to find the right record
	 * @param recordID
	 * @param fieldName
	 * @param newValue
	 * @param RecordInit
	 * @return
	 */
	private String traverseToEdit(String recordID, String fieldName, String newValue, char RecordInit, String managerID) {
		Iterator it = recordData.entrySet().iterator();
		while(it.hasNext()){
			   Entry entry = (Entry) it.next();
			   LinkedList<Record> recordList = (LinkedList<Record>) entry.getValue();
			   
			   synchronized(recordList){
				   Iterator listIt = recordList.iterator();
				   
				   while(listIt.hasNext()){
					   Record record = (Record) listIt.next();
					   if(record.getRecordID().equalsIgnoreCase(recordID)){
						   if(RecordInit == 't'){
							   if(fieldName.equalsIgnoreCase("address")){
								   ((TeacherRecord)record).setAddress(newValue);
				        	  		return recordID+"'s address is changed to "+((TeacherRecord)record).getAddress();
							   } 
							   else if(fieldName.equalsIgnoreCase("phone")){
								   ((TeacherRecord)record).setPhone(newValue);
				        	  		return recordID+"'s phone is changed to "+((TeacherRecord)record).getPhone();
							   }
							   else if(fieldName.equalsIgnoreCase("location")){
								   newValue = newValue.toUpperCase(); // location are all upper case
								   ((TeacherRecord)record).setLocation(newValue);
				        	  		String output = recordID+"'s location is changed to "+((TeacherRecord)record).getLocation().toString();
				        			for(CenterServer server : ServerRunner.serverList){
				        				if(server.getLocation() == Location.valueOf(newValue)){
						        	  		requestCreateRecord(server, record, managerID);
						        	  		listIt.remove();
						        	  		recordCount --;
				        				}
				        			}
				        	  		return output;
							   }
						   } 
						   else if(RecordInit == 's'){
							   if(fieldName.equalsIgnoreCase("course")){
								   newValue = newValue.toUpperCase(); // course, status are all upper case
								   ((StudentRecord)record).editCourse(newValue);
				        	  		return recordID+"'s course is changed to "+((StudentRecord)record).getCourse();
							   } 
							   else if(fieldName.equalsIgnoreCase("status")){
								   newValue = newValue.toUpperCase(); // course, status are all upper case
								   ((StudentRecord)record).setStatus(newValue);
				        	  		return recordID+"'s status is changed to "+((StudentRecord)record).getStatus().toString();
							   }
							   else if(fieldName.equalsIgnoreCase("status date")){
								   ((StudentRecord)record).setStatusDate(newValue);
				        	  		return recordID+"'s status date is changed to "+((StudentRecord)record).getStatusDate();   
							   }
						   }
						   else{
							   return "RecordId has problem";
						   }
					   }
				   }
			   }
		}

		return "cannot find such record";
	}

	/**
	 * socket programming, request to other server to add a record
	 * record re allocation
	 * @param server
	 * @param record
	 */
	private void requestCreateRecord(CenterServer server, Record record, String managerID) {

		DatagramSocket aSocket = null;
		
		try{
			aSocket = new DatagramSocket();
			String recordString  = "";
		    if(record instanceof TeacherRecord) 
		    	recordString += "TeacherRecord&"+managerID+"&"+record.getFirstName()+"&"+record.getLastName()+"&"+((TeacherRecord)record).getAddress()+
		    					"&"+((TeacherRecord)record).getPhone()+"&"+((TeacherRecord)record).getSpecialization().toString()+
		    					"&"+((TeacherRecord)record).getLocation().toString();
		    if (record instanceof StudentRecord)
		    	recordString += "StudentRecord&"+managerID+"&"+record.getFirstName()+"&"+record.getLastName()+"&"+((StudentRecord)record).getCourse()+
								"&"+((StudentRecord)record).getStatus().toString()+
								"&"+((StudentRecord)record).getStatusDate();
		    System.out.println(recordString);
			byte[] message = recordString.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			int serverPort = server.getLocation().getPort();
			DatagramPacket request = new DatagramPacket(message, message.length, aHost , serverPort);
			aSocket.send(request);
			
			byte[] buffer = new byte[5000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);

			String str = new String(reply.getData(), reply.getOffset(),reply.getLength());
			System.out.println( str);
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
		
	}
	
	/**
	 * log file write always needs to be multrual exclusion
	 * @param str
	 * @throws IOException
	 */
	public synchronized void writeToLog(String str) throws IOException{
		 FileWriter writer = new FileWriter(logFile,true);
		 Date date = new Date();
		 writer.write(PublicParamters.dateFormat.format(date) +" : " + str  +"\n");
		 writer.flush();
		 writer.close();
	}

	public File getLogFile() {
		return logFile;
	}

	public void setLog(File log) {
		this.logFile = log;
	}

	public HashMap<Character, LinkedList<Record>> getRecordData() {
		return recordData;
	}

	public void setRecordData(HashMap<Character, LinkedList<Record>> recordData) {
		this.recordData = recordData;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public int getRecordCount() {
		return recordCount;
	}

	public void setRecordCount(int recordCount) {
		this.recordCount = recordCount;
	}


	
	
}
