package modelers.unitssold;

import java.util.Iterator;
import java.util.LinkedList;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * An abstract class contianing the common elements of many Units Sold Models.
 * @author cjc
 *
 */
public abstract class UnitsSoldModelBasic implements UnitsSoldModel {
	//protected SalesReport _salesReport;
	protected int _distributionWindow;
	protected LinkedList<Integer> _sold;
	protected int _estimatedLastDaySold;
	
	public UnitsSoldModelBasic(int distributionWindow){
		_distributionWindow = distributionWindow;
		_sold = new LinkedList<Integer>();
	}
	
	public void updateReport(SalesReport salesReport){
		int conversions = 0;
		for(Query q : salesReport){
			conversions += salesReport.getConversions(q);
		}
		
		_sold.addLast(conversions);
		
		_estimatedLastDaySold = updateLastDaySold();
	}
	
	abstract protected int updateLastDaySold();
	
	protected Iterator<Integer> buildWindowIterator(){
		if(_sold.size() > _distributionWindow){
			return _sold.listIterator(_sold.size()-_distributionWindow+1);
		}
		else {
			return _sold.listIterator();
		}
	}
	
	public int getTotalSold(){
		int total = 0;
		for(int i : _sold){
			total += i;
		}
		total += _estimatedLastDaySold;
		return total;
	}
	
	public int getWindowSold(){
		int total = 0;
		Iterator<Integer> iter = buildWindowIterator();
		while(iter.hasNext()){
			total += iter.next();
		}
		
		total += _estimatedLastDaySold;
		return total;
	}
	
	public int getYesterday(){
		return _estimatedLastDaySold;
	}
}
