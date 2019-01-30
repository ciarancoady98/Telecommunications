package cs.tcd.ie;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public abstract class Node {
	static final int PACKETSIZE = /* 65536 60000 */ 40;
	public ArrayList<Integer> packetsAwaitingAck = new ArrayList<Integer>();

	DatagramSocket socket;
	Listener listener;
	CountDownLatch latch;

	//Constructor
	Node() {
		latch = new CountDownLatch(1);
		listener = new Listener();
		listener.setDaemon(true);
		listener.start();
	}

	/*
	 * createPacketData method. Creates an array of bytes for a DatagramPacket and
	 * returns it. Based on custom packet data layout; byte 0 = type, byte 1 =
	 * packet number, byte 2 = sequence number for Go-Back-N, 
	 * bytes 3-5 = topic number, remaining bytes =
	 * message.
	 * 
	 */
	protected byte[] createPacketData(int type, int packetNumber, int sequenceNumber, int topicNumber, byte[] message) {
		byte[] data = new byte[PACKETSIZE];
		data[0] = (byte) type; // Set type
		data[1] = (byte) packetNumber; //Set packet number
		data[2] = (byte) sequenceNumber; //Set sequence number
		data[3] = (byte) topicNumber; // Set topic number
		for (int i = 0; i < message.length; i++) {
			data[i + 4] = message[i]; // input message content
		}
		return data;
	}

	/*
	 * createPackets method. Takes the type of packet, topic number, message and
	 * destination address. Returns an array of one or more packets, with the
	 * message broken up and each packet having a different packet number.
	 */
	protected DatagramPacket[] createPackets(int type, int topicNumber, String message, InetSocketAddress dstAddress) {
		int maxMessageSize = PACKETSIZE - Constants.SIZE_OF_HEADER;
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
		int sequenceNumber = numberOfPackets-1;
		DatagramPacket[] packets = new DatagramPacket[numberOfPackets];
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
			byte[] data = createPacketData(type, packetNumber, sequenceNumber, topicNumber, dividedMessage);
			DatagramPacket packet = new DatagramPacket(data, data.length, dstAddress);
			packets[packetNumber] = packet;
			offset += maxMessageSize;
		}
		return packets;
	}

	/*
	 * lastPacket method. Determines if the received packet is the 
	 * last packet in the current transmission based on how full
	 * the payload section of the packet is.
	 */
	protected boolean lastPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		if (data[data.length - 1] == 0) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * processMessageFromPacket method. Parses a packet and returns a string from
	 * the message field of the packet.
	 */
	protected String processMessageFromPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		char[] stringInChars = new char[data.length];
		for (int index = Constants.SIZE_OF_HEADER; index < data.length && data[index] != 0; index++) {
			stringInChars[index - Constants.SIZE_OF_HEADER] = (char) data[index];
		}
		String topic = "";
		for (int index = 0; index < stringInChars.length && stringInChars[index] != 0; index++) {
			topic = topic + stringInChars[index];
		}
		return topic;
	}

	/*
	 * printPacketContent method. Used for debugging and visualisation
	 * of packet content. returns a string containing the packet content
	 */
	protected String printPacketContent(DatagramPacket packet) {
		byte[] data = packet.getData();
		String packetContent = ("Type: " + data[0] + " PacketNo: " + data[1] + " SequenceNo: " + data[2] + " TopicNo: " + data[3] + "\n"
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
	protected synchronized void sendPackets(DatagramPacket[] packets) throws InterruptedException {
		for (int index = 0; index < packets.length; index++) {
			try {
				socket.send(packets[index]);
				this.wait();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
	 * This class used to keep track of timeouts for packets
	 * each frame contains a packet and a timestamp. if a packet times out
	 * send it again.
	 */
	class frame {
		
	}
}