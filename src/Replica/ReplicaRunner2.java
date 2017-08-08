package Replica;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner2 {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		Replica replica2;
		
		if (args.length > 0){
			replica2 = new Replica(2,Integer.parseInt(args[0])-1000, Integer.parseInt(args[0]));
		} else{
			replica2 = new Replica(2,PublicParamters.SERVER_PORT_FEND2, PublicParamters.SERVER_PORT_REPLICA2);
		}
		
		replica2.setPsId(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		
		replica2.openUDPListener();

		replica2.launch();

	}

}
