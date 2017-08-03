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
import Replica.Replica;
import MiddleWare.*;

/**
 * Server class, using CORBA, and UDP, for server-server communication
 * @author Chao
 *
 */

public class Database {	
	
	private File logFile = null;
	private HashMap<Character, LinkedList<Record>> recordData;  // store Student Record and Teacher Record. Servers doen't share record
	private Location location;
	private int recordCount = 0; 
	private Replica server;
	
	public Database(Location loc, int id, Replica replica)throws IOException{
		location = loc;
		server = replica;
		logFile = new File(location+Integer.toString(id)+"_log.txt");
		if(! logFile.exists())
			logFile.createNewFile();
		else{
			if(logFile.delete())
				logFile.createNewFile();
		}
		recordData = new HashMap<Character, LinkedList<Record>>();
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
		if(server.getDatabaseList().size() ==1 ){
			return output;
		}
		// send request using multi threading
//		else{
//			ExecutorService pool = Executors.newFixedThreadPool(ServerRunner.serverList.size()-1);
//			List<Future<String>> requestArr = new ArrayList<Future<String>>();
//			for(Database server : ServerRunner.serverList){
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
		for(Database database : server.getDatabaseList()){
			if(database.getLocation() !=this.getLocation()){
				output += database.getLocation().toString() + " " + database.getRecordCount() + ",";
			}
		}
//		
		return output;
	}



	private class RecordCountRequest implements Callable<String>{
		
		private Database server;
		private String output;

		public RecordCountRequest(Database server){
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
	public String requestRecordCounts(Database server){
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
	

	public String transferRecord(String managerId, String recordID, String remoteClinicServerName) throws IOException {

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
								for(Database db : server.getDatabaseList()){
									if(db.getLocation() == Location.valueOf(remoteClinicServerName)){
										if(record instanceof TeacherRecord) 
											db.createTRecord(managerId, record.getFirstName(), record.getLastName(),
													((TeacherRecord)record).getAddress(), ((TeacherRecord)record).getPhone(), 
													((TeacherRecord)record).getSpecialization().toString(), db.getLocation().toString());
										else if(record instanceof StudentRecord) 
											db.createSRecord(managerId, record.getFirstName(), record.getLastName(),
													((StudentRecord)record).getCourse(), ((StudentRecord)record).getStatus().toString(), 
													((StudentRecord)record).getStatusDate());
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
								   ((TeacherRecord)record).setLocation(newValue);
				        	  		return recordID+"'s locaion is changed to " + newValue;
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
