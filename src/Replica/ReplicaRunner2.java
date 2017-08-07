package Replica;

import java.io.IOException;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner2 {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Replica replica1 = new Replica(2, PublicParamters.SERVER_PORT_REPLICA1);
		
		replica1.openUDPListener();

	}

}
