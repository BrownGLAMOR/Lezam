package models.advertiserspecialties;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

import java.util.HashMap;
import java.util.List;

public abstract class AbstractSpecialtyModel extends AbstractModel {

	public abstract String getComponentSpecialty(String advertiser);
	public abstract String getManufacturerSpecialty(String advertiser);

   public abstract void updateModel(QueryReport queryReport);

}
