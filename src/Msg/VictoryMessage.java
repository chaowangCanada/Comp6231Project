package Msg;

public class VictoryMessage extends Message {

	public VictoryMessage(int coordinatorPort) {
		// TODO Auto-generated constructor stub
		super(coordinatorPort);
	}

	public String toString() {
        return String.format("Coordinater selected", num);
    }
}