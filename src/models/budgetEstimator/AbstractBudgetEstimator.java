package models.budgetEstimator;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;

import java.util.HashMap;

public abstract class AbstractBudgetEstimator extends AbstractModel {

   public abstract double getBudgetEstimate(Query q, String advertiser);

   public abstract void updateModel(QueryReport queryReport,
                                    SalesReport salesReport,
                                    BidBundle bidBundle,
                                    int numberPromotedSlots,
                                    double[] convProbs,
                                    HashMap<Query, Double> contProbs,
                                    HashMap<Query, int[]> order,
                                    HashMap<Query, int[]> impressions,
                                    HashMap<Query, double[]> bids,
                                    HashMap<Product, HashMap<UserState, Integer>> userStates);

}
