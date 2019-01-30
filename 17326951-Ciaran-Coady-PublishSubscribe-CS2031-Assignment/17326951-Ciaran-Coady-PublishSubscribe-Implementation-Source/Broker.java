package cs.tcd.ie;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import tcdIO.Terminal;

/*
 * Broker Class
 * 
 * Features:
 * 			Receives packets from both publishers and subscribers
 * 			Maintains a list of topics and the subscribers to each topic
 * 			Forwards publications to the correct subscribers
 * 			
 * Known Bugs/Issues
 * 			Doesnt cache publications before resending them
 * 			Doesnt account for dropped packets
 * 			Cannot resent packets after they have been send.
 * 
 */

public class Broker extends Node {
	private Terminal terminal;
	private Map<String, ArrayList<Integer>> subscriberMap;
	private Map<Integer, String> topicNumbers;

	//Constructor
	Broker(Terminal terminal) {
		this.terminal = terminal;
		try {
			socket = new DatagramSocket(Constants.TEST_BKR_PORT);
			listener.go();
		} catch (

		java.lang.Exception e) {
			e.printStackTrace();
		}
		subscriberMap = new HashMap<String, ArrayList<Integer>>();
		topicNumbers = new HashMap<Integer, String>();
	}

	/*
	 * Main line
	 * Initialises the broker
	 */
	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Broker");
			(new Broker(terminal)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * createTopic method. Receives the name of a topic and creates an ArrayList of
	 * Subscriber socket numbers to give to the topic. Adds the ArrayList to
	 * SubscriberMap using the topic name as a key.
	 */
	private void createTopic(String topicName) {
		ArrayList<Integer> socketNumbers = new ArrayList<Integer>();
		subscriberMap.put(topicName, socketNumbers);
		topicNumbers.put(topicNumbers.size(), topicName);
		terminal.println("Topic " + topicName + " was created.");
	}
	
	/*
	 * checkTopic method. Checks if a topic exists,
	 * if the topic exists, return its topic number,
	 *  if not create one and return a topic number
	 */
	private void checkTopic(String topicName, DatagramPacket packet) {
		Integer topicIndex = null;
		for(int index = 0; index < topicNumbers.size(); index++) {
			if(topicNumbers.get(index).equals(topicName));
				topicIndex = index;
		}
		if(topicIndex == null) {
			createTopic(topicName);
			sendTopicNumber(topicNumbers.size()-1, packet);
		}
		else {
			sendTopicNumber(topicIndex, packet);
		}
	}
	
	
	/*
	 * sendTopicNumber method. Sends the topic number of the requested topic
	 * to the publisher
	 */
	private void sendTopicNumber(int topicNumber, DatagramPacket receivedPacket) {
		byte[] newPacketData = createPacketData(Constants.CREATION, 0, 0, topicNumber, processMessageFromPacket(receivedPacket).getBytes());
		DatagramPacket newPacket = new DatagramPacket(newPacketData, newPacketData.length);
		newPacket.setSocketAddress(receivedPacket.getSocketAddress());
		try {
			socket.send(newPacket);
		} catch (IOException e) {
			System.out.println("Broker failed to send confirmation message to publisher.");
		}
	}

	/*
	 * sendAck method. Receives a packet and a confirmation message to send
	 * back. Sends the message to the sender of the original packet.
	 */
	private void sendAck(String message, DatagramPacket receivedPacket) {
		byte[] data = receivedPacket.getData();
		byte[] messageInBytes = message.getBytes();
		int packetNumber = data[1];
		byte[] confirmationPacketContent = createPacketData(Constants.ACK, packetNumber, 0, 0, messageInBytes);
		DatagramPacket confirmation = new DatagramPacket(confirmationPacketContent, confirmationPacketContent.length);
		confirmation.setSocketAddress(receivedPacket.getSocketAddress());
		try {
			socket.send(confirmation);
		} catch (IOException e) {
			System.out.println("Broker failed to send confirmation message to publisher.");
		}
	}

	/*
	 * takes the published message and forwards it to all subscribers
	 * subed to the corresponding topic
	 */
	private void publishMessage(DatagramPacket packet) {
		byte[] message = packet.getData();
		int topicNumber = message[3];
		if (topicNumbers.containsKey(topicNumber)) {
			String topicName = topicNumbers.get(topicNumber);
			if (subscriberMap.containsKey(topicName)) {
				ArrayList<Integer> topicSubscribers = subscriberMap.get(topicName);
				for (int index = 0; index < topicSubscribers.size(); index++) {
					InetSocketAddress subAddress = new InetSocketAddress(Constants.DEFAULT_DST_NODE,
							topicSubscribers.get(index));
					DatagramPacket messagePacket = new DatagramPacket(message, message.length,
							subAddress);
					try {
						socket.send(messagePacket);
					} catch (IOException e) {
						System.out.println("Broker failed to send confirmation message to publisher.");
					}
				}
			}
		}
	}
	
	/*
	 * subscribe method. Checks if a topic exists, if so
	 * adds a new subscriber to that topic if they
	 * are not already subscribed.
	 */
	private void subscribe(DatagramPacket packet) {
		String topic = processMessageFromPacket(packet);
		if (subscriberMap.containsKey(topic)) {
			ArrayList<Integer> topicSubscribers = subscriberMap.get(topic);
			if(!topicSubscribers.contains(packet.getPort())){
				topicSubscribers.add(packet.getPort());
			}
			sendAck("Success", packet);
		} else {
			sendAck("Topic Doesnt Exist", packet);
		}
	}
	
	/*
	 * unsubscribe method. Checks if a topic exists, if so
	 * removes an existing subscriber from that topic if they
	 * are already subscribed.
	 */
	private void unsubscribe(DatagramPacket packet) {
		String topic = processMessageFromPacket(packet);
		if (subscriberMap.containsKey(topic)) {
			ArrayList<Integer> topicSubscriptions = subscriberMap.get(topic);
			for (int index = 0; index < topicSubscriptions.size(); index++) {
				if (topicSubscriptions.get(index) == packet.getPort()) {
					topicSubscriptions.remove(index);
				}
			}
			sendAck("Success", packet);
		} else {
			sendAck("No Subscription Exists", packet);
		}
	}

	/*
	 * the main loop of the broker. loops continuously waiting for packets
	 */
	public synchronized void start() throws Exception {
		while (true) {
			terminal.println("Waiting for contact");
			this.wait();
		}
	}

	
	/*
	 * handles what to do when each type of packet is received
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			this.notify();
			byte[] data = packet.getData();
			System.out.println(printPacketContent(packet));
			if (data[0] == Constants.CREATION) {
				terminal.println("Creation Received From The Publisher");
				String topic = processMessageFromPacket(packet);
				checkTopic(topic, packet);
				sendAck("The topic " + topic + " was created.", packet);
			} else if (data[0] == Constants.PUBLICATION) {
				terminal.println("Publication Received From The Publisher");
				publishMessage(packet);
				sendAck("Message has been published", packet);
			} else if (data[0] == Constants.SUBSCRIPTION) {
				terminal.println("Subscription recieved from Subscriber.");
				subscribe(packet);
			} else if (data[0] == Constants.UNSUBSCRIPTION) {
				terminal.println("Unsubscription recieved from Subscriber.");
				unsubscribe(packet);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
