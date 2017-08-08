package Replica;

import java.io.IOException;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner1 {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		Replica replica1 = new Replica(1,PublicParamters.SERVER_PORT_FEND1, PublicParamters.SERVER_PORT_REPLICA1);
		
		replica1.openUDPListener();

		replica1.launch();

	}

}
