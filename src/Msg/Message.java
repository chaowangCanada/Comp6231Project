package Msg;

import java.io.Serializable;

public class Message implements Serializable {
    public int num;
    public String msg;
    
    public Message(){
    	num= 0;
    	msg = "";
    }
    
    public Message(String str){
    	msg= str;
    }
    
    public Message(String str, int port){
    	msg= str;
    	num = port;
    }
    
    public Message(int port){
    	num = port;
    }
    public String toString() {
        return String.format("Message %d: %s", num, msg);
    }
}
