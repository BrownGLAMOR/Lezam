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
	private int _day;
	
	public SimParserMessage(int sender, int receiver, int day, Transportable content) {
		_sender = sender;
		_receiver = receiver;
		_day = day;
		_content = content;
	}

	public Transportable getContent() {
		return _content;
	}

	public int getReceiver() {
		return _receiver;
	}
	
	public int getDay() {
		return _day;
	}

	public int getSender() {
		return _sender;
	}

}
