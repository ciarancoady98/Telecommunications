package cs.tcd.ie;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import tcdIO.Terminal;

public abstract class Node {
	static final int PACKETSIZE = /* 65536 60000 */ 40;

	public DatagramPacket[] packetsToSend;
	public Integer currentPacketPort;
	public SortedMap<Integer, DatagramPacket> receivedPackets;
	public TreeMap<Integer, DatagramPacket[]> queue;
	public int state;
	public int sendState;
	public String pendingPrint;

	DatagramSocket socket;
	Listener listener;
	CountDownLatch latch;

	Node() {
		latch = new CountDownLatch(1);
		listener = new Listener();
		listener.setDaemon(true);
		listener.start();
	}

	/*
	 * createPacketData method. Creates an array of bytes for a DatagramPacket and
	 * returns it. Based on custom packet data layout; byte 0 = type, byte 1 =
	 * packet number for Go-Back-N, bytes 2-5 = topic number, remaining bytes =
	 * message.
	 * 
	 */
	protected byte[] createPacketData(int type, int packetNumber, int topicNumber, byte[] message) {
		byte[] data = new byte[PACKETSIZE];
		data[0] = (byte) type; // Set type
		data[1] = (byte) packetNumber; // Set packet number
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byte[] topicNumberArray = byteBuffer.putInt(topicNumber).array();
		for (int i = 0; i < 4; i++) {
			data[i + 2] = topicNumberArray[i]; // Set topic number
		}
		for (int i = 0; i < message.length; i++) {
			data[i + 6] = message[i]; // input message content
		}
		return data;
	}

	/*
	 * createPackets method. Takes the type of packet, topic number, message and
	 * destination address. Returns an array of one or more packets, with the
	 * message broken up and each packet having a different packet number.
	 */
	protected DatagramPacket[] createPackets(int type, int topicNumber, String message, InetSocketAddress dstAddress) {
		int maxMessageSize = PACKETSIZE - 6;
		byte[] tmpArray = message.getBytes();
		byte[] messageArray = new byte[tmpArray.length + 1];
		for (int i = 0; i < tmpArray.length; i++) {
			messageArray[i] = tmpArray[i];
		}
		messageArray[tmpArray.length] = 0;
		int numberOfPackets = 0;
		for (int messageLength = messageArray.length; messageLength > 0; messageLength -= maxMessageSize) {
			numberOfPackets++;
		}
		DatagramPacket[] packets = new DatagramPacket[numberOfPackets];
		// packetsAwaitingAck = new DatagramPacket[numberOfPackets];
		int offset = 0;
		for (int packetNumber = 0; packetNumber < numberOfPackets; packetNumber++) {
			byte[] dividedMessage;
			if (messageArray.length - offset > maxMessageSize) {
				dividedMessage = new byte[maxMessageSize];
			} else {
				dividedMessage = new byte[messageArray.length - offset];
			}
			for (int j = offset; j - offset < dividedMessage.length; j++) {
				dividedMessage[j - offset] = (byte) messageArray[j];
			}
			byte[] data = createPacketData(type, packetNumber, topicNumber, dividedMessage);
			DatagramPacket packet = new DatagramPacket(data, data.length, dstAddress);
			// packetsAwaitingAck[packetNumber] = packet;
			packets[packetNumber] = packet;
			// packetsAwaitingAck.add(packetNumber);
			offset += maxMessageSize;
		}
		return packets;
	}

	/*
	 * add packets to the queue
	 */
	protected void addToReceivedPackets(DatagramPacket packet) {
		byte[] data = packet.getData();
		int packetNumber = (int) data[1];
		if (!receivedPackets.containsKey(packetNumber)) {
			receivedPackets.put(packetNumber, packet);
		}
	}

	/*
	 * processMessageFromPacket method. Parses a packet and returns a string from
	 * the message field of the packet.
	 */
	protected String processMessageFromPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		char[] stringInChars = new char[data.length];
		for (int index = 6; index < data.length && data[index] != 0; index++) {
			stringInChars[index - 6] = (char) data[index];
		}
		String topic = "";
		for (int index = 0; index < stringInChars.length && stringInChars[index] != 0; index++) {
			topic = topic + stringInChars[index];
		}
		return topic;
	}

	protected String printPacketContent(DatagramPacket packet) {
		byte[] data = packet.getData();
		byte[] topicNumberInBytes = new byte[4];
		for (int index = 2; index < 6; index++) {
			topicNumberInBytes[index - 2] = data[index];
		}
		ByteBuffer wrapped = ByteBuffer.wrap(topicNumberInBytes);
		int topicNumber = wrapped.getInt();
		String packetContent = ("Type: " + data[0] + " PacketNo: " + data[1] + " TopicNo: " + topicNumber + "\n"
				+ "Message: " + processMessageFromPacket(packet));
		return packetContent;
	}

	/*
	 * Sends all the packets in an array list of packets
	 * 
	 * features: loop through array of packets send a packet wait for a response
	 * send next packet
	 * 
	 */
	/*
	 * TODO make the sendPackets method add the packets to the queue if its empty
	 * otherwise add them to an hashmap of arrays that are waiting to be sent
	 * 
	 * make this a new method which is looping all the time while the hash map of
	 * items waiting in the queue is not empty
	 * 
	 * change queue to something with a better name
	 */
	protected synchronized void sendPackets(DatagramPacket[] packets) throws InterruptedException {
		sendState = Constants.NODE_BUSY;
		DatagramPacket packet = packets[0];
		packetsToSend = packets;
		while (packetsToSend != null) {
			System.out.println("sending packets");
			int maxWindowSize = 3;
			int windowSize = 0;
			if (packets.length < maxWindowSize)
				windowSize = packets.length;
			else
				windowSize = maxWindowSize;
			DatagramPacket[] currentFrame = new DatagramPacket[windowSize];
			int frameIndex = 0;
			for (int index = 0; index < packets.length && frameIndex < currentFrame.length; index++) {
				if (packets[index] != null) {
					currentFrame[frameIndex] = packets[index];
					frameIndex++;
				}
			}
			for (int index = 0; index < currentFrame.length; index++) {
				if (currentFrame[index] != null) {
					try {
						socket.send(currentFrame[index]);
						//this.wait(200);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			//this.wait(2000);
			if (safeToSend(packet) == 1) {
				packetsToSend = null;
				sendEndOfTransmission(packet);
			} else if (safeToSend(packet) == 3) {
				packetsToSend = null;
			}
		}
		sendState = Constants.IDLE;
	}

	protected synchronized void removeAckedPacketFromPacketsToSend(DatagramPacket packet) {
		byte[] data = packet.getData();
		byte packetNumber = data[1];
		int packetNumberIndex = (int) packetNumber;
		if (packetsToSend != null) {
			DatagramPacket sendingPacket = null;
			for (int index = 0; sendingPacket == null && index < packetsToSend.length; index++) {
				sendingPacket = packetsToSend[index];
			}
			if (sendingPacket != null && sendingPacket.getPort() == currentPacketPort) {
				packetsToSend[packetNumberIndex] = null;
			} else if (sendingPacket == null) {
				packetsToSend = null;
			}
		} else if (packetsToSend == null) {
			state = Constants.IDLE;
		}
	}

	protected synchronized void addItemsToQueue(DatagramPacket[] packets) {
		queue.put(queue.size(), packets);
	}

	protected synchronized void sendQueue() throws InterruptedException {
		if (!queue.isEmpty() && sendState != Constants.NODE_BUSY) {
			DatagramPacket[] packets = queue.get(queue.firstKey());
			queue.remove(queue.firstKey());
			sendPackets(packets);
		}
	}

	protected synchronized int safeToSend(DatagramPacket packet) {
		int safeToSend = 1;
		for (int index = 0; packetsToSend != null && index < packetsToSend.length; index++) {
			if (packetsToSend[index] != null)
				safeToSend = 2;
		}
		byte[] data = packet.getData();
		if (data[0] == Constants.ENDOFTRANSMISSION) {
			safeToSend = 3;
		}
		return safeToSend;
	}

	protected boolean checkIsEndOfCurrentPub(DatagramPacket packet) {
		if (currentPacketPort == packet.getPort()) {
			return true;
		} else
			return false;
	}

	protected void sendAck(DatagramPacket receivedPacket) {
		byte[] data = receivedPacket.getData();
		int packetNumber = data[1];
		byte[] confirmationPacketContent = createPacketData(Constants.ACK, packetNumber, 0, "".getBytes());
		DatagramPacket confirmation = new DatagramPacket(confirmationPacketContent, confirmationPacketContent.length);
		confirmation.setSocketAddress(receivedPacket.getSocketAddress());
		System.out.println("Ack sent");
		try {
			socket.send(confirmation);
			this.wait(20);
			// socket.send(confirmation);
			// this.wait(20);
			// socket.send(confirmation);
		} catch (IOException | InterruptedException e) {
			System.out.println("Subscriber failed to send confirmation message to broker.");
		}
	}

	protected void sendEndOfTransmission(DatagramPacket receivedPacket) throws InterruptedException {
		DatagramPacket[] packets = new DatagramPacket[1];
		byte[] data = receivedPacket.getData();
		int packetNumber = data[1];
		byte[] confirmationPacketContent = createPacketData(Constants.ENDOFTRANSMISSION, packetNumber, 0,
				"".getBytes());
		DatagramPacket confirmation = new DatagramPacket(confirmationPacketContent, confirmationPacketContent.length);
		confirmation.setSocketAddress(receivedPacket.getSocketAddress());
		packets[0] = confirmation;
		addItemsToQueue(packets);
		/*
		 * try { socket.send(confirmation); this.wait(20); //socket.send(confirmation);
		 * //this.wait(20); //socket.send(confirmation); } catch (IOException |
		 * InterruptedException e) { System.out.
		 * println("Subscriber failed to send confirmation message to broker."); }
		 */
	}

	public abstract void onReceipt(DatagramPacket packet);

	/**
	 *
	 * Listener thread
	 * 
	 * Listens for incoming packets on a datagram socket and informs registered
	 * receivers about incoming packets.
	 */
	class Listener extends Thread {

		/*
		 * Telling the listener that the socket has been initialized
		 */
		public void go() {
			latch.countDown();
		}

		/*
		 * Listen for incoming packets and inform receivers
		 */
		public void run() {
			try {
				latch.await();
				// Endless loop: attempt to receive packet, notify receivers, etc
				while (true) {
					DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);
					socket.receive(packet);
					onReceipt(packet);
				}
			} catch (Exception e) {
				if (!(e instanceof SocketException))
					e.printStackTrace();
			}
		}
	}

	/*
	class Sender extends Thread {
		public void run() {
			while (true) {
				try {
					//pendingPrint = "running";
					if(!queue.isEmpty()) {
						sendQueue();
					}
					waitSome();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		private synchronized void waitSome() {
			try {
				wait(Math.round(Math.random() * 5000));
			} catch (Exception e) {
			}
		}
	}

	class Writer extends Thread {
		Terminal terminal;
		
		Writer(Terminal terminal){
			this.terminal = terminal;
		}
		public void run() {
			while (true) {
				if (pendingPrint != "") {
					terminal.println(pendingPrint);
					pendingPrint = "";
				}
				waitSome();
			}
		}

		private synchronized void waitSome() {
			try {
				wait(Math.round(Math.random() * 5000));
			} catch (Exception e) {
			}
		}
	}
	*/
}