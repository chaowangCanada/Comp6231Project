package Replica;

import java.io.IOException;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner3 {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Replica replica1 = new Replica(3, PublicParamters.SERVER_PORT_REPLICA2);
		
		replica1.openUDPListener();

	}

}
