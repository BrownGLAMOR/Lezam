package newmodels.slottoprclick;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import newmodels.AbstractModel;

public abstract class NewAbstractPosToPrClick extends AbstractModel {

	public abstract boolean updateModel(QueryReport queryReport);


	public abstract double getPrediction(Query query, double currentPos);


}
