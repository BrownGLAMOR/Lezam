package newmodels.prconv;

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
import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class WEKAPrConv extends AbstractConversionModel {

	FastVector _allAttributes;
	Instances _data;
	Classifier _predictor;
	int _idx;
	double _weight;

	public WEKAPrConv(int idx,double weight) {
		_idx = idx;
		_weight = weight;
		Attribute prClickAttribute = new Attribute("prclick");
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
		_allAttributes.addElement(prClickAttribute);
		_allAttributes.addElement(queryAttribute);
		Random random = new Random();
		_data = new Instances("data"+random.nextLong(),_allAttributes,100);
		_data.setClassIndex(0);

		_predictor = getClassifier(idx);
		
		try {
			_predictor.buildClassifier(_data);
		} catch (Exception e) {
//			e.printStackTrace();
		}
	}

	@Override
	public double getPrediction(Query query) {
		Instance pred = new Instance(2);
		pred.setValue((Attribute)_allAttributes.elementAt(1),query.getManufacturer() + "-" + query.getComponent());
		pred.setDataset(_data);
		double prediction = 0.0;
		try {
			prediction = _predictor.classifyInstance(pred);
		} catch (Exception e) {
//			e.printStackTrace();
		}
		
		if(prediction <= 0.0) {
			return 0.0;
		}
		
		double bound;
		if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			bound = .3;
		}
		else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			bound = .4;
		}
		else {
			bound = .5;
		}

		if(prediction > bound) {
			return bound;
		}

		return prediction;
	}

	@Override
	public String toString() {
        switch (_idx) {
        case 1:  return "WEKAPrConvLinearRegression), weight: " + _weight + ")";
        case 2:  return "WEKAPrConvIBk), weight: " + _weight + ")";
        case 3:  return "WEKAPrConvKStar), weight: " + _weight + ")";
        case 4: return "WEKAPrConvLWL), weight: " + _weight + ")";
        case 5: return "WEKAPrConvAdditiveRegression), weight: " + _weight + ")";
        case 6:  return "WEKAPrConvREPTree), weight: " + _weight + ")";
        case 7:  return "WEKAPrConvRegressionByDiscretization), weight: " + _weight + ")";
        default: return "WEKAPrConvLinearRegression), weight: " + _weight + ")";
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
			Instance newInstance = new Instance(2);
			double numClicks = queryReport.getClicks(query);
			double numConvs = salesReport.getConversions(query);
			double convPr;
			if(numClicks == 0 || numConvs == 0) {
				convPr = 0.0;
			}
			else {
				convPr = numConvs / numClicks;
			}
			newInstance.setValue((Attribute)_allAttributes.elementAt(0),convPr);
			newInstance.setValue((Attribute)_allAttributes.elementAt(1),query.getManufacturer() + "-" + query.getComponent());
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
            case 4:  classifier = (Classifier)new LWL(); break;
            case 5:  classifier = (Classifier)new AdditiveRegression(); break;
            case 6:  classifier = (Classifier)new REPTree(); break;
            case 7:  classifier = (Classifier)new RegressionByDiscretization(); break;
            default: classifier = (Classifier)new LinearRegression(); break;
        }
		return classifier;
	}

	@Override
	public AbstractModel getCopy() {
		return new WEKAPrConv(_idx,_weight);
	}

	@Override
	public void setSpecialty(String manufacturer, String component) {
	}

	@Override
	public double getPredictionWithBid(Query query, double bid) {
		return getPrediction(query);
	}

	@Override
	public double getPredictionWithPos(Query query, double pos) {
		return getPrediction(query);
	}

	@Override
	public void setTimeHorizon(int min) {
	}
}
