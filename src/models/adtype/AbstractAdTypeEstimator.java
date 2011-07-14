package models.adtype;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

import java.util.HashMap;

public abstract class AbstractAdTypeEstimator extends AbstractModel {

   public abstract Ad getAdTypeEstimate(Query q, String advertiser);

   public abstract void updateModel(QueryReport queryReport);

}
