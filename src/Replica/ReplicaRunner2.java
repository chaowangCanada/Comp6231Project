package Replica;

import java.io.IOException;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner2 {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		Replica replica2 = new Replica(2,PublicParamters.SERVER_PORT_FEND2, PublicParamters.SERVER_PORT_REPLICA2);
		
		replica2.openUDPListener();

		replica2.launch();

	}

}
