package newmodels.usermodel;

/**
 * @author sodomka
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Random;
import java.util.StringTokenizer;

import newmodels.AbstractModel;

import usermodel.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class HistoricalDailyAverageUserModel extends AbstractUserModel {



	//#Users in each state, for each product/day
	private double[][] F0users; //[product][day]
	private double[][] F2users;
	private double[][] F1users;
	private double[][] ISusers;
	private double[][] Tusers;
	private double[][] NSusers;

	public HistoricalDailyAverageUserModel() {

		int numProds = 9; //FIXME: DON'T HARDCODE!!! Is this data available globally?
		int numDays = 60; //FIXME: DON'T HARDCODE!!! Is this data available globally?
		String filename = "src/newmodels/usermodel/UserDetailData-Finals.csv";;

		String[][] rawVals = readCSVData(filename);

		F0users = new double[numProds][numDays];
		F1users = new double[numProds][numDays];
		F2users = new double[numProds][numDays];
		ISusers = new double[numProds][numDays];
		Tusers = new double[numProds][numDays];
		NSusers = new double[numProds][numDays];

		fillUserStatesFromRawData(rawVals);
	}




	/**
	 * Take in matrix containing raw data,
	 * Fill user models w/ correct average number of impressions.
	 * @param rawVals
	 */
	private void fillUserStatesFromRawData(String[][] rawVals) {
		int numRows = rawVals.length;

		int DAY_IDX = 0;
		int STATE_IDX = 1;
		int MAN_IDX = 2;
		int COMP_IDX = 3;
		int AVG_IMPS_IDX = 4;


		//Don't look at first row, since it contains header information
		for (int i=1; i<numRows; i++) {

			//Need to get: day(j=0), userstate(j=1), prod(j=2-3), val(j=4) 
			int day = Integer.parseInt(rawVals[i][DAY_IDX].trim());
			UserState userState = getUserState(rawVals[i][STATE_IDX]);
			int prodIdx = getProdIdx(rawVals[i][MAN_IDX], rawVals[i][COMP_IDX]);//userState
			double avgImpressions = Double.parseDouble(rawVals[i][AVG_IMPS_IDX].trim());


			if(userState == UserState.F0) {
				F0users[prodIdx][day] = avgImpressions;
			}
			else if(userState == UserState.F1) {
				F1users[prodIdx][day] = avgImpressions;
			}
			else if(userState == UserState.F2) {
				F2users[prodIdx][day] = avgImpressions;
			}
			else if(userState == UserState.IS) {
				ISusers[prodIdx][day] = avgImpressions;
			}
			else if(userState == UserState.T) {
				Tusers[prodIdx][day] = avgImpressions;
			}
			else if(userState == UserState.NS) {
				NSusers[prodIdx][day] = avgImpressions;
			}
			else {
				System.out.println("Error! User state " + userState + " doesn't exist.");
			}
		}
	}



	/**
	 * This is a hack to get a product index from a manufacturer and component.
	 * Do we not have a way to refer to products by an index?
	 * Can we even get a list of possible products?
	 * @param manufacturerString
	 * @param componentString
	 * @return
	 */
	private int getProdIdx(String manufacturerString, String componentString) {

		int manIdx = -1;
		int compIdx = -1;

		if (manufacturerString.equalsIgnoreCase("lioneer")) manIdx=0;
		if (manufacturerString.equalsIgnoreCase("pg")) manIdx=1;
		if (manufacturerString.equalsIgnoreCase("flat")) manIdx=2;

		if (componentString.equalsIgnoreCase("tv")) compIdx=0;
		if (componentString.equalsIgnoreCase("dvd")) compIdx=1;
		if (componentString.equalsIgnoreCase("audio")) compIdx=2;

		if (manIdx==-1 || compIdx==-1) System.out.println("Error! No match for " + manufacturerString + ", " + componentString);
		int prodIdx =  3 * manIdx + compIdx;
		return prodIdx;
	}




	private UserState getUserState(String string) {
		if (string.equals("F0")) return UserState.F0;
		if (string.equals("F1")) return UserState.F1;
		if (string.equals("F2")) return UserState.F2;
		if (string.equals("Informational Search")) return UserState.IS;
		if (string.equals("Non-Searching")) return UserState.NS;
		if (string.equals("Transacted")) return UserState.T;

		System.out.println("Bad string! " + string + " is not a valid user state.");
		return null;
	}

	@Override
	public int getPrediction(Product product, UserState userState, int day) {
		String manString = product.getManufacturer();
		String compString = product.getComponent();
		int prodIdx=getProdIdx(manString, compString);


		if(userState == UserState.F0) {
			return (int) F0users[prodIdx][day];
		}
		else if(userState == UserState.F1) {
			return (int) F1users[prodIdx][day];
		}
		else if(userState == UserState.F2) {
			return (int) F2users[prodIdx][day];
		}
		else if(userState == UserState.IS) {
			return (int) ISusers[prodIdx][day];
		}
		else if(userState == UserState.T) {
			return (int) Tusers[prodIdx][day];
		}
		else if(userState == UserState.NS) {
			return (int) NSusers[prodIdx][day];
		}
		else {
			throw new RuntimeException("");
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new HistoricalDailyAverageUserModel();
	}





	/**
	 * Reads data in the CSV file, output an array containing this data
	 * Example format:
	 * "Day","Description","ManuPreference","CompPreference","AvgUserCount","UserCountStdDev"
	 * "0","F0","flat","audio","0.0000","0.0000"
	 * "0","F0","flat","dvd","0.0000","0.0000"
	 */
	public static String[][] readCSVData(String filename) {
		String [][] values = null;

		try {
			File file = new File(filename);


			//---
			//Get numRows, and numCols for the first row (assume it's the same for each row!)
			//---
			BufferedReader bufRdr = new BufferedReader(new FileReader(file));
			int numRows = 0;
			int numCols = 0;
			String line = null;
			while((line = bufRdr.readLine()) != null) {	
				if (numRows==0) {
					StringTokenizer st = new StringTokenizer(line,",");
					while (st.hasMoreTokens()) {
						st.nextToken();
						numCols++;
					}
				}
				numRows++;
			}
			bufRdr.close();
			System.out.println("numRows: " + numRows + "\tnumCols: " + numCols);





			//---
			//Create a 2d array, and read values into this array.
			values = new String [numRows][numCols];





			bufRdr = new BufferedReader(new FileReader(file));
			int row=0;
			int col=0;
			while((line = bufRdr.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line,",");
				while (st.hasMoreTokens())
				{
					//get next token and store it in the array
					values[row][col] = st.nextToken();

					//Remove quotes from this string.
					values[row][col] = values[row][col].replaceAll("[\'\"]", "");  
					col++;
				}
				row++;
				col = 0;
			}
			bufRdr.close();


		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return values;
	}





	public static void main(String[] args) {
		//String filename = "/home/sodomka/workspace/TAC_AA/src/newmodels/usermodel/UserDetailData-Finals.csv";
		String filename = "src/newmodels/usermodel/UserDetailData-Finals.csv";
		String[][] vals = HistoricalDailyAverageUserModel.readCSVData(filename);

		System.out.println("Finished reading.");
		System.out.println("vals size: " + vals.length + "\t" + vals[0].length);
		for (int i=0; i<vals.length; i++) {
			for (int j=0; j<vals[0].length; j++) {
				System.out.print(vals[i][j] + "\t");
			}
			System.out.println();
		}


		HistoricalDailyAverageUserModel model = new HistoricalDailyAverageUserModel();

		Product p = new Product("lioneer", "tv");
		UserState u = UserState.T;
		int d = 59;

		System.out.println("prediction: " + model.getPrediction(p,u,d));
	}


}
