package cs.tcd.ie;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import tcdIO.Terminal;

public class Publisher extends Node {

	Terminal terminal;
	InetSocketAddress dstAddress;
	private Map<Integer, String> topicNumbers;

	Publisher(Terminal terminal) { // :)
		try {
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(Constants.DEFAULT_DST_NODE, Constants.TEST_BKR_PORT);
			socket = new DatagramSocket(Constants.TEST_PUB_PORT);
			listener.go();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
		topicNumbers = new HashMap<Integer, String>();
		queue = new TreeMap<Integer, DatagramPacket[]>();
		currentPacketPort = null;
		state = Constants.IDLE;
		sendState = Constants.IDLE;
		pendingPrint = "";
	}

	public static void main(String[] args) { // :)
		try {
			Terminal terminal = new Terminal("Publisher");
			(new Publisher(terminal)).start();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * adds a topic to the publishers hashmap of topics
	 */
	private void addTopicToList(int topicNumber, String topicName) {
		topicNumbers.put(topicNumber, topicName);
	}
	
	/*
	 * Asks the user to create a topic and sends a packet to the publisher
	 * to see if that topic exists
	 */
	private void checkTopic() throws InterruptedException { // :)
		String topic = terminal.readString("Please enter a topic to create: ");
		DatagramPacket[] packets = createPackets(Constants.CREATION, 0, topic, dstAddress);
		addItemsToQueue(packets);
	}

	/*
	 * Checks to see if a topic exists with this publisher
	 * if so it allows the user to send a publication
	 * else it prints a message saying that the topic
	 * does not exist
	 */
	private void publishMessage() throws InterruptedException { // :)
		String topic = terminal.readString("Enter the topic to Publish for: ");
		Integer topicNumber = null;
		for(Integer key : topicNumbers.keySet()) {
			if(topicNumbers.get(key).equals(topic))
				topicNumber = key;
		}
		if (topicNumber == null) {
			terminal.println("This topic does not exist.");
		} else {
			String message = terminal.readString("Please enter the message that you would like to publish: ");
			DatagramPacket[] packets = createPackets(Constants.PUBLICATION, topicNumber, message, dstAddress);
			addItemsToQueue(packets);
		}
	}

	/*
	 * starts the publisher loop which asks for input from the user
	 */
	public synchronized void start() throws Exception {
		while (true) {
			sendQueue();
			terminal.println("Please choose an operation to carry out:\n" + "1. Create a topic\n" + "2. Publish a message");
			String startingString = terminal.readString();
			if (startingString.toUpperCase().contains("1")) {
				checkTopic();
			} else if (startingString.toUpperCase().contains("2")) {
				publishMessage();
			}
			sendQueue();
		}
	}

	/*
	 * is called when a packet is received
	 *
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		//this.notify();
		/*
		 * if we arent receiving change our state to receiving
		 * and only take packets from this port number
		 */
		int packetPort = packet.getPort();
		if(currentPacketPort == null) {
			currentPacketPort = packetPort;
		}
		
		byte[] data = packet.getData();
		System.out.println(printPacketContent(packet) + " Publisher received");
		
		/*
		 * if we get an ack remove the packet corresponding to that
		 * packet number from the queue
		 */
		if (data[0] == Constants.ACK) {
			// print the message in the acknowledgement
			//terminal.println(processMessageFromPacket(packet));
			//remove the acked packet from queue
			removeAckedPacketFromPacketsToSend(packet);	
			/*
			 * if the packet is a topic creation get the topic number
			 * and add it to the list of topics
			 */
		} else if (data[0] == Constants.CREATION) {
			byte[] topicNumberInBytes = new byte[4];
			for (int index = 2; index < 6; index++) {
				topicNumberInBytes[index - 2] = data[index];
			}
			ByteBuffer wrapped = ByteBuffer.wrap(topicNumberInBytes);
			int topicNumber = wrapped.getInt();
			addTopicToList(topicNumber, processMessageFromPacket(packet));
			sendAck(packet);
		} else if (data[0] == Constants.ENDOFTRANSMISSION) {
			sendAck(packet);
		}
	}

}
