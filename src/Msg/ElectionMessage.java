package Msg;

public class ElectionMessage extends Message {

	public ElectionMessage(int senderPort) {
		// TODO Auto-generated constructor stub
		super(senderPort);
	}

	public String toString() {
        return String.format("selecteion start from", num);
    }
}