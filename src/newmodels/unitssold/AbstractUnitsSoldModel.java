package newmodels.unitssold;

import edu.umich.eecs.tac.props.SalesReport;
import newmodels.AbstractModel;

/**
 * Predicts the number of units sold over the distribution window.
 * This includes yesterday, which is not yet known
 * @author cjc
 *
 */
public abstract class AbstractUnitsSoldModel extends AbstractModel{

	public abstract void update(SalesReport salesReport);
	
	public abstract double getWindowSold();
	public abstract double getLatestSample();
	public abstract double getEstimate();
	
}

