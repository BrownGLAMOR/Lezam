package models.postocpc;

import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LeastMedSq;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.RBFNetwork;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.functions.SimpleLinearRegression;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.lazy.LWL;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.RegressionByDiscretization;
import weka.classifiers.rules.ConjunctiveRule;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.M5Rules;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.M5P;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.m5.M5Base;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import models.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class WEKAPosToCPC extends AbstractPosToCPC {

	FastVector _allAttributes;
	Instances _data;
	Classifier _predictor;
	int _idx;
	double _weight;

	public WEKAPosToCPC(int idx,double weight) {
		_idx = idx;
		_weight = weight;
		Attribute posAttribute = new Attribute("pos");
		Attribute cpcAttribute = new Attribute("cpc");
		FastVector fvQuery = new FastVector(16);
		fvQuery.addElement("null-null");
		fvQuery.addElement("lioneer-null");
		fvQuery.addElement("null-tv");
		fvQuery.addElement("lioneer-tv");
		fvQuery.addElement("null-audio");
		fvQuery.addElement("lioneer-audio");
		fvQuery.addElement("null-dvd");
		fvQuery.addElement("lioneer-dvd");
		fvQuery.addElement("pg-null");
		fvQuery.addElement("pg-tv");
		fvQuery.addElement("pg-audio");
		fvQuery.addElement("pg-dvd");
		fvQuery.addElement("flat-null");
		fvQuery.addElement("flat-tv");
		fvQuery.addElement("flat-audio");
		fvQuery.addElement("flat-dvd");
		Attribute queryAttribute = new Attribute("query",fvQuery);
		_allAttributes = new FastVector(3);
		_allAttributes.addElement(posAttribute);
		_allAttributes.addElement(cpcAttribute);
		_allAttributes.addElement(queryAttribute);
		Random random = new Random();
		_data = new Instances("data"+random.nextLong(),_allAttributes,100);
		_data.setClassIndex(1);

//		Instance query1 = new Instance(3);
//		query1.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query1.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query1.setValue((Attribute)_allAttributes.elementAt(2),"null-null");
//		_data.add(query1);
//
//		Instance query2 = new Instance(3);
//		query2.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query2.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query2.setValue((Attribute)_allAttributes.elementAt(2),"lioneer-null");
//		_data.add(query2);
//
//		Instance query3 = new Instance(3);
//		query3.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query3.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query3.setValue((Attribute)_allAttributes.elementAt(2),"null-tv");
//		_data.add(query3);
//
//		Instance query4 = new Instance(3);
//		query4.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query4.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query4.setValue((Attribute)_allAttributes.elementAt(2),"lioneer-tv");
//		_data.add(query4);
//
//		Instance query5 = new Instance(3);
//		query5.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query5.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query5.setValue((Attribute)_allAttributes.elementAt(2),"null-audio");
//		_data.add(query5);
//
//		Instance query6 = new Instance(3);
//		query6.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query6.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query6.setValue((Attribute)_allAttributes.elementAt(2),"lioneer-audio");
//		_data.add(query6);
//
//		Instance query7 = new Instance(3);
//		query7.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query7.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query7.setValue((Attribute)_allAttributes.elementAt(2),"null-dvd");
//		_data.add(query7);
//
//		Instance query8 = new Instance(3);
//		query8.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query8.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query8.setValue((Attribute)_allAttributes.elementAt(2),"lioneer-dvd");
//		_data.add(query8);
//
//		Instance query9 = new Instance(3);
//		query9.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query9.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query9.setValue((Attribute)_allAttributes.elementAt(2),"pg-null");
//		_data.add(query9);
//
//		Instance query10 = new Instance(3);
//		query10.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query10.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query10.setValue((Attribute)_allAttributes.elementAt(2),"pg-tv");
//		_data.add(query10);
//
//		Instance query11 = new Instance(3);
//		query11.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query11.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query11.setValue((Attribute)_allAttributes.elementAt(2),"pg-audio");
//		_data.add(query11);
//
//		Instance query12 = new Instance(3);
//		query12.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query12.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query12.setValue((Attribute)_allAttributes.elementAt(2),"pg-dvd");
//		_data.add(query12);
//
//		Instance query13 = new Instance(3);
//		query13.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query13.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query13.setValue((Attribute)_allAttributes.elementAt(2),"flat-null");
//		_data.add(query13);
//
//		Instance query14 = new Instance(3);
//		query14.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query14.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query14.setValue((Attribute)_allAttributes.elementAt(2),"flat-tv");
//		_data.add(query14);
//
//		Instance query15 = new Instance(3);
//		query15.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query15.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query15.setValue((Attribute)_allAttributes.elementAt(2),"flat-audio");
//		_data.add(query15);
//
//		Instance query16 = new Instance(3);
//		query16.setValue((Attribute)_allAttributes.elementAt(0),0.0);
//		query16.setValue((Attribute)_allAttributes.elementAt(1),0.0);
//		query16.setValue((Attribute)_allAttributes.elementAt(2),"flat-dvd");
//		_data.add(query16);

		_predictor = getClassifier(idx);
		
		try {
			_predictor.buildClassifier(_data);
		} catch (Exception e) {
//			e.printStackTrace();
		}
	}

	@Override
	public double getPrediction(Query query, double pos) {
		if(Double.isNaN(pos) || pos > 5) {
			return 0;
		}
		Instance pred = new Instance(3);
		pred.setValue((Attribute)_allAttributes.elementAt(0),pos);
		pred.setValue((Attribute)_allAttributes.elementAt(2),query.getManufacturer() + "-" + query.getComponent());
		pred.setDataset(_data);
		double prediction = 0.0;
		try {
			prediction = _predictor.classifyInstance(pred);
		} catch (Exception e) {
//			e.printStackTrace();
		}
		
		if(prediction < 0.0) {
			return 0.0;
		}

		return prediction;
	}

	@Override
	public String toString() {
        switch (_idx) {
        case 1:  return "WEKAPosToCPC(LinearRegression), weight: " + _weight + ")";
        case 2:  return "WEKAPosToCPC(IBk), weight: " + _weight + ")";
        case 3:  return "WEKAPosToCPC(KStar), weight: " + _weight + ")";
        case 4: return "WEKAPosToCPC(LWL), weight: " + _weight + ")";
        case 5: return "WEKAPosToCPC(AdditiveRegression), weight: " + _weight + ")";
        case 6:  return "WEKAPosToCPC(REPTree), weight: " + _weight + ")";
        case 7:  return "WEKAPosToCPC(RegressionByDiscretization), weight: " + _weight + ")";
        default: return "WEKAPosToCPC(LinearRegression), weight: " + _weight + ")";
        }
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bidBundle) {

		if(_weight > 0.0 && _weight != 0) {
			/*
			 * Reweight old data
			 */
			int numDays = (int) (_data.numInstances()/16.0);
			for(int i = 0; i < _data.numInstances(); i++) {
				int idx = (int) (i/16.0);
				_data.instance(i).setWeight(Math.pow(_weight, numDays - idx));
			}
		}
		
		for(Query query : queryReport) {
			Instance newInstance = new Instance(3);
			double pos = queryReport.getPosition(query);
			if(Double.isNaN(pos)) {
				pos = 6.0;
			}
			double cpc = queryReport.getCPC(query);
			if(Double.isNaN(cpc)) {
				cpc = 0.0;
			}
			newInstance.setValue((Attribute)_allAttributes.elementAt(0),pos);
			newInstance.setValue((Attribute)_allAttributes.elementAt(1),cpc);
			newInstance.setValue((Attribute)_allAttributes.elementAt(2),query.getManufacturer() + "-" + query.getComponent());
			_data.add(newInstance);
		}
		try {
			_predictor.buildClassifier(_data);
		} catch (Exception e) {
//			e.printStackTrace();
		}
		
		return true;
	}
	
	public Classifier getClassifier(int idx) {
		Classifier classifier;
        switch (idx) {
            case 1:  classifier = (Classifier)new LinearRegression(); break;
            case 2:  classifier = (Classifier)new IBk(); break;
            case 3:  classifier = (Classifier)new KStar(); break;
            case 4: classifier = (Classifier)new LWL(); break;
            case 5: classifier = (Classifier)new AdditiveRegression(); break;
            case 6:  classifier = (Classifier)new REPTree(); break;
            case 7:  classifier = (Classifier)new RegressionByDiscretization(); break;
            default: classifier = (Classifier)new LinearRegression(); break;
        }
		return classifier;
	}

	@Override
	public AbstractModel getCopy() {
		return new WEKAPosToCPC(_idx,_weight);
	}
}