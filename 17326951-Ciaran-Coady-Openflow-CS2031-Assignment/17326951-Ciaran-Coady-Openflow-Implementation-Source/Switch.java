import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import tcdIO.Terminal;

public class Switch extends Node {
	private int[][] flowTable;
	private Terminal terminal;
	private int switchNumber;
	private DatagramPacket buffer;
	Switch(Terminal terminal, int switchNumber) {
		this.terminal = terminal;
		this.switchNumber = switchNumber;
		try {
			socket = new DatagramSocket(switchNumber + Constants.SWITCH_BASE_PORT);
			listener.go();
		} catch (

		java.lang.Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Switch");
			int switchNumber = Integer.parseInt(terminal.readString("Please enter the switch number: "));
			(new Switch(terminal, switchNumber)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void start() throws Exception {
		//send hello to controller
		Packet helloPacket = new Packet();
		helloPacket.helloPacket(Constants.CONTROLLER_PORT_NUMBER, this.switchNumber);
		sendPacket(helloPacket);
		while (true) {
			terminal.println("Waiting....");
			this.wait();
		}
	}
	
	public void processFlowTable(byte[] packetData) {
		byte[] temp1 = new byte[packetData.length-3];
		for(int i = 0; i < temp1.length; i++) {
			temp1[i] = packetData[i+3];
		}
		byte[] temp = new String(temp1).replaceAll("\0", "").getBytes();
		String flowTableString = "";
		try {
			flowTableString = new String(temp, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] flowTableRows = flowTableString.split(",");
		flowTable = new int[flowTableRows.length/3][3];
		int row = 0;
		for(int i = 0; i < flowTableRows.length; i++) {
			if(i != 0 && i%3 == 0) {
				row++;
			}
			flowTable[row][i%3] = Integer.parseInt(flowTableRows[i]);
		}
		
	}
	
	public void forwardPacket() {
		byte[] packetContent = this.buffer.getData();
		boolean found = false;
		for(int i = 0; i < this.flowTable.length && !found; i++) {
			if(packetContent[1] == flowTable[i][0] && this.buffer.getPort() == flowTable[i][1]) {
				this.buffer.setPort(flowTable[i][2]);
				found = true;
			}
		}
		try {
			socket.send(this.buffer);
			terminal.println("Forwarded Buffer to the Next hop");
			this.buffer = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void onReceipt(DatagramPacket packet) {
		byte[] packetContent = packet.getData();
		if(packetContent[0] == Constants.HELLO) {
			//our hello has been acked
			terminal.println("hello received");
		}
		else if (packetContent[0] == Constants.FEATURES_REQUEST) {
			//send feature response
			terminal.println("feature request received");
			//process received table
			processFlowTable(packetContent);
			Packet response = new Packet();
			response.featureReply(packet.getPort(), this.switchNumber);
			sendPacket(response);
		}
		else if (packetContent[0] == Constants.PACKET_OUT) {
			terminal.println("PacketOut received");
			//save it to the buffer
			if(buffer == null) {
				buffer = packet;
			}
			//convert to packetIn and send to controller
			Packet packetIn = new Packet();
			packetIn.packetIn(Constants.CONTROLLER_PORT_NUMBER, packetContent[1]);
			sendPacket(packetIn);
			terminal.println("Forwarded PacketIn to the Controller");
		}
		else if (packetContent[0] == Constants.FLOW_MOD) {
			terminal.println("flowMod received from controller");
			/*
			 * processing the flow table has a bug that breaks the flow table
			 * hence it is commented out
			 */
			//processFlowTable(packetContent);
			forwardPacket();
		}
	}
}