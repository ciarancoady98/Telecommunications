import java.net.DatagramPacket;
import java.net.DatagramSocket;
import tcdIO.Terminal;

public class Controller extends Node {
	
	private final int[][] preconfigTable = {
			//{Dest   					source 	  SWITCH  				in 						 out  }
			{Constants.ENDUSER2, Constants.ENDUSER1, 1, Constants.ENDUSER1_PORT_NUMBER, Constants.SWITCH2_PORT_NUMBER}, 
			{Constants.ENDUSER2, Constants.ENDUSER1, 2, Constants.SWITCH1_PORT_NUMBER, Constants.SWITCH3_PORT_NUMBER},
			{Constants.ENDUSER2, Constants.ENDUSER1, 3, Constants.SWITCH2_PORT_NUMBER, Constants.ENDUSER2_PORT_NUMBER},
			{Constants.ENDUSER1, Constants.ENDUSER2, 1, Constants.SWITCH2_PORT_NUMBER, Constants.ENDUSER1_PORT_NUMBER}, 
			{Constants.ENDUSER1, Constants.ENDUSER2, 2, Constants.SWITCH3_PORT_NUMBER, Constants.SWITCH1_PORT_NUMBER},
			{Constants.ENDUSER1, Constants.ENDUSER2, 3, Constants.ENDUSER2_PORT_NUMBER, Constants.SWITCH2_PORT_NUMBER},
	};
	
	private Terminal terminal;
	
	Controller(Terminal terminal) {
		this.terminal = terminal;
		try {
			socket = new DatagramSocket(Constants.CONTROLLER_PORT_NUMBER);
			listener.go();
		} catch (

		java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Controller");
			(new Controller(terminal)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void start() throws Exception {
		terminal.println("Controller Intialized On Port No: " + Constants.CONTROLLER_PORT_NUMBER);
		while (true) {
			terminal.println("Waiting for contact");
			this.wait();
		}
	}
	
	public synchronized void onReceipt(DatagramPacket packet) {
		/* if we receive a hello packet 
		 * respond by sending back a hello packet and then a feature request 
		 * 
		*/
		byte[] packetContent = packet.getData();
		if(packetContent[0] == Constants.HELLO) {
			terminal.println("Hello received from switch number: " + (packet.getPort() - Constants.SWITCH_BASE_PORT));
			//send hello in response
			Packet response = new Packet();
			response.helloPacket(packet.getPort(), packetContent[1]);
			sendPacket(response);
			//send feature request
			Packet featureRequest = new Packet();
			featureRequest.featureRequestPacket(packet.getPort(), packetContent[1], this.preconfigTable);;
			sendPacket(featureRequest);
		}
		else if (packetContent[0] == Constants.FEATURES_REPLY) {
			//TODO this will be used if we implement distance based routing
			terminal.println("feature reply received");
		}
		else if (packetContent[0] == Constants.PACKET_IN) {
			terminal.println("packetIn received from switch number: " + (packet.getPort() - Constants.SWITCH_BASE_PORT));
			Packet flowMod = new Packet();
			flowMod.flowMod(packet.getPort(), packetContent[1], this.preconfigTable);
			sendPacket(flowMod);
			terminal.println("flowMod send to switch number: " + (packet.getPort() - Constants.SWITCH_BASE_PORT));
		}
	}
}
