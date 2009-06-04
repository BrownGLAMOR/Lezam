package modelers.bidtoposition.ilke;

import java.util.ArrayList;
import java.util.Arrays;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;


public class LeastErrorModel extends PositionToBidModel{
	protected ArrayList<AbstractComparableModel> _candidateModels;
	protected AbstractComparableModel _bestModel;
	protected ArrayList<ModelDataPoint> _data;
	protected int _slots;
	protected final int TEST_DISTANCE = 3;
	
	public LeastErrorModel(ArrayList<AbstractComparableModel> candidateModels, ArrayList<ModelDataPoint> data, int slots) {
		_candidateModels = candidateModels;
		_data = data;
		_slots = slots;
	}
	
	public void train() {
		int dataPoints = _data.size();
		double error[] = new double[_candidateModels.size()];
		
		for(AbstractComparableModel model : _candidateModels) {
			
			for(int d = dataPoints-TEST_DISTANCE; d<dataPoints; d++) {
				ModelDataPoint mp = _data.get(d);
				
				for(int t=0; t<d; t++) {
					model.insertPoint(_data.get(t));
				}
				
				model.train();
				
				error[_candidateModels.indexOf(model)] += Math.abs( (model.getPrediction(mp.getGiven()) - mp.getToBePredicted()) );
			
				model.reset();
			}
			
		}
		
		int minIndex = -1;
		double minError = Integer.MAX_VALUE;
		for(int i=0; i<error.length; i++) {
			if(error[i] < minError) {
				minError = error[i];
				minIndex = i;
			}
		}
		_bestModel = _candidateModels.get(minIndex);
//		System.out.println("LEAST ERROR: " + Arrays.toString(error) + ", picked: " + _bestModel);

		
		for(ModelDataPoint mp : _data) {
			_bestModel.insertPoint(mp);
		}
		
		_bestModel.train();
	}

	@Override
	public double getBid(Query q, double position) {
		Object[] given = new Object[2];
		given[1] = q;
		given[2] = position;
		
		double prediction = getPrediction(given);
		
		return prediction;
	}

	@Override
	public void updateBidBundle(BidBundle bids) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateQueryReport(QueryReport qr) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getPrediction(Object[] given) {
		return _bestModel.getPrediction(given);

	}

	@Override
	public void insertPoint(ModelDataPoint mp) {
		_data.add(mp);
		
	}

	@Override
	public void reset() {
		_data = new ArrayList<ModelDataPoint>();
		_bestModel = null;	
		
		for(AbstractComparableModel model : _candidateModels) {
			model.reset();
		}
	}
	
	public String toString() {
		return "LeastErrorModel with " + Arrays.toString(_candidateModels.toArray());
	}
}
