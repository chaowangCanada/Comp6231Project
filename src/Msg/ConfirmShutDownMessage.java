package Msg;

public class ConfirmShutDownMessage extends Message {

	public ConfirmShutDownMessage(int shutDownPort) {
		// TODO Auto-generated constructor stub
		super(shutDownPort);
	}

	public String toString() {
        return String.format("Shut down process", num);
    }
}