package modelers.bidtoposition.ilke;

import java.util.ArrayList;
import java.util.Arrays;

import modelers.bidtoposition.ilke.AbstractComparableModel;
import modelers.bidtoposition.ilke.ModelDataPoint;

public class AbsErrorModelCompare {
	protected ArrayList<AbstractComparableModel> _candidateModels;
	protected ArrayList<ModelDataPoint> _data;
	protected final int TEST_DISTANCE = 10;
	
	public AbsErrorModelCompare(ArrayList<AbstractComparableModel> candidateModels, ArrayList<ModelDataPoint> data) {
		_candidateModels = candidateModels;
		_data = data;
	}
	
	public void compareModels() {
		int dataPoints = _data.size();
		System.out.println(dataPoints);
		double error[] = new double[_candidateModels.size()];
		
		for(AbstractComparableModel model : _candidateModels) {
			System.out.print("Model: " + model +" Errors in each test: ");
			for(int d = dataPoints-TEST_DISTANCE; d<dataPoints; d++) {
				ModelDataPoint mp = _data.get(d);
				
				for(int t=0; t<d; t++) {
					model.insertPoint(_data.get(t));
				}
				
				model.train();
				
				double err = Math.abs( (model.getPrediction(mp.getGiven()) - mp.getToBePredicted()) );
				System.out.print(err + " ");
				
				error[_candidateModels.indexOf(model)] += err ;
			
				model.reset();
			}
			System.out.println();
			
		}
		
		System.out.println("Total Error: " + Arrays.toString(error));
		int minIndex = -1;
		double minError = Integer.MAX_VALUE;
		for(int i=0; i<error.length; i++) {
			if(error[i] < minError) {
				minError = error[i];
				minIndex = i;
			}
		}
		
		AbstractComparableModel bestModel = _candidateModels.get(minIndex);
		
		System.out.println("Best Model is " + bestModel + " with error " + minError);
	}
}
