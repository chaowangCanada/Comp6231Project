package Msg;

public class HeartbeatMessage extends Message {

	public String toString() {
        return String.format("Heartbeat num %d", num);
    }
}
