package modelers.bidtoposition.sam;

import edu.umich.eecs.tac.props.Query;

public interface PositionBidModel {
	
	/**
	 * return the minimum bid to recive the given slot
	 * 
	 * @param slot
	 * @return min bid
	 */
	double getBid(Query q, int slot);

}
