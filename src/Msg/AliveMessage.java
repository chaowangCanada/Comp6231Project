package Msg;

public class AliveMessage extends Message {

	public AliveMessage(int selectionReplyPort) {
		// TODO Auto-generated constructor stub
		super(selectionReplyPort);
	}

	public String toString() {
        return String.format("Selection continue on", num);
    }
}