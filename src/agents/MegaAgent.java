package agents;

import java.util.ArrayList;
import java.util.Random;

import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;

public class MegaAgent extends Agent {

	SimAbstractAgent agent;
	ArrayList<Message> _messagesOnHold;
	private Random _R = new Random();

	public MegaAgent() {
		agent = null;
		_messagesOnHold = new ArrayList<Message>();
	}

	@Override
	protected void messageReceived(Message message) {
		if(agent == null) {
			_messagesOnHold.add(message);
			Transportable content = message.getContent();
			if (content instanceof AdvertiserInfo) {
				AdvertiserInfo advertiserInfo = (AdvertiserInfo) content;
				int capacity = advertiserInfo.getDistributionCapacity();
				if(capacity == 300) {
					double rand = _R.nextDouble();
					if(rand >= .5) {
						System.out.println("RUNNING MCKP");
						agent = new MCKPAgentMkIIBids(this);
					}
					else {
						System.out.println("RUNNING G3");
						agent = new G3Agent(this);
					}
				}
				else if(capacity == 400) {
					System.out.println("RUNNING G3");
					agent = new G3Agent(this);
				}
				else if(capacity == 500) {
					System.out.println("RUNNING MCKP");
					agent = new MCKPAgentMkIIBids(this);
				}
				else {
					System.out.println("RUNNING MCKP");
					agent = new MCKPAgentMkIIBids(this);
				}
				while(_messagesOnHold.size() > 0) {
					Message mes = _messagesOnHold.remove(0);
					agent.messageReceived(mes);
				}
			}
		}
		else {
			agent.messageReceived(message);
		}
	}

	@Override
	protected void simulationFinished() {
		if(agent != null) {
			agent.simulationFinished();
		}
	}

	@Override
	protected void simulationSetup() {
	}

	public void sendBundleMessage(String publisherAddress, BidBundle bidBundle) {
		sendMessage(publisherAddress, bidBundle);
	}

}
