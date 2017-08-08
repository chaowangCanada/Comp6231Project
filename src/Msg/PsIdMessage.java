package Msg;

public class PsIdMessage extends Message {

	public PsIdMessage(String psid, int port) {
		// TODO Auto-generated constructor stub
		super(psid, port);
	}

	public String toString() {
        return super.msg;
    }
}