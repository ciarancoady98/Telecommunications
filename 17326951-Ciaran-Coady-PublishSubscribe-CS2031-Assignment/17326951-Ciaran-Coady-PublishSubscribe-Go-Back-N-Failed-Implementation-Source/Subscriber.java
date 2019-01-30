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
	private String buffer = "";
	

	Subscriber(Terminal terminal) {
		try {
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(Constants.DEFAULT_DST_NODE, Constants.TEST_BKR_PORT);
			socket = new DatagramSocket(Constants.TEST_SUB_PORT);
			listener.go();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
		receivedPackets = new TreeMap<Integer, DatagramPacket>();
		queue = new TreeMap<Integer, DatagramPacket[]>();
		currentPacketPort = null;
		state = Constants.IDLE;
	}

	public synchronized void start() throws Exception {
		while (true) {
			sendQueue();
			if (state == Constants.IDLE) {
				String command = terminal.readString("Please choose an action to be carried out:\n "
						+ "1. Subscribe to a topic\n" + "2. Unsubscribe from a topic\n" + "3. Wait for topics\n");
				if (command.contains("1"))
					subscribe();
				else if (command.contains("2"))
					unsubscribe();
				else if (command.contains("3")) {
					terminal.println("waiting for publications");
					this.wait();
				}
				
				//this.wait();
			} else if (state == Constants.SUB_RECEIVING_PUB) {
				this.wait();
			}
		}
		
	}

	public void printMessage() {
		for (Integer key : receivedPackets.keySet()) {
			buffer += processMessageFromPacket(receivedPackets.get(key));
			receivedPackets.remove(key);
		}
		terminal.println(buffer);
		buffer = "";
	}

	/*
	 * subscribe method. Asks the user for the name of the topic to subscribe to.
	 * Sends the name of this topic to the Broker.
	 */
	public synchronized void subscribe() {
		DatagramPacket[] packets = new DatagramPacket[1];
		byte[] topic = (terminal.readString("Please enter a topic to subscribe to: ")).getBytes();
		byte[] data = createPacketData(Constants.SUBSCRIPTION, 0, 0, topic);
		terminal.println("Sending packet...");
		DatagramPacket packet = new DatagramPacket(data, data.length, dstAddress);
		packets[0] = packet;
		addItemsToQueue(packets);
		terminal.println("Packet sent");
	}

	public synchronized void unsubscribe() {
		DatagramPacket[] packets = new DatagramPacket[1];
		byte[] topic = (terminal.readString("Please enter a topic to unsubscribe from: ")).getBytes();
		byte[] data = createPacketData(Constants.UNSUBSCRIPTION, 0, 0, topic);
		terminal.println("Sending packet...");
		DatagramPacket packet = new DatagramPacket(data, data.length, dstAddress);
		packets[0] = packet;
		addItemsToQueue(packets);
		terminal.println("Packet sent");
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Subscriber");
			(new Subscriber(terminal)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void onReceipt(DatagramPacket packet) {
		this.notify();
		int packetPort = packet.getPort();
		if (currentPacketPort == null) {
			currentPacketPort = packetPort;
		}
		if (/*currentPacketPort == packetPort*/true) {
			byte[] data = packet.getData();
			if (data[0] == Constants.ACK) {
				removeAckedPacketFromPacketsToSend(packet);
				terminal.println("Acknowledgement received: " + data.length);
			} else if (data[0] == Constants.PUBLICATION) {
				//change state
				state = Constants.SUB_RECEIVING_PUB;
				addToReceivedPackets(packet);
				sendAck(packet);
			} else if (data[0] == Constants.ENDOFTRANSMISSION) {
				if (state == Constants.SUB_RECEIVING_PUB && checkIsEndOfCurrentPub(packet)) {
					printMessage();
					sendAck(packet);
					state = Constants.SUB_DEFAULT_STATE;
				}
				else
					sendAck(packet);
			}
		}
		System.out.println(printPacketContent(packet) + " Subscriber received");
	}

}
