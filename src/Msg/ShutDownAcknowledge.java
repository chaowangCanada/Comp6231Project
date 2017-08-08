package Msg;

public class ShutDownAcknowledge extends Message {

	public ShutDownAcknowledge(int shutDownPort) {
		// TODO Auto-generated constructor stub
		super(shutDownPort);
	}

	public String toString() {
        return String.format("Shut down acknowledge", num);
    }
}