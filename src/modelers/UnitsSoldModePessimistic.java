package modelers;

import java.util.Collection;

import edu.umich.eecs.tac.props.SalesReport;

public class UnitsSoldModePessimistic implements UnitsSoldModel {
	Collection<UnitsSoldModel> _models;
	
	public UnitsSoldModePessimistic(Collection<UnitsSoldModel> models) {
		_models = models;
	}
	
	public void updateReport(SalesReport salesReport){
		for(UnitsSoldModel usm : _models){
			usm.updateReport(salesReport);
		}
	}

	public int getTotalSold() {
		int max = 0;
		for(UnitsSoldModel usm : _models){
			int sold = usm.getTotalSold();
			if(sold > max)
				max = sold;
		}
		return max;
	}

	public int getWindowSold() {
		int max = 0;
		for(UnitsSoldModel usm : _models){
			int sold = usm.getWindowSold();
			if(sold > max)
				max = sold;
		}
		return max;
	}

	public int getYesterday() {
		int max = 0;
		for(UnitsSoldModel usm : _models){
			int sold = usm.getYesterday();
			if(sold > max)
				max = sold;
		}
		return max;
	}
	
}
