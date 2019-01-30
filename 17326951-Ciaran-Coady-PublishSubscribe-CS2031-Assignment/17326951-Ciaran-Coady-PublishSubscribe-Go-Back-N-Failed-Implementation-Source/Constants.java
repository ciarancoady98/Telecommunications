package cs.tcd.ie;

public class Constants {
	
	static final String DEFAULT_DST_NODE = "localhost";	
	
	// Publisher
	static final int TEST_PUB_PORT = 50002;
	
	// Broker
	static final int TEST_BKR_PORT = 50003;
	
	// Subscriber
	static final int TEST_SUB_PORT = 50004;
	
	// Subscriber state
	static final int SUB_DEFAULT_STATE = 0;
	static final int SUB_AWAIT_NEW_PACKET = 1;
	static final int SUB_RECEIVING_PUB = 2;
	
	// Broker State
	static final int BKR_DEFAULT_STATE = 0;
	static final int BKR_AWAIT_NEW_TOPIC = 1;
	static final int BKR_RECEIVING_PUB = 2;
	
	static final int NODE_BUSY = 22;
	static final int IDLE = 23;
	static final int TRANSMITTING = 24;
	static final int RECEIVING = 25;
	
	
	// Packet
	static final byte ACK = 0;
	static final byte NAK = 1;
	static final byte RTS = 2;
	static final byte CTS = 3;
	static final byte CREATION = 4;
	static final byte PUBLICATION = 5;
	static final byte SUBSCRIPTION = 6;
	static final byte UNSUBSCRIPTION = 7;
	static final byte CHECKTOPIC = 8;
	static final byte ENDOFTRANSMISSION = 9;

}
