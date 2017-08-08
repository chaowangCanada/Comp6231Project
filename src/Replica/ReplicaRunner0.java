package Replica;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner0 {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Replica replica0;
		
		if (args.length > 0){
			replica0 = new Replica(0,Integer.parseInt(args[0])-1000, Integer.parseInt(args[0]));
		} else{
			replica0 = new Replica(0,PublicParamters.SERVER_PORT_FEND0, PublicParamters.SERVER_PORT_REPLICA0);
		}

		replica0.setPsId(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		
		replica0.openUDPListener();
		
		replica0.launch();


	}

}
