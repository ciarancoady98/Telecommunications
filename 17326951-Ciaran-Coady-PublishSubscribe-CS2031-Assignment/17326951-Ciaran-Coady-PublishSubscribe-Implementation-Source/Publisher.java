package cs.tcd.ie;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import tcdIO.Terminal;

public class Publisher extends Node {

	Terminal terminal;
	InetSocketAddress dstAddress;
	private Map<Integer, String> topicNumbers;

	//Constructor
	Publisher(Terminal terminal) {
		try {
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(Constants.DEFAULT_DST_NODE, Constants.TEST_BKR_PORT);
			socket = new DatagramSocket(Constants.TEST_PUB_PORT);
			listener.go();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
		topicNumbers = new HashMap<Integer, String>();
	}

	/*
	 * Main line
	 * Initialises the publisher
	 */
	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Publisher");
			(new Publisher(terminal)).start();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * addTopicToList method. Adds a new topic to the hashmap of topics
	 * using the topic number as an index.
	 */
	private void addTopicToList(int topicNumber, String topicName) {
		topicNumbers.put(topicNumber, topicName);
	}

	
	/*
	 * checkTopic method. Requests to create a topic with the broker,
	 * sends a creation packet containing the topic name it wishes to 
	 * create
	 */
	private void checkTopic() throws InterruptedException { // :)
		String topic = terminal.readString("Please enter a topic to create: ");
		terminal.println("Sending packet...");
		DatagramPacket[] packets = createPackets(Constants.CREATION, 0, topic, dstAddress);
		sendPackets(packets);
		terminal.println("Packet sent");
	}

	/*
	 * publishMessage method. Check the topic list to see if that topic exists,
	 * if it does take the message the user wants to publish (breaking it up into 
	 * multiple packets if necessary) and send it to the broker
	 */
	private void publishMessage() throws InterruptedException {
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
			sendPackets(packets);
			terminal.println("Packet sent");
		}
	}

	/*
	 * start method. "Starts" the publisher and asks the user what action they wish to 
	 * carry out. This is the main loop of the Publisher, it will carry out the current 
	 * task and when complete will give the user the option to do more operations
	 */
	public synchronized void start() throws Exception {
		while (true) {
			String startingString = terminal.readString(
					"Please choose an operation to carry out:\n" + "1. Create a topic\n" + "2. Publish a message\n");
			if (startingString.toUpperCase().contains("1")) {
				checkTopic();
			} else if (startingString.toUpperCase().contains("2")) {
				publishMessage();
			}
		}
	}

	/*
	 * handles what to do when each type of packet is received
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		this.notify();
		byte[] data = packet.getData();
		if (data[0] == Constants.ACK) {
			// print the message in the acknowledgement
			terminal.println(processMessageFromPacket(packet));
		} else if (data[0] == Constants.CREATION) {
			int topicNumber = data[3];
			addTopicToList(topicNumber, processMessageFromPacket(packet));
		}
	}

}
