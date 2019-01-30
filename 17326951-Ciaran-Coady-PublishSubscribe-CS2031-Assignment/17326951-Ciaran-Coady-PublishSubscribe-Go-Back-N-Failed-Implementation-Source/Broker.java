package cs.tcd.ie;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import tcdIO.Terminal;

public class Broker extends Node {
	private Terminal terminal;
	private SocketAddress publisherAddress = null;
	private Map<String, ArrayList<Integer>> subscriberMap;
	private Map<Integer, String> topicNumbers;
	
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
		receivedPackets = new TreeMap<Integer, DatagramPacket>();
		queue = new TreeMap<Integer, DatagramPacket[]>();
		
		state = Constants.BKR_DEFAULT_STATE;
		currentPacketPort = null;
		state = Constants.IDLE;
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Broker");
			(new Broker(terminal)).start();
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
	 * checks if a topic exists in the list of topics if it does send packet with
	 * the topic number else create a new topic and send its topic number back
	 */
	private void checkTopic(String topicName, DatagramPacket packet) throws InterruptedException {
		int topicIndex = 0;
		boolean found = false;
		for (int index = 0; index < topicNumbers.size(); index++) {
			if (topicNumbers.get(index).equals(topicName)) {
				topicIndex = index;
				found = true;
			}
		}
		if (!found) {
			createTopic(topicName);
			sendTopicNumber(topicNumbers.size() - 1, packet);
		} else {
			sendTopicNumber(topicIndex, packet);
		}
	}

	/*
	 * takes a topic number and a packet, creates a creation packet with the topic
	 * number and sends it to the destination the received packet came from
	 */
	private void sendTopicNumber(int topicNumber, DatagramPacket receivedPacket) throws InterruptedException {
		DatagramPacket[] packets = new DatagramPacket[1];
		byte[] newPacketData = createPacketData(Constants.CREATION, 0, topicNumber,
				processMessageFromPacket(receivedPacket).getBytes());
		DatagramPacket newPacket = new DatagramPacket(newPacketData, newPacketData.length);
		newPacket.setSocketAddress(receivedPacket.getSocketAddress());
		packets[0] = newPacket;
		addItemsToQueue(packets);
	}

	/*
	 * sendConfirmation method. Receives a packet and a confirmation message to send
	 * back. Sends the message to the sender of the original packet.
	 * 
	 * NOT USED ANYMORE CAN DELETE WHEN HAPPY
	 */
	private void sendConfirmation(String message, DatagramPacket receivedPacket) {
		byte[] data = receivedPacket.getData();
		int packetNumber = data[1];
		byte[] confirmationPacketContent = createPacketData(Constants.ACK, packetNumber, data[1],
				("received " + packetNumber).getBytes());
		DatagramPacket confirmation = new DatagramPacket(confirmationPacketContent, confirmationPacketContent.length);
		confirmation.setSocketAddress(receivedPacket.getSocketAddress());
		try {
			socket.send(confirmation);
		} catch (IOException e) {
			System.out.println("Broker failed to send confirmation message.");
		}
	}

	/*
	 * moves the sorted treemap of received packets to an array for processing
	 * purposes
	 */
	private DatagramPacket[] moveReceivedPacketsToArray() {
		DatagramPacket[] packets = new DatagramPacket[receivedPackets.size()];
		int messageIndex = 0;
		for (Integer key : receivedPackets.keySet()) {
			// change the destination of each packet
			packets[messageIndex] = receivedPackets.get(key);
			messageIndex++;
		}
		receivedPackets = new TreeMap<Integer, DatagramPacket>();
		return packets;
	}

	private void publishMessage() throws InterruptedException {
		// move packets reveived to array
		DatagramPacket[] packets = moveReceivedPacketsToArray();
		// get packet info
		DatagramPacket packet = packets[0];
		byte[] packetData = packet.getData();
		byte[] topicNumberInBytes = new byte[4];
		for (int index = 2; index < 6; index++) {
			topicNumberInBytes[index - 2] = packetData[index];
		}
		ByteBuffer wrapped = ByteBuffer.wrap(topicNumberInBytes);
		int topicNumber = wrapped.getInt();

		// check if the topic exists
		if (topicNumbers.containsKey(topicNumber)) {
			String topicName = topicNumbers.get(topicNumber);
			if (subscriberMap.containsKey(topicName)) {
				ArrayList<Integer> topicSubscribers = subscriberMap.get(topicName);
				for (int index = 0; index < topicSubscribers.size(); index++) {
					// make an array
					DatagramPacket[] packetsToSendToSub = new DatagramPacket[packets.length];
					// change the destination of all the packets
					for (int i = 0; i < packets.length; i++) {
						DatagramPacket tempPacket = packets[i];
						/*
						 * String pubMessage = processMessageFromPacket(tempPacket); byte[]
						 * messagePacketBytes = createPacketData(Constants.PUBLICATION, message[2],
						 * topicNumber, pubMessage.getBytes()); byte[] packetContent =
						 * tempPacket.getData();
						 */
						InetSocketAddress subAddress = new InetSocketAddress(Constants.DEFAULT_DST_NODE,
								topicSubscribers.get(index));
						tempPacket.setSocketAddress(subAddress);
						packetsToSendToSub[i] = tempPacket;
					}
					addItemsToQueue(packetsToSendToSub);
				}
			}
		}
	}

	/*
	 * starts the broker and loops waiting for contact
	 */
	public synchronized void start() throws Exception {
		while (true) {
			terminal.println("Waiting for contact");
			sendQueue();
			this.wait();
		}
	}

	/*
	 * invoked when a packet is received
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			this.notify();
			/*
			 * if we arent receiving change our state to receiving and only take packets
			 * from this port number
			 */
			int packetPort = packet.getPort();
			if (currentPacketPort == null) {
				currentPacketPort = packetPort;
			}
			if (/*currentPacketPort == packetPort*/true) {
				byte[] data = packet.getData();
				System.out.println(printPacketContent(packet) + " Broker received");
				/*
				 * if its a creation check to see if the topic exists and send an ack
				 */
				if (data[0] == Constants.CREATION) {
					//terminal.println("Creation Received From The Publisher");
					String topic = processMessageFromPacket(packet);
					sendAck(packet);
					checkTopic(topic, packet);
					currentPacketPort = null;
					/*
					 * if its a publication store the packets in the sorted treemap, change state to
					 * receiving pub
					 */
				} else if (data[0] == Constants.PUBLICATION) {
					//terminal.println("Publication Received From The Publisher");
					state = Constants.BKR_RECEIVING_PUB;
					//add check to see if it is for the currentTransmission
					addToReceivedPackets(packet);
					sendAck(packet);
					/*
					 * its a subscription add the subscriber to the topic if it exists and send an
					 * ack
					 */
				} else if (data[0] == Constants.SUBSCRIPTION) {
					//terminal.println("Subscription recieved from Subscriber.");
					String topic = processMessageFromPacket(packet);
					if (subscriberMap.containsKey(topic)) {
						ArrayList<Integer> subscriberPorts = subscriberMap.get(topic);
						if(!subscriberPorts.contains(packet.getPort())) {
							subscriberMap.get(topic).add(packet.getPort());
						}
						sendAck(packet);
					} else {
						sendAck(packet);
					}
					/*
					 * if its an unsubscription remove that sub from the topics list of port numbers
					 */
				} else if (data[0] == Constants.UNSUBSCRIPTION) {
					//terminal.println("Unsubscription recieved from Subscriber.");
					String topic = processMessageFromPacket(packet);
					if (subscriberMap.containsKey(topic)) {
						ArrayList<Integer> topicSubscriptions = subscriberMap.get(topic);
						for (int index = 0; index < topicSubscriptions.size(); index++) {
							if (topicSubscriptions.get(index) == packet.getPort()) {
								topicSubscriptions.remove(index);
							}
						}
						sendAck(packet);
					} else {
						sendAck(packet);
					}
					/*
					 * if its an ack remove the acked packet from the queue
					 */
				} else if (data[0] == Constants.ACK) {
					// remove the acked packet from packetsAwaitingAck
					//TODO if the ack has something in the message part that
					//its for a endoftransmission then
					removeAckedPacketFromPacketsToSend(packet);	
					/*
					 * if its the end of a transmission change state back to receiving and publish
					 * the packets received
					 */
				} else if (data[0] == Constants.ENDOFTRANSMISSION) {
					//if the end of transmission is for the current publication
					//then invoke publishMessage and change our state to idle
					if (state == Constants.BKR_RECEIVING_PUB && checkIsEndOfCurrentPub(packet)) {
						sendAck(packet);
						publishMessage();
						state = Constants.BKR_DEFAULT_STATE;
					}
					else
						sendAck(packet);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
