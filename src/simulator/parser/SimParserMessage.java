package simulator.parser;

import se.sics.isl.transport.Transportable;

/**
*
* @author jberg
* 
*/
public class SimParserMessage {
	
	int _sender;
	int _receiver;
	Transportable _content;
	
	public SimParserMessage(int sender, int receiver, Transportable content) {
		_sender = sender;
		_receiver = receiver;
		_content = content;
	}

	public Transportable getContent() {
		return _content;
	}

	public int getReceiver() {
		return _receiver;
	}

	public int getSender() {
		return _sender;
	}

}
