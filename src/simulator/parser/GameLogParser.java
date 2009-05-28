package simulator.parser;

import java.util.ArrayList;
import java.util.LinkedList;

import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.BankStatus;
import edu.umich.eecs.tac.props.UserClickModel;
import edu.umich.eecs.tac.TACAAConstants;
import edu.umich.eecs.tac.Parser;
import se.sics.tasim.props.StartInfo;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.logtool.ParticipantInfo;
import se.sics.tasim.logtool.LogReader;
import se.sics.isl.transport.Transportable;


/**
 * <code>BankStatusParser</code> is a simple example of a TAC AA
 * parser that prints out all advertiser's BankStatus received in
 * a simulation from the simulation log file.<p>
 * <p/>
 * The class <code>Parser</code> is inherited to
 * provide base functionality for TAC AA log processing.
 *
 * @author - Lee Callender
 * 
 * @see edu.umich.eecs.tac.Parser
 */
public class GameLogParser extends Parser {
    private int day = 0;
    private String[] participantNames;
    private boolean[] is_Advertiser;
    private ParticipantInfo[] participants;
    
    LinkedList<SimParserMessage> _messages;

    public GameLogParser(LogReader reader) {
        super(reader);

        participants = reader.getParticipants();
        if (participants == null) {
            throw new IllegalStateException("no participants");
        }
        int agent;
        participantNames = new String[participants.length];
        is_Advertiser = new boolean[participants.length];
        for (int i = 0, n = participants.length; i < n; i++) {
            ParticipantInfo info = participants[i];
            agent = info.getIndex();
            System.out.println(info.getName() + ": " + agent);
            participantNames[agent] = info.getName();
            if (info.getRole() == TACAAConstants.ADVERTISER) {
                is_Advertiser[agent] = true;
            } else
                is_Advertiser[agent] = false;
        }
        _messages = new LinkedList<SimParserMessage>();
    }

    // -------------------------------------------------------------------
    // Callbacks from the parser.
    // Please see the class edu.umich.eecs.tac.Parser for more callback
    // methods.
    // -------------------------------------------------------------------


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
    	_messages.add(parseMessage);
    }

	public LinkedList<SimParserMessage> getMessages() {
		return _messages;
	}

}
