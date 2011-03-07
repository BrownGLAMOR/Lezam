package simulator.parser;

import edu.umich.eecs.tac.Parser;
import edu.umich.eecs.tac.TACAAConstants;
import edu.umich.eecs.tac.props.UserPopulationState;
import se.sics.isl.transport.Transportable;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;

import java.util.LinkedList;


/**
 * @author jberg
 */
public class GameLogParser extends Parser {
   private String[] _participantNames;
   private boolean[] _is_Advertiser;
   private ParticipantInfo[] _participants;
   private int _day;

   LinkedList<SimParserMessage> _messages;

   public GameLogParser(LogReader reader) {
      super(reader);

      _day = -1;
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
         } else {
            _is_Advertiser[agent] = false;
         }
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
      SimParserMessage parseMessage = new SimParserMessage(sender, receiver, _day, content);
      _messages.addLast(parseMessage);
   }

   protected void dataUpdated(int agent, int type, Transportable content) {
      if (content instanceof UserPopulationState) {
         SimParserMessage parseMessage = new SimParserMessage(agent, type, _day, content);
         _messages.addLast(parseMessage);
      }
   }

   protected void nextDay(int date, long serverTime) {
      _day = date;
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
