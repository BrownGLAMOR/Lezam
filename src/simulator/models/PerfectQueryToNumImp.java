package simulator.models;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.querytonumimp.BasicQueryToNumImp;
import newmodels.usermodel.AbstractUserModel;

public class PerfectQueryToNumImp extends BasicQueryToNumImp {

	public PerfectQueryToNumImp(PerfectUserModel userModel) {
		super(userModel);
	}

}
