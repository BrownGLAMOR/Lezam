package simulator;

import java.util.HashMap;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class Reports {

	private QueryReport _queryReport;
	private SalesReport _salesReport;
	private int _numReports;

	/*
	 * USE THESE TO SAVE SPACE ON HOLDING LOTS OF REPORTS!
	 * 
	 * use the add functions
	 * 
	 * this is a total hack and don't use the add and get functions
	 * unless you know what you are doing!
	 */
	private HashMap<Query,Double> _clicks;
	private HashMap<Query,Double> _cost;
	private HashMap<Query,Double> _CPC;
	private HashMap<Query,Double> _convPr;
	private HashMap<Query,Double> _clickPr;
	private HashMap<Query,Double> _regimps;
	private HashMap<Query,Double> _promimps;
	private HashMap<Query,Double> _avgPos;
	private HashMap<Query,Double> _conversions;
	private HashMap<Query,Double> _revenue;

	public Reports(Reports reports) {
		_queryReport = new QueryReport();
		_salesReport = new SalesReport();
		QueryReport queryReport = reports.getQueryReport();
		SalesReport salesReport = reports.getSalesReport();
		for(Query query : queryReport) {
			_queryReport.setClicks(query, queryReport.getClicks(query));
			_queryReport.setCost(query, queryReport.getCost(query));
			_queryReport.setImpressions(query, queryReport.getRegularImpressions(query), queryReport.getPromotedImpressions(query));
			_queryReport.setPositionSum(query, queryReport.getPosition(query) * (queryReport.getRegularImpressions(query) + queryReport.getPromotedImpressions(query)));
			_salesReport.setConversions(query, salesReport.getConversions(query));
			_salesReport.setRevenue(query, salesReport.getRevenue(query));
		}
		_numReports = reports.getNumReports();
		_clicks = new HashMap<Query, Double>();
		_cost = new HashMap<Query, Double>();
		_CPC = new HashMap<Query, Double>();
		_convPr = new HashMap<Query, Double>();
		_clickPr = new HashMap<Query, Double>();
		_regimps = new HashMap<Query, Double>();
		_promimps = new HashMap<Query, Double>();
		_avgPos = new HashMap<Query, Double>();
		_conversions = new HashMap<Query, Double>();
		_revenue = new HashMap<Query, Double>();
		for(Query query : queryReport) {
			_clicks.put(query,(double)_queryReport.getClicks(query));
			_cost.put(query,_queryReport.getCost(query));
			_CPC.put(query,_queryReport.getCPC(query));
			_convPr.put(query,_salesReport.getConversions(query)/((double)_queryReport.getClicks(query)));
			_clickPr.put(query,_queryReport.getClicks(query)/((double)_queryReport.getImpressions(query)));
			_regimps.put(query,(double)_queryReport.getRegularImpressions(query));
			_promimps.put(query,(double)_queryReport.getPromotedImpressions(query));
			_avgPos.put(query,_queryReport.getPosition(query));
			_conversions.put(query,(double)_salesReport.getConversions(query));
			_revenue.put(query,_salesReport.getRevenue(query));
		}
	}

	public Reports(QueryReport queryReport, SalesReport salesReport) {
		_queryReport = queryReport;
		_salesReport = salesReport;
		_numReports = 1;
		_clicks = new HashMap<Query, Double>();
		_cost = new HashMap<Query, Double>();
		_CPC = new HashMap<Query, Double>();
		_convPr = new HashMap<Query, Double>();
		_clickPr = new HashMap<Query, Double>();
		_regimps = new HashMap<Query, Double>();
		_promimps = new HashMap<Query, Double>();
		_avgPos = new HashMap<Query, Double>();
		_conversions = new HashMap<Query, Double>();
		_revenue = new HashMap<Query, Double>();
		for(Query query : queryReport) {
			_clicks.put(query,(double)_queryReport.getClicks(query));
			_cost.put(query,_queryReport.getCost(query));
			_CPC.put(query,_queryReport.getCPC(query));
			_convPr.put(query,_salesReport.getConversions(query)/((double)_queryReport.getClicks(query)));
			_clickPr.put(query,_queryReport.getClicks(query)/((double)_queryReport.getImpressions(query)));
			_regimps.put(query,(double)_queryReport.getRegularImpressions(query));
			_promimps.put(query,(double)_queryReport.getPromotedImpressions(query));
			_avgPos.put(query,_queryReport.getPosition(query));
			_conversions.put(query,(double)_salesReport.getConversions(query));
			_revenue.put(query,_salesReport.getRevenue(query));
		}
	}

	public double getClicks(Query query) {
		return _clicks.get(query);
	}

	public void setClicks(Query query, double clicks) {
		_clicks.put(query, clicks);
	}

	public double getCost(Query query) {
		return _cost.get(query);
	}

	public void setCost(Query query, double cost) {
		_cost.put(query, cost);
	}

	public double getCPC(Query query) {
		return _CPC.get(query);
	}

	public void setCPC(Query query, double CPC) {
		_CPC.put(query, CPC);
	}
	
	public double getConvPr(Query query) {
		return _convPr.get(query);
	}

	public void setConvPr(Query query, double convPr) {
		_convPr.put(query, convPr);
	}
	
	public double getClickPr(Query query) {
		return _clickPr.get(query);
	}

	public void setClickPr(Query query, double convPr) {
		_clickPr.put(query, convPr);
	}

	public double getRegimps(Query query) {
		return _regimps.get(query);
	}

	public void setRegimps(Query query, double regImps) {
		_regimps.put(query, regImps);
	}

	public double getPromimps(Query query) {
		return _promimps.get(query);
	}

	public void setPromimps(Query query, double promImps) {
		_promimps.put(query, promImps);
	}

	public double getAvgPos(Query query) {
		return _avgPos.get(query);
	}

	public void setAvgPos(Query query, double avgPos) {
		_avgPos.put(query, avgPos);
	}

	public double getConversions(Query query) {
		return _conversions.get(query);
	}

	public void setConversions(Query query, double conversions) {
		_conversions.put(query, conversions);
	}

	public double getRevenue(Query query) {
		return _revenue.get(query);
	}

	public void setRevenue(Query query, double revenue) {
		_revenue.put(query,revenue);
	}

	public QueryReport getQueryReport() {
		return _queryReport;
	}

	public SalesReport getSalesReport() {
		return _salesReport;
	}

	public int getNumReports() {
		return _numReports;
	}

	public void addReport(Reports report) {
		_numReports += report.getNumReports();
		double weight = (((_numReports-report.getNumReports())*1.0)/_numReports);
		for(Query query : _queryReport) {
			double ourClicks = this.getClicks(query);
			double ourCost = this.getCost(query);
			double ourCPC = this.getCPC(query);
			double ourConvPr = this.getConvPr(query);
			double ourClickPr = this.getClickPr(query);
			double ourRegimps = this.getRegimps(query);
			double ourPromimps = this.getPromimps(query);
			double ourAvgPos = this.getAvgPos(query);
			double ourConversions = this.getConversions(query);
			double ourRevenue = this.getRevenue(query);
			
			double otherClicks = report.getClicks(query);
			double otherCost = report.getCost(query);
			double otherCPC = report.getCPC(query);
			double otherConvPr = report.getConvPr(query);
			double otherClickPr = report.getClickPr(query);
			double otherRegimps = report.getRegimps(query);
			double otherPromimps = report.getPromimps(query);
			double otherAvgPos = report.getAvgPos(query);
			double otherConversions = report.getConversions(query);
			double otherRevenue = report.getRevenue(query);
			
			if(Double.isNaN(ourCPC)) {
				ourCPC = 0.0;
			}
			if(Double.isNaN(ourAvgPos)) {
				ourAvgPos = 6.0;
			}
			if(Double.isNaN(ourConvPr)) {
				ourConvPr = 0.0;
			}
			if(Double.isNaN(ourClickPr)) {
				ourClickPr = 0.0;
			}
			
			if(Double.isNaN(otherCPC)) {
				otherCPC = 0.0;
			}
			if(Double.isNaN(otherAvgPos)) {
				otherAvgPos = 6.0;
			}
			if(Double.isNaN(otherConvPr)) {
				otherConvPr = 0.0;
			}
			if(Double.isNaN(otherClickPr)) {
				otherClickPr = 0.0;
			}
			
			this.setClicks(query,ourClicks*weight + otherClicks*(1-weight));
			this.setCost(query,ourCost*weight + otherCost*(1-weight));
			this.setCPC(query,ourCPC*weight + otherCPC*(1-weight));
			this.setConvPr(query,ourConvPr*weight + otherConvPr*(1-weight));
			this.setClickPr(query,ourClickPr*weight + otherClickPr*(1-weight));
			this.setRegimps(query,ourRegimps*weight + otherRegimps*(1-weight));
			this.setPromimps(query,ourPromimps*weight + otherPromimps*(1-weight));
			this.setAvgPos(query,ourAvgPos*weight + otherAvgPos*(1-weight));
			this.setConversions(query,ourConversions*weight + otherConversions*(1-weight));
			this.setRevenue(query,ourRevenue*weight + otherRevenue*(1-weight));
		}
	}
}
