package Msg;

import java.io.Serializable;

public class Message implements Serializable {
    public long num;
    public String msg;
    
    public Message(){
    	num= 0;
    	msg = "";
    }
    
    public Message(int port){
    	num = port;
    }
    public String toString() {
        return String.format("Message %d: %s", num, msg);
    }
}
