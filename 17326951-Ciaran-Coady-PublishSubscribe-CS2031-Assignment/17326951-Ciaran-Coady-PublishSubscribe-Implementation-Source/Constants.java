package cs.tcd.ie;

public class Constants {
	
	static final String DEFAULT_DST_NODE = "localhost";	
	static final int SIZE_OF_HEADER = 4;
	
	// Publisher
	static final int TEST_PUB_PORT = 50002;
	
	// Broker
	static final int TEST_BKR_PORT = 50003;
	
	// Subscriber
	static final int TEST_SUB_PORT = 50004;
	
	// Subscriber state
	static final int SUB_DEFAULT_STATE = 0;
	static final int SUB_AWAIT_NEW_PACKET = 1;
	
	// Broker State
	static final int BKR_DEFAULT_STATE = 0;
	static final int BKR_AWAIT_NEW_TOPIC = 1;
	
	// Packet type
	static final byte ACK = 0;
	static final byte NAK = 1;
	static final byte RTS = 2;
	static final byte CTS = 3;
	static final byte CREATION = 4;
	static final byte PUBLICATION = 5;
	static final byte SUBSCRIPTION = 6;
	static final byte UNSUBSCRIPTION = 7;
	static final byte CHECKTOPIC = 8;

}
