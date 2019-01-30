


public class Constants {
	
	//controller
	static final int CONTROLLER_PORT_NUMBER = 4998;
	//end users
	static final int ENDUSER_BASE_PORT = 4999;
	//actual ports when end users are intialized
	static final int ENDUSER1_PORT_NUMBER = 5000;
	static final int ENDUSER2_PORT_NUMBER = 5001;
	//switches
	static final int SWITCH_BASE_PORT = 5002;
	//actual ports when switches are intialized
	static final int SWITCH1_PORT_NUMBER = 5003;
	static final int SWITCH2_PORT_NUMBER = 5004;
	static final int SWITCH3_PORT_NUMBER = 5005;
	//number of enduser
	static final int ENDUSER1 = 1;
	static final int ENDUSER2 = 2;
	
	//types of packet
	static final int FEATURES_REQUEST = 0;
	static final int HELLO = 1;
	static final int FEATURES_REPLY = 2;
	static final int PACKET_IN = 4;
	static final int PACKET_OUT = 5;
	static final int FLOW_MOD = 6;
	
	//type of node
	static final int CONTROLLER = 0;
	static final int SWITCH = 1;
	static final int END_NODE = 2;
	
	
	
	
}
