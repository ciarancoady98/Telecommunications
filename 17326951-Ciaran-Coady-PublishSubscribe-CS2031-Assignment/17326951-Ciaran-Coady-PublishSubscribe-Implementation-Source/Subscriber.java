package cs.tcd.ie;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.SortedMap;
import java.util.TreeMap;

import tcdIO.Terminal;

public class Subscriber extends Node {

	private Terminal terminal;
	private InetSocketAddress dstAddress;
	private int state = Constants.SUB_DEFAULT_STATE;
	private String buffer = "";
	private SortedMap<Integer, DatagramPacket> receivedPublication = new TreeMap<Integer, DatagramPacket>();

	//Constructor
	Subscriber(Terminal terminal) {
		try {
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(Constants.DEFAULT_DST_NODE, Constants.TEST_BKR_PORT);
			socket = new DatagramSocket(Constants.TEST_SUB_PORT);
			listener.go();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * main loop of the subscriber. loops asking for input from the user if the user
	 * wants to wait for topics then it cannot do anything else
	 */
	public synchronized void start() throws Exception {
		while (true) {
			if (state == Constants.SUB_DEFAULT_STATE) {
				String command = terminal.readString("Please choose an action to be carried out:\n "
						+ "1. Subscribe to a topic\n" + "2. Unsubscribe from a topic\n" + "3. Wait for topics\n");
				if (command.contains("1"))
					subscribe();
				else if (command.contains("2"))
					unsubscribe();
				else if (command.contains("3")) {
					terminal.println("waiting for publications");
				}
				this.wait();
			} else if (state == Constants.SUB_AWAIT_NEW_PACKET) {
				this.wait();
			}
		}
	}

	/*
	 * subscribe method. Asks the user for the name of the topic to subscribe to.
	 * Sends the name of this topic to the Broker.
	 */
	public synchronized void subscribe() {
		byte[] topic = (terminal.readString("Please enter a topic to subscribe to: ")).getBytes();
		byte[] data = createPacketData(Constants.SUBSCRIPTION, 0, 0, 0, topic);
		terminal.println("Sending packet...");
		DatagramPacket packet = new DatagramPacket(data, data.length, dstAddress);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		terminal.println("Packet sent");
	}

	/*
	 * unsubscribe method. Asks the user for the name of the topic to unsubscribe
	 * from. Sends the name of this topic to the Broker.
	 */
	public synchronized void unsubscribe() {
		byte[] topic = (terminal.readString("Please enter a topic to unsubscribe from: ")).getBytes();
		byte[] data = createPacketData(Constants.UNSUBSCRIPTION, 0, 0, 0, topic);
		terminal.println("Sending packet...");
		DatagramPacket packet = new DatagramPacket(data, data.length, dstAddress);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		terminal.println("Packet sent");
	}
	
	/*
	 * flushBuffer method. Checks if the packet received is the end of a publication,
	 * if so it will change the state of the subscriber and flush the buffer to the 
	 * terminal. Otherwise the subscriber remains waiting for packets.
	 */
	private void flushBuffer(DatagramPacket packet) {
		if (lastPacket(packet)) {
			state = Constants.SUB_DEFAULT_STATE;
			printPublication();
		} else {
			state = Constants.SUB_AWAIT_NEW_PACKET;
		}
	}

	/*
	 * printPublication method. Takes the ordered packets 
	 * from the receivedPublication treemap and adds them
	 * to a string which is printed to the terminal
	 */
	public void printPublication() {
		for (Integer key : receivedPublication.keySet()) {
			DatagramPacket packet = receivedPublication.get(key);
			String message = processMessageFromPacket(packet);
			buffer += message;
		}
		terminal.println(buffer);
		buffer = "";
	}

	/*
	 * Main line
	 * Initialises the subscriber
	 */
	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Subscriber");
			(new Subscriber(terminal)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * handles what to do when each type of packet is received
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		this.notify();
		byte[] data = packet.getData();
		if (data[0] == Constants.ACK) {
			terminal.println("ACK: " + processMessageFromPacket(packet));
		} else if (data[0] == Constants.PUBLICATION) {
			receivedPublication.put((int) data[1], packet);
			flushBuffer(packet);
		}
	}
}
