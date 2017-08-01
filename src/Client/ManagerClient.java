package Client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import Config.PublicParamters;
import Config.PublicParamters.*;
import DCMS_CORBA.DCMS;
import DCMS_CORBA.DCMSHelper;


/**
 * Manager class to create manager access server depends on location. 
 * manager can only access its locaion's server
 * @author Chao
 *
 */

public class ManagerClient {


	protected static int managerIDbase =1000; // static to mark unique manager ID
	private String managerID;	
	private File log = null;
	private DCMS dcms_linker = null;

	public ManagerClient(String args[], Location l) throws IOException, NotBoundException{
		managerID = l.toString() + managerIDbase;
		log = new File(managerID+".txt");
		if(! log.exists())
			log.createNewFile();
		else{
			if(log.delete())
				log.createNewFile();
		}
		managerIDbase++;
		dcms_linker = getServerReferrence(args, managerID);
	}

	/**
	 * This is a local function for check manager format use Regular expression.
	 * @param n_managerID
	 * @return
	 */
	private static Boolean checkManagerIDFormat(String n_managerID){
		String pattern = "^(MTL|LVL|DDO)(\\d{5})$";
		Pattern re = Pattern.compile(pattern,Pattern.CASE_INSENSITIVE);
		Matcher matcher = re.matcher(n_managerID);
		if(matcher.find()){
			return true;
		}else{
			System.err.println("Usage:[MTL,LVL,DDO]+[10000]\n");
			return false;
		}
	}
	
	/**
	 * If managerID is valid, this function is for get the stub of that server.
	 * @param managerID
	 * @return
	 * @throws Exception
	 */
	private DCMS getServerReferrence(String[] args, String managerID){
		try {
			//initial the port number of 1050;
			Properties props = new Properties();
	        props.put("org.omg.CORBA.ORBInitialPort", PublicParamters.ORB_INITIAL_PORT);
	        
			// create and initialize the ORB
			ORB orb = ORB.init(args, props);

			// get the root naming context
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			
			// Use NamingContextExt instead of NamingContext. This is 
			// part of the Interoperable naming Service.  
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			
			if(managerID.substring(0, 3).equalsIgnoreCase("mtl")){
				return DCMSHelper.narrow(ncRef.resolve_str(Location.MTL.toString()));
			}else if(managerID.substring(0, 3).equalsIgnoreCase("lvl")){
				return DCMSHelper.narrow(ncRef.resolve_str(Location.LVL.toString()));
			}else if(managerID.substring(0, 3).equalsIgnoreCase("ddo")){
				return DCMSHelper.narrow(ncRef.resolve_str(Location.DDO.toString()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
		return null;
	}
	
	
	public String getManagerID(){
		return managerID;
	}
	
	/**
	 * eacher manager has its own log, no race condition
	 * @param str
	 * @throws IOException
	 */
	public void writeToLog(String str) throws IOException{
		 FileWriter writer = new FileWriter(log,true);
		 Date date = new Date();
		 writer.write(PublicParamters.dateFormat.format(date) +" : " + str  +"\n");
		 writer.flush();
		 writer.close();
	}
	
	/**
	 * no need, if need to switch manager to different location
	 * @param l
	 * @throws RemoteException 
	 */
	public void changeLocation(Location l) throws RemoteException{
		String tmp = managerID.substring(3);
		managerID = l.toString() + tmp;
	}
	
	/**
	 * manager side call Server createTRecord through orb obj
	 * @param firstName
	 * @param lastName
	 * @param address
	 * @param phone
	 * @param special
	 * @param loc
	 * @throws RemoteException
	 * @throws IOException
	 * @throws NotBoundException
	 */
	public void createTRecord(String firstName, String lastName, String address, 
			  					String phone, Specialization special, Location loc) throws RemoteException, IOException, NotBoundException{
		String result = dcms_linker.createTRecord(this.managerID, firstName, lastName, address, phone, special.toString(), loc.toString());
		System.out.println(result);
		writeToLog(result);
		
	}
	
	/**
	 * manager side call Server createSRecord through orb obj
	 * @param firstName
	 * @param lastName
	 * @param course
	 * @param status
	 * @param statusdate
	 * @throws IOException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public void createSRecord(String firstName, String lastName, Course course, 
								Status status, String statusdate) throws IOException, RemoteException, NotBoundException{
		String reply = dcms_linker.createSRecord(this.managerID, firstName, lastName, course.toString(), status.toString(), statusdate);
		System.out.println(reply);
		writeToLog(reply);
	}
	
	/**
	 * manager side call Server getRecordCounts through interface
	 * @throws IOException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public void getRecordCounts() throws IOException, RemoteException, NotBoundException{
		String reply = dcms_linker.getRecordCounts(this.managerID);
		System.out.println(reply);
		writeToLog(reply);
	}
	
	/**
	 * manager side call Server EditRecord through orb obj
	 * @param recordID
	 * @param fieldName
	 * @param newValue
	 * @throws IOException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public void EditRecord(String recordID, String fieldName, String newValue) throws IOException, RemoteException, NotBoundException{
		String reply = dcms_linker.editRecord(this.managerID,recordID, fieldName, newValue);
		System.out.println(reply);
		writeToLog(reply);
	}

	/**
	 * manager side call Server Transfer record through orb obj
	 * @param recordID
	 * @param fieldName
	 * @param newValue
	 * @throws IOException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public void transferRecord(String recordID, String remoteCenterServerName) throws IOException, RemoteException, NotBoundException{
		String reply = dcms_linker.transferRecord(this.managerID, recordID, remoteCenterServerName);
		System.out.println(reply);
		writeToLog(reply);
	}

	public File getLog() {
		return log;
	}

	public void setLog(File log) {
		this.log = log;
	}
	
	
	
}
