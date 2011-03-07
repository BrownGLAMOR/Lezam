/**
 *
 */
package models.usermodel;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;

/**
 * @author jberg
 */
public abstract class AbstractUserModel extends AbstractModel {

   public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport);

   public abstract int getPrediction(Product product, UserState userState, int day);

}
