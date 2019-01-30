import java.net.DatagramPacket;
import java.net.DatagramSocket;

import tcdIO.Terminal;

public class EndNode extends Node {
	
	private Terminal terminal;
	private int endNodeNumber;
	private int connectedSwitch;
	EndNode(Terminal terminal, int endNodeNumber, int connectedSwitch) {
		this.terminal = terminal;
		this.endNodeNumber = endNodeNumber;
		this.connectedSwitch = connectedSwitch;
		try {
			socket = new DatagramSocket(endNodeNumber + Constants.ENDUSER_BASE_PORT);
			listener.go();
		} catch (
		java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("EndNode");
			int endNodeNumber = Integer.parseInt(terminal.readString("Please enter the endNode number: "));
			int connectedSwitch = Integer.parseInt(terminal.readString("Please enter the swtich number you are connected to: ")) 
					+ Constants.SWITCH_BASE_PORT;
			(new EndNode(terminal, endNodeNumber, connectedSwitch)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}
	
	public void composeMessage() {
		int destination = Integer.parseInt(terminal.readString("Please enter the end user you want to send to: \n"));
		String message = terminal.readString("Please enter the message you wish to send: \n");
		Packet messagePacket = new Packet();
		messagePacket.packetOut(this.connectedSwitch, destination, message);
		sendPacket(messagePacket);
	}
	
	public synchronized void start() throws Exception {
		while (true) {
			String command = terminal.readString("\nPlease choose an action to be carried out:\n "
					+ "1. Send a message\n" + "2. Wait for messages\n");
			if (command.contains("1"))
				composeMessage();
			else if (command.contains("2")) {
				terminal.println("Waiting for contact");
				this.wait();
			}
		}
	}
	
	public synchronized void onReceipt(DatagramPacket packet) {
		this.notify();
		byte[] packetContent = packet.getData();
		if(packetContent[0] == Constants.PACKET_OUT) {
			//process message in the packet
			byte[] messageInBytes = new byte[packetContent.length];
			for(int i = 0; i+3 < packetContent.length; i++) {
				messageInBytes[i] = packetContent[i+3];
			}
			String message = new String(messageInBytes);
			terminal.println("Message Received-" + message + "\n");
		}
	}
}
