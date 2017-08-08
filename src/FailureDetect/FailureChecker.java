package FailureDetect;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

public class FailureChecker  extends TimerTask {
	private HashMap<Integer, Integer> flag;

	public FailureChecker(HashMap<Integer, Integer> flag){
		this.flag = flag;
	}
    public void run() {
            for(Map.Entry<Integer, Integer>pair : flag.entrySet()){
            	System.out.print(pair.getKey()+" : "+pair.getValue()+"; ");
            }
            System.out.println();
    	
    }

}
