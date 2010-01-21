package carleton.oldmodels.unitssold;

import edu.umich.eecs.tac.props.SalesReport;

/**
 * Predicts the number of units sold over the distribution window.
 * This includes yesterday, which is not yet known
 * @author cjc
 *
 */
public interface UnitsSoldModel {

	public void updateReport(SalesReport salesReport);
	
	public int getTotalSold();
	public int getWindowSold();
	public int getYesterday();
	
}
