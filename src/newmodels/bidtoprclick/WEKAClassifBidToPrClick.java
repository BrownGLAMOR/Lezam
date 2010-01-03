package newmodels.bidtoprclick;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.StringTokenizer;

import newmodels.AbstractModel;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LeastMedSq;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.RBFNetwork;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.functions.SimpleLogistic;
import weka.classifiers.lazy.IB1;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.lazy.LWL;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.Dagging;
import weka.classifiers.meta.END;
import weka.classifiers.meta.LogitBoost;
import weka.classifiers.meta.RandomCommittee;
import weka.classifiers.meta.RegressionByDiscretization;
import weka.classifiers.meta.RotationForest;
import weka.classifiers.rules.ConjunctiveRule;
import weka.classifiers.rules.DTNB;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.M5Rules;
import weka.classifiers.rules.NNge;
import weka.classifiers.rules.OneR;
import weka.classifiers.rules.PART;
import weka.classifiers.rules.Prism;
import weka.classifiers.rules.Ridor;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.BFTree;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.FT;
import weka.classifiers.trees.Id3;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.J48graft;
import weka.classifiers.trees.LADTree;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.M5P;
import weka.classifiers.trees.NBTree;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.SimpleCart;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class WEKAClassifBidToPrClick extends AbstractBidToPrClick {

	FastVector _allAttributes;
	Instances _data,_filteredData;
	Classifier _predictor;
	Filter _filter;
	int _idx;

	public WEKAClassifBidToPrClick(int idx) {
		_idx = idx;
		Attribute bidAttribute = new Attribute("bid");
		Attribute clickPrAttribute = new Attribute("clickpr");
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
		_allAttributes.addElement(bidAttribute);
		_allAttributes.addElement(clickPrAttribute);
		_allAttributes.addElement(queryAttribute);
		Random random = new Random();
		_data = new Instances("data"+random.nextLong(),_allAttributes,100);
		//		_data.setClassIndex(1);

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

		_filter = new Discretize();
		((Discretize)_filter).setAttributeIndices("1-2");
		((Discretize)_filter).setBins(30);
		((Discretize)_filter).setFindNumBins(true);
		try {
			((Discretize)_filter).setInputFormat(_data);
		} catch (Exception e1) {
//			e1.printStackTrace();
		}

		_predictor = getClassifier(_idx);

		//		try {
		//			_predictor.buildClassifier(_data);
		//		} catch (Exception e) {
		//			//e.printStackTrace();
		//		}
	}

	@Override
	public double getPrediction(Query query, double bid, Ad currentAd) {
		Enumeration enumer = _filteredData.attribute(0).enumerateValues();
		String[] enumStrings = new String[_filteredData.attribute(0).numValues()];
		int count = 0;
		while(enumer.hasMoreElements()) {
			enumStrings[count] = (String)enumer.nextElement();
			count++;
		}
		int bidIdx = 0;
		for(int i = 0; i < count; i++) {
			StringTokenizer tokenizer = new StringTokenizer(enumStrings[i], "'()-[]");
			String token1 = tokenizer.nextToken();
			String token2 = tokenizer.nextToken();
			double tokeVal1,tokeVal2;
			if(token1.equals("inf")) {
				tokeVal1 = -Double.MAX_VALUE;
			}
			else {
				tokeVal1 = Double.parseDouble(token1);
			}

			if(token2.equals("inf")) {
				tokeVal2 = Double.MAX_VALUE;
			}
			else {
				tokeVal2 = Double.parseDouble(token2);
			}
			if(bid >= tokeVal1 && bid <= tokeVal2) {
				bidIdx = i;
				break;
			}
		}

		Enumeration enumerClickPr = _filteredData.attribute(1).enumerateValues();
		String[] enumClickPrStrings = new String[_filteredData.attribute(1).numValues()];
		int countClickPr = 0;
		while(enumerClickPr.hasMoreElements()) {
			enumClickPrStrings[countClickPr] = (String)enumerClickPr.nextElement();
			countClickPr++;
		}
		ArrayList<Double> tokenVals = new ArrayList<Double>();
		for(int i = 0; i < countClickPr; i++) {
			StringTokenizer tokenizer = new StringTokenizer(enumClickPrStrings[i], "'()-[]");
			String token1 = tokenizer.nextToken();
			String token2;
			if(tokenizer.hasMoreTokens()) {
				token2 = tokenizer.nextToken();
			}
			else {
				token1 = "0.0";
				token2 = "0.0";
			}
			double tokeVal1,tokeVal2;
			if(token1.equals("inf")) {
				tokeVal1 = -Double.MAX_VALUE;
			}
			else {
				tokeVal1 = Double.parseDouble(token1);
			}

			if(token2.equals("inf")) {
				tokeVal2 = Double.MAX_VALUE;
			}
			else {
				tokeVal2 = Double.parseDouble(token2);
			}
			tokenVals.add(tokeVal1);
		}
		tokenVals.add(Double.MAX_VALUE);


		Instance pred = new Instance(3);
		pred.setValue(_filteredData.attribute(0),_filteredData.attribute(0).value(bidIdx));
		pred.setValue(_filteredData.attribute(2),query.getManufacturer() + "-" + query.getComponent());
		pred.setDataset(_filteredData);

		double prediction = bid;
		try {
			//						double predIdx = _predictor.classifyInstance(pred);
			//						
			//						String cpcStr = _filteredData.attribute(1).value((int)predIdx);
			//						StringTokenizer tokenizer = new StringTokenizer(cpcStr, "'()-[]");
			//						String token1 = tokenizer.nextToken();
			//						String token2 = tokenizer.nextToken();
			//						double tokeVal1,tokeVal2;
			//						if(token1.equals("inf")) {
			//							tokeVal1 = 0.0;
			//						}
			//						else {
			//							tokeVal1 = Double.parseDouble(token1);
			//						}
			//						
			//						if(token2.equals("inf")) {
			//							tokeVal2 = bid;
			//						}
			//						else {
			//							tokeVal2 = Double.parseDouble(token2);
			//						}
			//						prediction = (tokeVal1+tokeVal2)/2.0;


			double[] predArr = _predictor.distributionForInstance(pred);
			int[] nominalCounts = _filteredData.attributeStats(1).nominalCounts;
			prediction = 0;
			double[] clickPrArr = _data.attributeToDoubleArray(1);
			for(int i = 0; i < clickPrArr.length; i++) {
				int predIdx = 0;
				double clickPr = clickPrArr[i];
				if(clickPr == 0) {
					continue;
				}
				for(int j = 0; j < tokenVals.size()-1; j++) {
					if(clickPr >= tokenVals.get(j) &&
					clickPr <= tokenVals.get(j+1)) {
						predIdx = j;
						break;
					}
				}
				if(nominalCounts[predIdx] != 0) {
					prediction += (predArr[predIdx]/((double)nominalCounts[predIdx])) * clickPr;
				}
			}
//			System.out.println("bid: " + bid + ", prediction: " +prediction + ", mean or mode: ");
		} catch (Exception e) {
			//e.printStackTrace();
		}

		if(prediction <= 0.0) {
			return 0.0;
		}
		
		double bound;
		if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			bound = .4;
		}
		else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			bound = .5;
		}
		else {
			bound = .6;
		}

		if(prediction > bound) {
			return bound;
		}

		return prediction;
	}

	@Override
	public String toString() {
		switch (_idx) {
		case 1:  return "WEKAClassifBidToCPC(BayesNet)";
		case 2:  return "WEKAClassifBidToCPC(NaiveBayes)";
		case 3:  return "WEKAClassifBidToCPC(IBk)";
		case 4: return "WEKAClassifBidToCPC(KStar)";
		case 5: return "WEKAClassifBidToCPC(LWL)";
		case 6:  return "WEKAClassifBidToCPC(AttributeSelectedClassifier)";
		case 7:  return "WEKAClassifBidToCPC(Bagging)";
		case 8:  return "WEKAClassifBidToCPC(LogitBoost)";
		case 9:  return "WEKAClassifBidToCPC(RandomCommittee)";
		case 10:  return "WEKAClassifBidToCPC(RotationForest)";
		case 11:  return "WEKAClassifBidToCPC(DecisionTable)";
		case 12:  return "WEKAClassifBidToCPC(DTNB)";
		case 13:  return "WEKAClassifBidToCPC(NNge)";
		case 14:  return "WEKAClassifBidToCPC(OneR)";
		case 15:  return "WEKAClassifBidToCPC(PART)";
		case 16:  return "WEKAClassifBidToCPC(Ridor)";
		case 17:  return "WEKAClassifBidToCPC(BFTree)";
		case 18:  return "WEKAClassifBidToCPC(J48)";
		case 19:  return "WEKAClassifBidToCPC(J48graft)";
		case 20:  return "WEKAClassifBidToCPC(NBTree)";
		case 21:  return "WEKAClassifBidToCPC(RandomForest)";
		case 22:  return "WEKAClassifBidToCPC(REPTree)";
		default: return "WEKAClassifBidToCPC(NaiveBayes)";
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport,SalesReport salesReport, BidBundle bidBundle) {
		for(Query query : queryReport) {
			Instance newInstance = new Instance(3);
			double bid = bidBundle.getBid(query);
			double numClicks = queryReport.getClicks(query);
			double numImps = queryReport.getImpressions(query);
			double clickPr;
			if(numClicks == 0 || numImps == 0) {
				clickPr = 0.0;
			}
			else {
				clickPr = numClicks / numImps;
			}
			newInstance.setValue((Attribute)_allAttributes.elementAt(0),bid);
			newInstance.setValue((Attribute)_allAttributes.elementAt(1),clickPr);
			newInstance.setValue((Attribute)_allAttributes.elementAt(2),query.getManufacturer() + "-" + query.getComponent());
			_data.add(newInstance);
		}

		try {
			_filter = new Discretize();
			((Discretize)_filter).setAttributeIndices("1-2");
			((Discretize)_filter).setBins(30);
			((Discretize)_filter).setFindNumBins(true);
			try {
				((Discretize)_filter).setInputFormat(_data);
			} catch (Exception e1) {
//				e1.printStackTrace();
			}
			_filteredData = Filter.useFilter(_data, _filter);
		} catch (Exception e1) {
//			e1.printStackTrace();
		}

		_filteredData.setClassIndex(1);

		//		System.out.println(_filteredData.toString());


		try {
			_predictor.buildClassifier(_filteredData);
		} catch (Exception e) {
			//e.printStackTrace();
		}

		return true;
	}
	
	public Classifier getClassifier(int idx) {
		Classifier classifier;
		switch (idx) {
		case 1:  classifier = (Classifier)new BayesNet(); break;
		case 2:  classifier = (Classifier)new NaiveBayes(); break;
		case 3:  classifier = (Classifier)new IBk(); break;
		case 4: classifier = (Classifier)new KStar(); break;
		case 5: classifier = (Classifier)new LWL(); break;
		case 6:  classifier = (Classifier)new AttributeSelectedClassifier(); break;
		case 7:  classifier = (Classifier)new Bagging(); break;
		case 8:  classifier = (Classifier)new LogitBoost(); break;
		case 9:  classifier = (Classifier)new RandomCommittee(); break;
		case 10:  classifier = (Classifier)new RotationForest(); break;
		case 11:  classifier = (Classifier)new DecisionTable(); break;
		case 12:  classifier = (Classifier)new DTNB(); break;
		case 13:  classifier = (Classifier)new NNge(); break;
		case 14:  classifier = (Classifier)new OneR(); break;
		case 15:  classifier = (Classifier)new PART(); break;
		case 16:  classifier = (Classifier)new Ridor(); break;
		case 17:  classifier = (Classifier)new BFTree(); break;
		case 18:  classifier = (Classifier)new J48(); break;
		case 19:  classifier = (Classifier)new J48graft(); break;
		case 20:  classifier = (Classifier)new NBTree(); break;
		case 21:  classifier = (Classifier)new RandomForest(); break;
		case 22:  classifier = (Classifier)new REPTree(); break;
		default: classifier = (Classifier)new NaiveBayes(); break;
		}
		return classifier;
	}

	@Override
	public AbstractModel getCopy() {
		return new WEKAClassifBidToPrClick(_idx);
	}

	@Override
	public void setSpecialty(String manufacturer, String component) {
	}
}
