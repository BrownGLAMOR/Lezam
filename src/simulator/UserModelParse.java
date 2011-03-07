package simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class UserModelParse {

   public UserModelParse() throws IOException {
      String baseSol = "jbergFilter(";
      ArrayList<String> paramStrings = new ArrayList<String>();
      BufferedReader input = new BufferedReader(new FileReader("/Users/jordanberg/Desktop/ALLUSERPRED.o"));

      HashMap<ConvDistPair, Double> convDistPairs = new HashMap<ConvDistPair, Double>();

      String line;
      while ((line = input.readLine()) != null) {
         if (line.length() > baseSol.length()) {
            if (line.subSequence(0, baseSol.length()).equals(baseSol)) {
               StringTokenizer st = new StringTokenizer(line, ",()");
               st.nextToken();
               double baseConv = Double.parseDouble(st.nextToken());
               double convVar = Double.parseDouble(st.nextToken());
               double RMSE = Double.parseDouble(st.nextToken());
               double RMSEFuture = Double.parseDouble(st.nextToken());
               convDistPairs.put(new ConvDistPair(baseConv, convVar), RMSE + RMSEFuture);
            }
         }
      }
      convDistPairs = sortHashMap(convDistPairs);
      for (ConvDistPair paramStr : convDistPairs.keySet()) {
         System.out.println(convDistPairs.get(paramStr) + ": " + paramStr);
      }
   }

   public class ConvDistPair {

      double _baseConv, _convVar;

      public ConvDistPair(double baseConv, double convVar) {
         _baseConv = baseConv;
         _convVar = convVar;
      }

      public String toString() {
         return "" + _baseConv + ", " + _convVar;
      }
   }

   private static HashMap<ConvDistPair, Double> sortHashMap(HashMap<ConvDistPair, Double> input) {
      Map<ConvDistPair, Double> tempMap = new HashMap<ConvDistPair, Double>();
      for (ConvDistPair wsState : input.keySet()) {
         tempMap.put(wsState, input.get(wsState));
      }

      List<ConvDistPair> mapKeys = new ArrayList<ConvDistPair>(tempMap.keySet());
      List<Double> mapValues = new ArrayList<Double>(tempMap.values());
      HashMap<ConvDistPair, Double> sortedMap = new LinkedHashMap<ConvDistPair, Double>();
      TreeSet<Double> sortedSet = new TreeSet<Double>(mapValues);
      Object[] sortedArray = sortedSet.toArray();
      int size = sortedArray.length;
      for (int i = 0; i < size; i++) {
         sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[i])),
                       (Double) sortedArray[i]);
      }
      return sortedMap;
   }

   public static void main(String[] args) throws IOException {
      UserModelParse parse = new UserModelParse();
   }
}
