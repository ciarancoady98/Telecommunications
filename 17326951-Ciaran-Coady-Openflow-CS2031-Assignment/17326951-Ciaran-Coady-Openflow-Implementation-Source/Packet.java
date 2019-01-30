import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class Packet {
	private DatagramPacket packet;
	
	Packet(){
	}
	
	public void addHeader(byte[] packetContent, int type, int endDestination, int length) {
		packetContent[0] = (byte) type;
		packetContent[1] = (byte) endDestination;
		packetContent[2] = (byte) length;
	}
	
	public void addMessageContent(byte[] packetContent, String message) {
		//TODO if message is too big to fit in a packet do not send it 
		byte[] messageArray = message.getBytes();
		for(int i = 0; i+3 < packetContent.length && i < messageArray.length; i++) {
			packetContent[i+3] = messageArray[i];
		}
	}
	
	//feature request packet
	public void featureRequestPacket(int portNumber, int switchNumber,  int configTable[][]) {
		DatagramPacket packet;
		//processing config table so it can be sent to the switch
		String content = "";
		for(int i = 0; i < configTable.length; i++) {
			if(configTable[i][2] == switchNumber) {
				content = content + configTable[i][0] + 
						"," + configTable[i][3] + "," + configTable[i][4] +
						",";
			}
		}
		//setup the array of bytes to be put in the packet
		byte[] packetContent = new byte[content.length() + 3];
		//add the header
		addHeader(packetContent, Constants.FEATURES_REQUEST, Constants.CONTROLLER_PORT_NUMBER, packetContent.length);
		//add the content
		addMessageContent(packetContent, content);
		//get the address of the packet
		InetSocketAddress dstAddress = new InetSocketAddress("localhost", portNumber);
		//create the packet
		packet = new DatagramPacket(packetContent, packetContent.length, dstAddress);
		this.packet = packet;
	}
	
	//feature reply packet
	public void featureReply(int portNumber, int nodeNumber) {
		DatagramPacket packet;
		byte[] packetContent = new byte[3];
		//add header
		addHeader(packetContent, Constants.FEATURES_REPLY, Constants.CONTROLLER_PORT_NUMBER, packetContent.length);
		//get the address of the packet
		InetSocketAddress dstAddress = new InetSocketAddress("localhost", portNumber);
		//create the packet
		packet = new DatagramPacket(packetContent, packetContent.length, dstAddress);
		this.packet = packet;
	}
	
	//hello packet
	public void helloPacket(int portNumber, int nodeNumber) {
		DatagramPacket packet;
		byte[] packetContent = new byte[3];
		//add header
		addHeader(packetContent, Constants.HELLO, nodeNumber, packetContent.length);
		InetSocketAddress dstAddress = new InetSocketAddress("localhost", portNumber);
		//create the packet
		packet = new DatagramPacket(packetContent, packetContent.length, dstAddress);
		this.packet = packet;
	}
	
	//packetIn packet
	public void packetIn(int portNumber, int nodeNumber) {
		DatagramPacket packet;
		byte[] packetContent = new byte[3];
		// add header
		addHeader(packetContent, Constants.PACKET_IN, nodeNumber, packetContent.length);
		InetSocketAddress dstAddress = new InetSocketAddress("localhost", portNumber);
		//create the packet
		packet = new DatagramPacket(packetContent, packetContent.length, dstAddress);
		this.packet = packet;
	}
	
	// packetOut packet
	public void packetOut(int portNumber, int nodeNumber, String message) {
		//TODO add error checking if the message is too big to fit into a packet
		DatagramPacket packet;
		byte[] packetContent = new byte[message.length()+3];
		// add header
		addHeader(packetContent, Constants.PACKET_OUT, nodeNumber, packetContent.length);
		addMessageContent(packetContent, message);
		InetSocketAddress dstAddress = new InetSocketAddress("localhost", portNumber);
		//create the packet
		packet = new DatagramPacket(packetContent, packetContent.length, dstAddress);
		this.packet = packet;
	}
	
	// flowMod packet
	public void flowMod(int portNumber, int switchNumber,  int configTable[][]) {
		DatagramPacket packet;
		//processing config table so it can be sent to the switch
		String content = "";
		for(int i = 0; i < configTable.length; i++) {
			if(configTable[i][2] == switchNumber) {
				content = content + configTable[i][0] + 
						"," + configTable[i][3] + "," + configTable[i][4] +
						",";
			}
		}
		//setup the array of bytes to be put in the packet
		byte[] packetContent = new byte[content.length() + 3];
		//add the header
		addHeader(packetContent, Constants.FLOW_MOD, Constants.CONTROLLER_PORT_NUMBER, packetContent.length);
		//add the content
		addMessageContent(packetContent, content);
		//get the address of the packet
		InetSocketAddress dstAddress = new InetSocketAddress("localhost", portNumber);
		packet = new DatagramPacket(packetContent, packetContent.length, dstAddress);
		this.packet = packet;
	}

	public DatagramPacket getPacket() {
		return this.packet;
	}
}
