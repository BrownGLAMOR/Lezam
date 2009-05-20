package modelers.positiontoclick;

import edu.umich.eecs.tac.props.Query;

public interface PositionClicksModel {
	
	/**
	 * return the number of clicks to recived by the given slot
	 * 
	 * @param slot
	 * @return number of clicks
	 */
	int getClicks(Query q, int slot);

}
