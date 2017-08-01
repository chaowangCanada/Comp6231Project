package Config;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * define all the parameter, configuration file
 * @author Chao
 *
 */
public interface PublicParamters {

	/**
	 * Location contains port number, 1 location only can have 1 server port
	 * @author Chao
	 *
	 */
	enum Location{
		MTL(SERVER_PORT_MTL), 
		LVL(SERVER_PORT_LVL), 
		DDO(SERVER_PORT_DDO);
		
		private int port;
		
		Location(int port) {
	        this.port = port;
	    }
	
	    public int getPort() {
	        return port;
	    }
	};
	
		
	enum Specialization {FRENCH, MATHS, SCIENCE};
	enum Course {FRENCH, MATHS, SCIENCE};
	enum Status {ACTIVE, INACTIVE};
	
	// server port cannot be change at run time
	final int SERVER_PORT_MTL = 7000;
	final int SERVER_PORT_LVL = 7001;
	final int SERVER_PORT_DDO = 7002;
	final String ORB_INITIAL_PORT = "1050";
	
	public DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

}
