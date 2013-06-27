package simulator.predictions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import models.queryanalyzer.ds.QADataAll;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.util.LoadData;

public class LogReader {

	/*
	 * This class reads from the logs or other files to generate data that can be plotted 
	 */
	public LogReader(){
		
	}
	
	public int[] collectTotalImpressionsInFirstSlot(int game, int auction, String folderName){
		int[] totalImps = new int[57];

		for(int day = 0; day<=56;day++){
			String file = folderName+"/GameLog_game"+game+"_day"+day+"_query"+auction;
			QADataAll data = null;
			
			data = LoadData.LoadIt(file);
			
			//make instance
			QAInstanceAll inst;
			if(data!=null){
				inst = data.buildInstances(data.getOurAgentNum());
				
			//get total impressions and store in array
			totalImps[day] = inst.getTotalImpsFirstSlot();
			}else{
				
				
				//get total impressions and store in array
				totalImps[day] = totalImps[day-1];
			}
		}
		
		return totalImps;
	}
	
	public int[][] getTotalImpsData(int auction, int startGame, int endGame, String folderName){
		int[][] data = new int[(endGame-startGame)+1][57];
		for(int game = startGame; game<=endGame; game++){
			
			data[game-startGame]= collectTotalImpressionsInFirstSlot(game, auction, folderName);
			
		}
		
		return data;
	}
	
	public void writeData(int[][] data, String fileOut){
		
		try {
			FileWriter writer = new FileWriter(new File(fileOut));
			for(int i=0;i<data.length;i++){
				if(i==0){
					for(int j=0;j<data[0].length;j++){
						writer.write(j+",");
					}
					writer.write("\n");
				}
				for(int j=0;j<data[0].length;j++){
					
					writer.write(data[i][j]+",");
				}
				writer.write("\n");
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LogReader reader = new LogReader();
		String fileIn = "./GameLogFiles_server1/files";
		int gameStart = 1435;
		int gameEnd = 1445;
		
		for(int query = 0;query<=14;query++){
	
			if(query!=8){
		int[][] data = reader.getTotalImpsData(query, gameStart, gameEnd, fileIn);
		
		String fileOut = "./GameLogFiles_server1/results/TotalImpsData/TotalImps_gameStart"+gameStart+"_gameEnd"+gameEnd+"_query"+query+".csv";
		
		reader.writeData(data, fileOut);
			}
		}
		
		

	}

}
