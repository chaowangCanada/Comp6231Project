package Replica;

import java.io.IOException;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner1 {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Replica replica1 = new Replica(1, PublicParamters.SERVER_PORT_REPLICA0);
		
		replica1.openUDPListener();

	}

}
