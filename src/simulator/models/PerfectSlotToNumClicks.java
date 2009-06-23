package simulator.models;

/*
 * NB
 * 
 * This class should no longer be used since this isn't actually a function.
 * Use Bid -> Click models, etc.
 * 
 * contact jberg if there is a problem with this :)
 * 
 */
//
//import edu.umich.eecs.tac.props.BidBundle;
//import edu.umich.eecs.tac.props.Query;
//import edu.umich.eecs.tac.props.QueryReport;
//import edu.umich.eecs.tac.props.SalesReport;
//import newmodels.querytonumimp.AbstractQueryToNumImp;
//import newmodels.slottonumclicks.AbstractSlotToNumClicks;
//import newmodels.slottoprclick.AbstractSlotToPrClick;
//
//public class PerfectSlotToNumClicks extends AbstractSlotToNumClicks {
//
//	private AbstractSlotToPrClick _clickPrModel;
//	private AbstractQueryToNumImp _numImpModel;
//
//	public PerfectSlotToNumClicks(AbstractSlotToPrClick clickPrModel, AbstractQueryToNumImp numImpModel, Query query) {
//		super(query);
//		_clickPrModel = clickPrModel;
//		_numImpModel = numImpModel;
//	}
//
//	@Override
//	public int getPrediction(double slot) {
//		return (int) (_clickPrModel.getPrediction(slot)*_numImpModel.getPrediction(_query));
//	}
//
//	@Override
//	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
//		return true;
//	}
//
//}
