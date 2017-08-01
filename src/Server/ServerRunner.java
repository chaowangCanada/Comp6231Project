package Server;

import java.util.ArrayList;

import Config.PublicParamters.*;

public class ServerRunner {

	public static ArrayList<CenterServer> serverList = new ArrayList<CenterServer>();
	
	
	public static void main(String args[]){
		try{
			
			CenterServer mtl = new CenterServer(Location.MTL);
			CenterServer lvl = new CenterServer(Location.LVL);
			CenterServer ddo = new CenterServer(Location.DDO); 
			
			serverList.add(mtl);
			serverList.add(lvl);
			serverList.add(ddo);
			
			// UDP waiting request thread
			mtl.openUDPListener();
			lvl.openUDPListener();
			ddo.openUDPListener();
			
			// create registry, Corba binding
			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						mtl.exportServer(args);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			}).start();

			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						lvl.exportServer(args);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			}).start();
			
			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						ddo.exportServer(args);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			}).start();

			System.out.println("Servers are up and running ");

		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

}
