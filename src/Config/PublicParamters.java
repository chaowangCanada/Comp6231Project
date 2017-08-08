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
		MTL, 
		LVL, 
		DDO;

	};
	
		
	enum Specialization {FRENCH, MATHS, SCIENCE};
	enum Course {FRENCH, MATHS, SCIENCE};
	enum Status {ACTIVE, INACTIVE};
	
	public final static int GROUP_PORT = 8437;

	// server port cannot be change at run time
	final int SERVER_PORT_FEND0 = 7000;  //leader port
	final int SERVER_PORT_FEND1 = 7001;
	final int SERVER_PORT_FEND2 = 7002;
	final int[] SERVER_PORT_FEND_ARR = new int[] {SERVER_PORT_FEND0, SERVER_PORT_FEND1, SERVER_PORT_FEND2};
    final int SERVER_PORT_REPLICA0 = 8000; 
	final int SERVER_PORT_REPLICA1 = 8001;
	final int SERVER_PORT_REPLICA2 = 8002;
	final int[] SERVER_PORT_ARR = new int[] {SERVER_PORT_REPLICA0, SERVER_PORT_REPLICA1, SERVER_PORT_REPLICA2};

	
	final String ORB_INITIAL_PORT = "1050";
	
	public DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

}
