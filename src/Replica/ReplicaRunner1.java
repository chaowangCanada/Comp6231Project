package Replica;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner1 {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		Replica replica1 ;
		
		if (args.length > 0){
			replica1 = new Replica(1,Integer.parseInt(args[0])-1000, Integer.parseInt(args[0]));
		} else{
			replica1 = new Replica(1,PublicParamters.SERVER_PORT_FEND1, PublicParamters.SERVER_PORT_REPLICA1);
		}

		replica1.setPsId(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);

		replica1.openUDPListener();

		replica1.launch();

	}

}
