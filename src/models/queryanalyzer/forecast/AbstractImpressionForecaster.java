package models.queryanalyzer.forecast;

import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.List;
import java.util.Map;

public abstract class AbstractImpressionForecaster extends AbstractModel {

   public abstract boolean updateModel(List<Map<Query, Integer>> allImpressions);

   public abstract double getPrediction(int agent, Query q);

}
