package Replica;

import java.io.IOException;

import Config.PublicParamters;
import MiddleWare.FrontEnd;

public class ReplicaRunner0 {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		Replica replica0 = new Replica(0,PublicParamters.SERVER_PORT_FEND0, PublicParamters.SERVER_PORT_REPLICA0);
		
		replica0.openUDPListener();
		
		replica0.launch();


	}

}
