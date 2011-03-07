package models.cpctobid;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.lazy.LWL;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.meta.RegressionByDiscretization;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class WEKACPCToBidPerQuery extends AbstractCPCToBid {

   FastVector _allAttributes;
   HashMap<Query, Instances> _data;
   HashMap<Query, Classifier> _predictor;
   int _idx;
   double _weight;
   int _maxDays;
   Set<Query> _querySpace;

   public WEKACPCToBidPerQuery(Set<Query> querySpace, int idx, double weight, int maxDays) {
      _querySpace = querySpace;
      _idx = idx;
      _weight = weight;
      _maxDays = maxDays;
      _data = new HashMap<Query, Instances>();
      _predictor = new HashMap<Query, Classifier>();
      Attribute bidAttribute = new Attribute("bid");
      Attribute cpcAttribute = new Attribute("cpc");
      _allAttributes = new FastVector(2);
      _allAttributes.addElement(bidAttribute);
      _allAttributes.addElement(cpcAttribute);
      Random random = new Random();
      for (Query query : _querySpace) {
         Instances data = new Instances("data" + random.nextLong(), _allAttributes, 100);
         data.setClassIndex(0);

         Instance query1 = new Instance(3);
         query1.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query1.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query1);

         Instance query2 = new Instance(3);
         query2.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query2.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query2);

         Instance query3 = new Instance(3);
         query3.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query3.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query3);

         Instance query4 = new Instance(3);
         query4.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query4.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query4);

         Instance query5 = new Instance(3);
         query5.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query5.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query5);

         Instance query6 = new Instance(3);
         query6.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query6.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query6);

         Instance query7 = new Instance(3);
         query7.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query7.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query7);

         Instance query8 = new Instance(3);
         query8.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query8.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query8);

         Instance query9 = new Instance(3);
         query9.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query9.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query9);

         Instance query10 = new Instance(3);
         query10.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query10.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query10);

         Instance query11 = new Instance(3);
         query11.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query11.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query11);

         Instance query12 = new Instance(3);
         query12.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query12.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query12);

         Instance query13 = new Instance(3);
         query13.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query13.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query13);

         Instance query14 = new Instance(3);
         query14.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query14.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query14);

         Instance query15 = new Instance(3);
         query15.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query15.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query15);

         Instance query16 = new Instance(3);
         query16.setValue((Attribute) _allAttributes.elementAt(0), 0.0);
         query16.setValue((Attribute) _allAttributes.elementAt(1), 0.0);
         data.add(query16);

         _data.put(query, data);

         Classifier predictor = getClassifier(idx);

         try {
            predictor.buildClassifier(data);
         } catch (Exception e) {
            //			e.printStackTrace();
         }

         _predictor.put(query, predictor);
      }
   }

   @Override
   public double getPrediction(Query query, double cpc) {
      if (cpc == 0) {
         return 0;
      }
      Instances data = _data.get(query);
      Instance pred = new Instance(2);
      pred.setValue((Attribute) _allAttributes.elementAt(1), cpc);
      pred.setDataset(data);
      double prediction = cpc;
      Classifier predictor = _predictor.get(query);
      try {
         prediction = predictor.classifyInstance(pred);
      } catch (Exception e) {
         //			e.printStackTrace();
      }

      /*
         * Our CPC can never be higher than our bid
         */
      if (cpc > prediction) {
         return cpc;
      }

      if (prediction < 0.0) {
         return 0.0;
      }

      return prediction;
   }

   @Override
   public String toString() {
      switch (_idx) {
         case 1:
            return "WEKACPCToBidPerQuery(LinearRegression), weight: " + _weight + ", maxDays: " + _maxDays + ")";
         case 2:
            return "WEKACPCToBidPerQuery(IBk):weight: " + _weight + ", maxDays: " + _maxDays + ")";
         case 3:
            return "WEKACPCToBidPerQuery(KStar):weight: " + _weight + ", maxDays: " + _maxDays + ")";
         case 4:
            return "WEKACPCToBidPerQuery(LWL):weight: " + _weight + ", maxDays: " + _maxDays + ")";
         case 5:
            return "WEKACPCToBidPerQuery(AdditiveRegression):weight: " + _weight + ", maxDays: " + _maxDays + ")";
         case 6:
            return "WEKACPCToBidPerQuery(REPTree):weight: " + _weight + ", maxDays: " + _maxDays + ")";
         case 7:
            return "WEKACPCToBidPerQuery(RegressionByDiscretization):weight: " + _weight + ", maxDays: " + _maxDays + ")";
         default:
            return "WEKACPCToBidPerQuery(LinearRegression):weight: " + _weight + ", maxDays: " + _maxDays + ")";
      }
   }

   @Override
   public boolean updateModel(QueryReport queryReport,
                              SalesReport salesReport, BidBundle bidBundle) {

      for (Query query : _querySpace) {

         Instances data = _data.get(query);

         while (data.numInstances() > _maxDays) {
            data.delete(0);
         }

         if (_weight > 0.0 && _weight != 0) {
            /*
                 * Reweight old data
                 */
            int numDays = (int) (data.numInstances() / 16.0);
            for (int i = 0; i < data.numInstances(); i++) {
               int idx = (int) (i / 16.0);
               data.instance(i).setWeight(Math.pow(_weight, numDays - idx));
            }
         }


         Instance newInstance = new Instance(2);
         double bid = bidBundle.getBid(query);
         double cpc = queryReport.getCPC(query);
         if (Double.isNaN(cpc)) {
            cpc = 0.0;
         }
         newInstance.setValue((Attribute) _allAttributes.elementAt(0), bid);
         newInstance.setValue((Attribute) _allAttributes.elementAt(1), cpc);
         data.add(newInstance);

         Classifier predictor = _predictor.get(query);

         try {
            predictor.buildClassifier(data);
         } catch (Exception e) {
            //			e.printStackTrace();
         }

         _predictor.put(query, predictor);
         _data.put(query, data);
      }

      return true;
   }

   public Classifier getClassifier(int idx) {
      Classifier classifier;
      switch (idx) {
         case 1:
            classifier = (Classifier) new LinearRegression();
            break;
         case 2:
            classifier = (Classifier) new IBk();
            break;
         case 3:
            classifier = (Classifier) new KStar();
            break;
         case 4:
            classifier = (Classifier) new LWL();
            break;
         case 5:
            classifier = (Classifier) new AdditiveRegression();
            break;
//		case 6:  classifier = (Classifier)new REPTree(); break;
         case 7:
            classifier = (Classifier) new RegressionByDiscretization();
            break;
         default:
            classifier = (Classifier) new LinearRegression();
            break;
      }
      return classifier;
   }

   @Override
   public AbstractModel getCopy() {
      return new WEKACPCToBidPerQuery(_querySpace, _idx, _weight, _maxDays);
   }
}
