package newmodels.targeting;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import newmodels.AbstractModel;


/**
 * This class returns a multiplier for clickPr, convPr, and USP that represents the change if we target.
 * 
 * For F0 we target our specialty
 * For F1 we target the searched term plus our manufacturer or component
 * For F2 we target underlying product for the query
 */

public class BasicTargetModel extends AbstractModel {

	String _manufacturer, _component;
	final double TE = 0.5;
	final double CSB = 0.5;
	final double MSB = 0.5;
	final double PSB = 0.5;
	final double USP = 10;

	public BasicTargetModel(String manufacturer, String component) {
		_manufacturer = manufacturer;
		_component = component;
	}

	private class Tuple {

		double _manufacturerRatio;
		double _componentRatio;

		public Tuple(double man, double comp){
			_manufacturerRatio = man;
			_componentRatio = comp;
		}

		// get methods without the "get", for easier use
		public double manufacturerRatio(){
			return _manufacturerRatio;
		}
		public double componentRatio(){
			return _componentRatio;
		}
	}

	private int toBinary(boolean statement){
		if (statement){
			return 1;
		} else {
			return 0;
		}
	}

	// for the current version, factors only includes targeting, not promoted slot bonus
	private double eta(double clickPr,double factors){
		return (clickPr*factors)/(clickPr*factors + 1 - clickPr);
	}

	private double newUserRatio(double users, double userMultiplier, double nonuserMultiplier){
		return (users*userMultiplier)/(users*userMultiplier + nonuserMultiplier*(1-users));
	}

	private double ratio(double newUsers, double oldUsers, double prUsers, double prOthers){
		return (prUsers*newUsers+prOthers*(1-newUsers))/(prUsers*oldUsers+prOthers*(1-oldUsers));
	}

	private double higherClickPr(double clickPr, int promoted){
		return eta(clickPr,(1+TE)*(1+promoted*PSB))/eta(clickPr,(1+promoted*PSB));
	}

	private double lowerClickPr(double clickPr, int promoted){
		return eta(clickPr,(1-TE)*(1+promoted*PSB))/eta(clickPr,(1+promoted*PSB));
	}

	// ratios of all users without targeting
	private Tuple baseUsers(Query query, double clickPr) {
		double man;
		double comp;
		if (query.getManufacturer() == null && query.getComponent() == null){
			man = 1.0/3.0;
			comp = 1.0/3.0;
		} else if (query.getManufacturer() == null){
			man = 1.0/3.0;
			comp = toBinary(_component.equals(query.getComponent()));
		} else if (query.getComponent() == null){
			man = toBinary(_manufacturer.equals(query.getManufacturer()));
			comp = 1.0/3.0;
		} else {
			man = toBinary(_manufacturer.equals(query.getManufacturer()));
			comp = toBinary(_component.equals(query.getComponent()));
		}
		return new Tuple(man,comp);
	}

	private Tuple targetedUsers(Query query, double clickPr, int promoted) {
		Tuple base = baseUsers(query, clickPr);
		double man = base.manufacturerRatio();
		double comp = base.componentRatio();

		if (query.getManufacturer() == null){
			man = newUserRatio(man,higherClickPr(clickPr,promoted),lowerClickPr(clickPr,promoted));
		}
		if (query.getComponent() == null){
			comp = newUserRatio(comp,higherClickPr(clickPr,promoted),lowerClickPr(clickPr,promoted));
		}

		return new Tuple(man,comp);
	}

	// returns multiplier for your ClickPr (ans * oldClickPr = newClickPr)
	public double getClickPrPrediction(Query query, double clickPr, boolean promoted) {
		double ratio;

		if (query.getType() == QueryType.FOCUS_LEVEL_TWO){
			ratio = higherClickPr(clickPr, toBinary(promoted));
		} else if (query.getType() == QueryType.FOCUS_LEVEL_ONE){
			ratio = 1.0/3.0*higherClickPr(clickPr,toBinary(promoted)) + 2.0/3.0*lowerClickPr(clickPr,toBinary(promoted));
		} else {
			ratio = 1.0/9.0*higherClickPr(clickPr,toBinary(promoted)) + 8.0/9.0*lowerClickPr(clickPr,toBinary(promoted));
		}

		return ratio;
	}
	public double getClickPrPrediction(Query query, double clickPr, double promoted) {
		return promoted*getClickPrPrediction(query, clickPr, true) + (1-promoted)*getClickPrPrediction(query, clickPr, false);
	}

	// returns multiplier for your ConversionPr (ans * oldConvPr = newConvPr)
	public double getConvPrPrediction(Query query, double clickPr, double convPr, boolean promoted) {
		return ratio(targetedUsers(query,clickPr,toBinary(promoted)).componentRatio(),baseUsers(query, clickPr).componentRatio(),eta(convPr,1+CSB),convPr);
	}
	public double getConvPrPrediction(Query query, double clickPr, double convPr, double promoted) {
		return promoted*getConvPrPrediction(query, clickPr, convPr, true) + (1-promoted)*getConvPrPrediction(query, clickPr, convPr, false);
	}

	// returns USP
	public double getUSPPrediction(Query query, double clickPr, boolean promoted) {
		//Old version: return ratio(targetedUsers(query,clickPr,toBinary(promoted)).manufacturerRatio(), baseUsers(query, clickPr).manufacturerRatio(), 1+MSB, 1);
		Tuple targeted = targetedUsers(query,clickPr,toBinary(promoted));
		return targeted.manufacturerRatio()*USP*(1+MSB) + (1-targeted.manufacturerRatio())*USP;
	}
	public double getUSPPrediction(Query query, double clickPr, double promoted) {
		return promoted*getUSPPrediction(query, clickPr, true) + (1-promoted)*getUSPPrediction(query, clickPr, false);
	}

	// ans[0] = clickPr if you hadn't targeted, assuming you did
	// ans[1] = convPr if you hadn't targeted, assuming you did
	public double[] getInversePredictions(Query query, double clickPr, double convPr, boolean promoted) {
		double[] mostRecentPredictions = new double[2];

		mostRecentPredictions[0] = clickPr/getClickPrPrediction(query, clickPr, promoted);
		mostRecentPredictions[1] = convPr/getConvPrPrediction(query, clickPr, convPr, promoted);

		double maxError = Math.abs(0.00001);
		double clickPrError = maxError+1;
		double convPrError = maxError+1;

		int iterations = 0;
		while((clickPrError>maxError || convPrError>maxError) && iterations<=15){
			double tempClickPr = mostRecentPredictions[0];
			mostRecentPredictions[0] = clickPr/getClickPrPrediction(query, tempClickPr, promoted);
			mostRecentPredictions[1] = convPr/getConvPrPrediction(query, tempClickPr, mostRecentPredictions[1], promoted);
			clickPrError = Math.abs(mostRecentPredictions[0]*getClickPrPrediction(query, mostRecentPredictions[0], promoted))-clickPr;
			convPrError = Math.abs(mostRecentPredictions[1]*getConvPrPrediction(query, mostRecentPredictions[0], mostRecentPredictions[1], promoted))-convPr;
			iterations++;
			//			System.out.println(iterations);
		}
		return mostRecentPredictions;
	}
	public double[] getInversePredictions(Query query, double clickPr, double convPr, double promoted) {
		double[] promotedPart = getInversePredictions(query, clickPr, convPr, true);
		double[] unpromotedPart = getInversePredictions(query, clickPr, convPr, false);
		double clickPrPred = promoted*promotedPart[0] + (1-promoted)*unpromotedPart[0];
		double convPrPred = promoted*promotedPart[1] + (1-promoted)*unpromotedPart[1];
		return new double[]{clickPrPred,convPrPred};
	}

	public static void main(String[] args) {
		BasicTargetModel test = new BasicTargetModel("pg","tv");
		//		double clickPr = .27;
		double clickPr = 0.22;
		double convPr = 0.16;
		Query query = new Query("pg","tv");
		double promoted = 1;
		/*for(int i = 0; i< 50; i++) {
			System.out.println(test.getConvPrPrediction(query, .01 * (i+1), convPr, false));

		}*/
		
		double clickPred1 = test.getInversePredictions(query, clickPr, convPr, promoted)[0];
		double convPred1 = test.getInversePredictions(query, clickPr, convPr, promoted)[1];
		double clickPred2 = test.getInversePredictions(query, clickPr, convPr, false)[0];
		double convPred2 = test.getInversePredictions(query, clickPr, convPr, false)[1];
		System.out.println(clickPr);
		System.out.println(convPr);
		System.out.println(clickPred1*test.getClickPrPrediction(query, clickPred1, promoted));
		System.out.println(convPred1*test.getConvPrPrediction(query, clickPred1, convPred1, promoted));
		System.out.println(clickPred1*test.getClickPrPrediction(query, clickPred2, promoted));
		System.out.println(convPred1*test.getConvPrPrediction(query, clickPred2, convPred2, promoted));
		
		//		double[] test3 = test.getInversePredictions(query, clickPr, convPr, promoted);
		//		System.out.println(test3[0]*test.getClickPrPrediction(query, test3[0], promoted));
		//		System.out.println(test3[1]*test.getConvPrPrediction(query, test3[0], test3[1], promoted));
		//		
		//		System.out.println();
		//		
		//		System.out.println(test3[0]);
		//		System.out.println(test3[1]);
		/*double clicksFactor0 = test.getClickPrPrediction(q, clickPr, false);
		double convFactor0 = test.getConvPrPrediction(q, clickPr, convPr, false);

		double predictedClicks1 = clickPr/clicksFactor0;
		double predictedConvs1 = convPr/convFactor0;

		double clicksFactor1 = test.getClickPrPrediction(q, predictedClicks1, false);
		double convFactor1 = test.getConvPrPrediction(q, predictedClicks1, predictedConvs1, false);

		double checkclicks1 = clicksFactor1*predictedClicks1;
		double checkconv1 = convFactor1*predictedConvs1;
		System.out.println(checkclicks1);
		System.out.println(checkconv1);

		double predictedClicks2 = clickPr/clicksFactor1;
		double predictedConvs2 = convPr/convFactor1;

		double clicksFactor2 = test.getClickPrPrediction(q, predictedClicks2, false);
		double convFactor2 = test.getConvPrPrediction(q, predictedClicks2, predictedConvs2, false);

		double checkclicks2 = clicksFactor2*predictedClicks2;
		double checkconv2 = convFactor2*predictedConvs2;
		System.out.println(checkclicks2);
		System.out.println(checkconv2);

		double predictedClicks3 = clickPr/clicksFactor2;
		double predictedConvs3 = convPr/convFactor2;

		double clicksFactor3 = test.getClickPrPrediction(q, predictedClicks3, false);
		double convFactor3 = test.getConvPrPrediction(q, predictedClicks3, predictedConvs3, false);

		double checkclicks3 = clicksFactor3*predictedClicks3;
		double checkconv3 = convFactor3*predictedConvs3;
		System.out.println(checkclicks3);
		System.out.println(checkconv3);*/
	}

	/*0.35243691164119806
0.2223582380812003
0.349882035343172
0.22003581376818002
0.3500056748411616
0.22000054359014434*/

}


