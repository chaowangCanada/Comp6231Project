package MiddleWare;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.Stack;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import Config.PublicParamters;
import Config.PublicParamters.*;
import DCMS_CORBA.DCMS;
import DCMS_CORBA.DCMSHelper;
import DCMS_CORBA.DCMSPOA;
import Record.StudentRecord;
import Record.TeacherRecord;
import Replica.Replica;

public class FrontEnd extends DCMSPOA{

		public  ArrayList<Replica> replicaList = new ArrayList<Replica>();
		private Replica replica1, replica2, replica3;
		private Replica leader;
		private Queue<String> requestQ = new LinkedList<String>();
		private Stack<String> processStack = new Stack<String>();
		private ORB orb;
		private int replicaID_base= 0;
		
		public FrontEnd() throws IOException{
			replica1 = new Replica(++replicaID_base);
			replica2 = new Replica(++replicaID_base);
			replica3 = new Replica(++replicaID_base);
			
			replicaList.add(replica1);
			replicaList.add(replica2);
			replicaList.add(replica3);
			
			// UDP waiting request thread
			replica1.openUDPListener();
			replica2.openUDPListener();
			replica3.openUDPListener();
			
			new ProcessQueueThread(this){

			}.start();		
		}
		
		
		// thread for while(true) loop, waiting for reply
		private class ProcessQueueThread extends Thread{

			private Replica replica = null;
			
			
			public ProcessQueueThread(Replica server) {
				replica = server;
			}
			
			public ProcessQueueThread(FrontEnd frontEnd) {
				// TODO Auto-generated constructor stub
			}

			@Override
			public void run() {

				DatagramSocket aSocket = null;
				
				while(!requestQ.isEmpty()){
					try{
					aSocket = new DatagramSocket();
					byte[] message = requestQ.poll().getBytes();
					InetAddress aHost = InetAddress.getByName("localhost");
					// ===================================
					// this part need to be modified to leader port number
					int serverPort = PublicParamters.SERVER_PORT_REPLICA0;
					//==========================
					DatagramPacket request = new DatagramPacket(message, message.length, aHost , serverPort);
					aSocket.send(request);
					
					byte[] buffer = new byte[5000];
					DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
					aSocket.receive(reply);

					String str = new String(reply.getData(), reply.getOffset(),reply.getLength());
					System.out.println( str);
					}catch (SocketException e){
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
			}
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
		public void deploy(String[] args) throws Exception {

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
				String name = "chao";
				NameComponent path[] = ncRef.to_name(name);
				ncRef.rebind(path, href);

				System.out.println("front end is up ...");

				// wait for invocations from clients
				orb.run();

			} catch (Exception e) {
				System.err.println("ERROR: " + e);
		        e.printStackTrace(System.out);
			}

			System.out.println("front end Exiting ...");
		}
	

		/**
		 * depend on managerID, dicdie to call which center server.
		 */
		@Override
		public String createTRecord(String managerId, String firstName, String lastName, String address, String phone,
				String specialization, String location) {
			String request = "";
			if(managerId.substring(0, 3).equalsIgnoreCase("mtl")){
				request = "mtl|" + "CT" + "|" + managerId + "|"  + firstName + "|" + lastName + "|" + address + "|" + phone + "|" + specialization + "|" + location;
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("lvl")){
				request = "lvl|" + "CT" + "|" + managerId + "|"  + firstName + "|" + lastName + "|" + address + "|" + phone + "|" + specialization + "|" + location;
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("ddo")){
				request = "ddo|" + "CT" + "|" + managerId + "|"  + firstName + "|" + lastName + "|" + address + "|" + phone + "|" + specialization + "|" + location;
			}
			requestQ.add(request);

			return "request queued";
		}
	
	
		/**
		 * depend on managerID, dicdie to call which center server.
		 */
		@Override
		public String createSRecord(String managerId, String firstName, String lastName, String courseRegistered,
				String status, String statusdate) {
			if(managerId.substring(0, 3).equalsIgnoreCase("mtl")){
				String request = "mtl|" + "CS" + "|" + managerId + "|"  + firstName + "|" + lastName + "|" + courseRegistered + "|" + status + "|" + statusdate;
				requestQ.add(request);
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("lvl")){
				String request = "lvl|" + "CS" + "|" + managerId + "|"  + firstName + "|" + lastName + "|" + courseRegistered + "|" + status + "|" + statusdate ;
				requestQ.add(request);
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("ddo")){
				String request = "ddo|" + "CS" + "|" + managerId + "|"  + firstName + "|" + lastName + "|" + courseRegistered + "|" + status + "|" + statusdate;
				requestQ.add(request);
			}
			return "request queued";
		}
	
	
		/**
		 * depend on managerID, dicdie to call which center server.
		 */
		@Override
		public String getRecordCounts(String managerId) {
			if(managerId.substring(0, 3).equalsIgnoreCase("mtl")){
				String request = "mtl|" + "RC" + "|" + managerId ;
				requestQ.add(request);
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("lvl")){
				String request = "lvl|" + "RC" + "|" + managerId ;
				requestQ.add(request);
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("ddo")){
				String request = "ddo|" + "RC" + "|" + managerId ;
				requestQ.add(request);
			}
			return "request queued";
		}
	
	
		/**
		 * depend on managerID, dicdie to call which center server.
		 */
		@Override
		public String editRecord(String managerId, String recordID, String filedname, String newValue) {
			if(managerId.substring(0, 3).equalsIgnoreCase("mtl")){
				String request = "mtl|" + "ER" + "|" + managerId + "|"  + recordID + "|" + filedname + "|" + newValue ;
				requestQ.add(request);
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("lvl")){
				String request = "lvl|" + "ER" + "|" + managerId + "|"  + recordID + "|" + filedname + "|" + newValue ;
				requestQ.add(request);
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("ddo")){
				String request = "ddo|" + "ER" + "|" + managerId + "|"  + recordID + "|" + filedname + "|" + newValue ;
				requestQ.add(request);
			}
			return "request queued";
		}
	
		/**
		 * depend on managerID, dicdie to call which center server.
		 */
		@Override
		public String transferRecord(String managerId, String recordID, String remoteCenterServerName) {
			if(managerId.substring(0, 3).equalsIgnoreCase("mtl")){
				String request = "mtl|" + "TR" + "|" + managerId + "|"  + recordID + "|" + remoteCenterServerName ;
				requestQ.add(request);
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("lvl")){
				String request = "lvl|" + "TR" + "|" + managerId + "|"  + recordID + "|" + remoteCenterServerName ;
				requestQ.add(request);
			}
			else if(managerId.substring(0, 3).equalsIgnoreCase("ddo")){
				String request = "ddo|" + "TR" + "|" + managerId + "|"  + recordID + "|" + remoteCenterServerName ;
				requestQ.add(request);
			}
			return "request queued";
		}

}


