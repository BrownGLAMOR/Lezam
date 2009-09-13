package simulator.models;

import java.util.LinkedList;

import newmodels.AbstractModel;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPrClick extends AbstractBidToPrClick {

	private BasicSimulator _simulator;
	
	public PerfectBidToPrClick(BasicSimulator simulator) {
		_simulator = simulator;
	}

	@Override
	public double getPrediction(Query query, double currentBid, Ad currentAd){
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(query, currentBid);
		double avgPrClick = 0;
		for(Reports report : reports) {
			if(report.getQueryReport().getImpressions(query) != 0) {
				avgPrClick += report.getQueryReport().getClicks(query)/((double)report.getQueryReport().getImpressions(query));
			}
		}
		avgPrClick = avgPrClick/((double) reports.size());
		return avgPrClick;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new PerfectBidToPrClick(_simulator);
	}

	@Override
	public void setSpecialty(String manufacturer, String component) {
		//not needed
	}

	@Override
	public String toString() {
		return "PerfectBidToPrClick";
	}

	@Override
	public void updatePredictions(BidBundle otherBidBundle) {
		//Not needed
	}

}
