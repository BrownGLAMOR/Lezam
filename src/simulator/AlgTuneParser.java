package simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class AlgTuneParser {

	public static void main(String[] args) throws IOException {
		String filename = args[0];
		generatePotentialParamsList(filename);
	}

	private static void generatePotentialParamsList(String filename) throws IOException {
		BufferedReader input =  new BufferedReader(new FileReader(filename));

		/*
		 * Skip the file header
		 */
		for(int i = 0; i < 18; i++) {
			input.readLine();
		}
		
		String line;
		while ((line = input.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line,"]");
			st.nextToken();
			st.nextToken();
			String token = st.nextToken();
			if(" Generation".equals(token.substring(0, 11))) {
				line = input.readLine();
				line = input.readLine();
				line = input.readLine(); // skip three lines
				StringTokenizer st2 = new StringTokenizer(line,"]");
				st2.nextToken();
				st2.nextToken();
				String token2 = st2.nextToken().substring(3);
				System.out.println(token2);
				input.readLine(); //skip next line
			}
		}
	}
	
}
