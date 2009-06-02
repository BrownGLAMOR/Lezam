package simulator.parser;

import java.util.ArrayList;
import java.util.LinkedList;

import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.ReserveInfo;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.BankStatus;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;
import edu.umich.eecs.tac.props.UserClickModel;
import edu.umich.eecs.tac.props.UserPopulationState;
import edu.umich.eecs.tac.TACAAConstants;
import edu.umich.eecs.tac.Parser;
import se.sics.tasim.props.StartInfo;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.logtool.ParticipantInfo;
import se.sics.tasim.logtool.LogReader;
import se.sics.isl.transport.Transportable;


/**
 *
 * @author jberg
 * 
 */
public class GameLogParser extends Parser {
    private String[] _participantNames;
    private boolean[] _is_Advertiser;
    private ParticipantInfo[] _participants;
    
    LinkedList<SimParserMessage> _messages;

    public GameLogParser(LogReader reader) {
        super(reader);

        _participants = reader.getParticipants();
        if (_participants == null) {
            throw new IllegalStateException("no participants");
        }
        _participantNames = new String[_participants.length];
        _is_Advertiser = new boolean[_participants.length];
        for (int i = 0, n = _participants.length; i < n; i++) {
            ParticipantInfo info = _participants[i];
            int agent = info.getIndex();
            _participantNames[agent] = info.getName();
            if (info.getRole() == TACAAConstants.ADVERTISER) {
                _is_Advertiser[agent] = true;
            } else
                _is_Advertiser[agent] = false;
        }
        _messages = new LinkedList<SimParserMessage>();
    }

    /**
     * Invoked when a message to a specific receiver is encountered in the log
     * file. Example of this is the offers sent by the manufacturers to the
     * customers.
     *
     * @param sender   the sender of the message
     * @param receiver the receiver of the message
     * @param content  the message content
     */
    protected void message(int sender, int receiver, Transportable content) {
    	SimParserMessage parseMessage = new SimParserMessage(sender,receiver,content);
    	_messages.addLast(parseMessage);
    }
    
    protected void dataUpdated(int agent, int type, Transportable content) {
    	if(content instanceof UserPopulationState){
    		//TODO
    	}
    }

	public LinkedList<SimParserMessage> getMessages() {
		return _messages;
	}

	public String[] getParticipantNames() {
		return _participantNames;
	}

	public ParticipantInfo[] getParticipants() {
		return _participants;
	}

	public boolean[] getIsAdvertiser() {
		return _is_Advertiser;
	}

}
