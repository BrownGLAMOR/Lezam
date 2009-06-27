package props;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Misc
{
	public static boolean verbose = true;
	public static int UNDEFINED_INT = -1111;
	public static String UNDEFINED_STRING = "";
	public static double DOUBLES_IMPRECISION = .000001;
	public static int INFINITY = 1000000;

	public static void error(String s) {
		System.out.println("Error: *** " + s);
	}
	
	public static void error(boolean test, String s) {
		if(test) {
			System.out.println("Error: *** " + s);
		}
	}

	public static void warn(String s) {
		System.out.println("Warn: ### " + s);
	}

	public static void warn(Object s) {
		System.out.println("Warn: ### " + s);
	}

	public static void print(Object s) {
		if (verbose) {
			System.out.print(s);
		}
	}

	public static void print(Object s, boolean print) {
		if (verbose && print) {
			System.out.print(s);
		}
	}

	public static void println(Object s) {
		if (verbose) {
			System.out.println(s);
		}
	}

	public static void println() {
		if (verbose) {
			System.out.println();
		}
	}

	public static void println(Object s, boolean print) {
		if (print && verbose) {
			System.out.println(s);
		}
	}

	public static void println(Object s, PrintStream ps, boolean print) {
		if (print && verbose) {
			ps.println(s);
		}
	}

	public static void myassert(boolean test) {
		myassert(test, "");
	}

	public static void myassert(boolean test, String s) {
		if(!test) {
			Misc.error("Misc.myassert failed: " + s);
			try {
				throw new Error();
			}
			catch(Error e){
				e.printStackTrace(System.err);
				e.printStackTrace(System.out);
				Misc.println("assert. exiting");
				System.exit(0);
			}
		}
	}


	/*

	public static void printGameStatus(DataBank dataBank) {
		System.out.println();
		System.out.println("------ game status -------");
		System.out.println("product inventory");
		int[] prodInv = SimpleInventory.getTomorrowsProductInventory();
		for (int i=0; i<prodInv.length; i++) {
			System.out.println("sku " + i + " = " + prodInv[i]);
		}
		System.out.println();

		CustomerOrderCollection orders = dataBank.getActiveCustomerOrders();
		System.out.println(orders.size() + " orders");
		Iterator i_orders = orders.iterator();
		while(i_orders.hasNext()){
			CustomerOrder order = (CustomerOrder)i_orders.next();
			System.out.println(order);
		}
		System.out.println();

		SupplierOfferCollection soffers = dataBank.getSupplierOffers();
		if (soffers.size() > 0)  {
			System.out.println(soffers.size() + " supplier offers");
			Iterator i_soffers = soffers.iterator();
			while(i_soffers.hasNext()) {
				System.out.println((SupplierOffer)i_soffers.next());
			}
		}

		System.out.println("------ end game status -------");
	}
	*/

	public static void printSeparator(int currentIndex, int totalCount, PrintStream ps) {
		printSeparator(currentIndex, totalCount, ",\t", "\n", ps);
		/*
		if (currentIndex < totalCount-1) {
			ps.print(",\t");
		}else if(currentIndex == totalCount-1){
			ps.println();
		}else{
			myassert(false);
		}
		*/
	}

	public static void printSeparator(int currentIndex, int totalCount, String intermidiateSeparator, String lastSeparator) {
		printSeparator(currentIndex, totalCount, intermidiateSeparator, lastSeparator, System.out);
	}

	public static String printsSeparator(int currentIndex, int totalCount, String intermidiateSeparator, String lastSeparator) {
		return printsSeparator(currentIndex, totalCount, intermidiateSeparator, lastSeparator, System.out);
	}

	public static String printsSeparator(int currentIndex, int totalCount, String intermidiateSeparator, String lastSeparator, PrintStream ps) {
		String s="";
		if (currentIndex < totalCount-1) {
			s = intermidiateSeparator;
		}else if(currentIndex == totalCount-1){
			s = lastSeparator;
		}else{
			myassert(false);
		}
		return s;
	}

	public static void printSeparator(int currentIndex, int totalCount, String intermidiateSeparator, String lastSeparator, PrintStream ps) {
		ps.print(printsSeparator(currentIndex, totalCount, intermidiateSeparator, lastSeparator, ps));
	}

	public static void printSpaces(int numSpaces) {
		for (int i=0; i<numSpaces; i++) {
			Misc.print(" ");
		}
	}

	public static void printPadded(Object o, int len) {
		Misc.print(o);
		for(int i=0; i<len-o.toString().length(); i++) {
			Misc.print(" ");
		}
	}

    public static long getTime() {
    	return System.currentTimeMillis()/1000;
    }

    public static boolean floatsEqual(float a, float b) {
    	if(Math.abs(a-b) < .001) {
    		return true;
    	}
    	return false;
    }

    public static boolean doublesEqual(double a, double b) {
    	if(Math.abs(a-b) < DOUBLES_IMPRECISION) {
    		return true;
    	}
    	return false;
    }

    public static boolean doublesLt(double a, double b) {
    	if(a < b + DOUBLES_IMPRECISION) {
    		return true;
    	}
    	return false;
    }


    public static boolean compareArrays(Object a1, Object a2) {
    	//Misc.println("comparing " + a1 + " = " + a2);
	    int len = Array.getLength(a1);
	    int dim = getDim(a1);
	    int len2 = Array.getLength(a2);
	    int dim2 = getDim(a2);

	    Misc.myassert(len == len2);
	    Misc.myassert(dim == dim2);

	    for(int i=0; i<len; i++) {
	    	Object e1 = Array.get(a1, i);
	    	Object e2 = Array.get(a2, i);
	    	//Misc.println("element " + i + "   " + e1 + " = " + e2 + " class " + e1.getClass());
	    	if(1 == dim) {
	    		if(!e1.equals(e2)) {
	    			return false;
	    		}
	    	}else{
	    		return compareArrays(e1, e2);
	    	}
		}
		return true;
    }

    // If `array' is an array object returns its dimensions; otherwise returns 0
    public static int getDim(Object array) {
        int dim = 0;
        Class cls = array.getClass();
        while (cls.isArray()) {
            dim++;
            cls = cls.getComponentType();
        }
        return dim;
    }


	public static String printsArray(Object array) {
		return printsArray(array, " ");
	}

	/*
	 * returns a copy of the array.
	 * array must have at least 1 element
	 */
	public static Object getArrayCopy(Object array) {
		int len = Array.getLength(array);
		Misc.myassert(len > 0);
		Object copy = Array.newInstance(Array.get(array,0).getClass(), len);
	    for(int i=0; i<len; i++) {
	    	Object element = Array.get(array, i);
	    	Array.set(copy, i, element);
	    }

		return copy;
	}

	public static void testgetArrayCopy() {
		int[] a = {4,7,5};
		Integer[] b = (Integer[]) getArrayCopy(a);
		Misc.printArray(b);
		Misc.myassert(Misc.compareArrays(a, b));
	}

	public static String printsArray(Object array, String separator) {
		String s = "";
	    int len = Array.getLength(array);
	    int dim = getDim(array);

	    for(int i=0; i<len; i++) {
	    	Object element = Array.get(array, i);
	    	if(1 == dim) {
	    		s += element;
	    		if(i != len-1) {
	    			s += separator;
	    		}
	    	}else{
	    		s += printsArray(element) + "\n";
	    	}
		}
	    if(dim > 1) {
	    	s += "\n";
	    }

		return s;
	}

	public static void printArray(Object array, String separator) {
		Misc.println(printsArray(array, separator));
	}

	public static void printArray(Object array, String separator, boolean print) {
		if(print) {
			Misc.println(printsArray(array, separator));
		}
	}

	public static void printArray(Object array) {
		Misc.printArray(array, " ");
	}

	public static void printArray(String m, Object array) {
		Misc.print(m + ": ");
		Misc.printArray(array);
	}

	public static void printArray(String m, Object array, String separator) {
		Misc.print(m + ": ");
		Misc.printArray(array, separator);
	}


	public static void printList(List list) {
		Misc.println(printsList(list));
	}

	public static void printList(List list, String separator, boolean print) {
		if(print) {
			Misc.println(printsList(list, separator));
		}
	}

	public static void printList(List list, String separator) {
		Misc.println(printsList(list, separator));
	}

	public static void printList(String s, List list) {
		Misc.println(s + printsList(list));
	}
	
	public static String printsList(List list) {
		return printsList(list, "");
	}

	public static String printsList(List list, String separator) {
		String s = "";
		Iterator i = list.iterator();
		while(i.hasNext()) {
			s += i.next() + separator;
		}
		return s;
	}

	public static Comparable getMin(Comparable[] array) {
		Comparable min = array[0];
		for (int i=1; i<array.length; i++) {
			if (array[i].compareTo(min) < 0) {
				min = array[i];
			}
		}
		return min;
	}

	public static Comparable getMax(Comparable[] array) {
		Comparable max = array[0];
		for (int i=1; i<array.length; i++) {
			if (array[i].compareTo(max) > 0) {
				max = array[i];
			}
		}
		return max;
	}

	/*
	 *comparator must sort items in increasing order
	 */
	public static Object getMin(Object[] array, Comparator c) {
		Object min = array[0];
		for (int i=1; i<array.length; i++) {
			if (c.compare(min, array[i]) > 0) {
				min = array[i];
			}
		}
		return min;
	}

	/*
	 *comparator must sort items in increasing order
	 */
	public static Object getMax(Object[] array, Comparator c) {
		Object max = array[0];
		for (int i=1; i<array.length; i++) {
			if (c.compare(max, array[i]) < 0) {
				max = array[i];
			}
		}
		return max;
	}


	/*
	public static int getMin(int[] array) {
		int min = array[0];
		for (int i=1; i<array.length; i++) {
			if (array[i] < min) {
				min = array[i];
			}
		}
		return min;
	}
	*/


	public static boolean isBitOn(int bitIndex, int number) {
		int mask = (int)Math.pow(2, bitIndex);
		return ((mask & number) == mask);
	}

	//when exact is true, return minus one when the element is not in the array
	public static int binarySearch(double[] sorted, double key, boolean exact) {
		myassert(sorted.length > 0);
		int first = 0;
		int last = sorted.length-1;
	    while (first < last) {
	        int mid = (first + last) / 2;  // Compute mid point.
	        if (key < sorted[mid]) {
	            last = mid;     // repeat search in bottom half.
	        } else if (key > sorted[mid]) {
	            first = mid + 1;  // Repeat search in top half.
	        } else {
	            return mid;     // Found it. return position
	        }
	    }

	    if(key == sorted[first]) {
	    	return first;
	    }else{
		    if(exact) {
		    	return -1;
		    }else{
		    	return first;//this is where the element should be inserted
		    }
	    }
	}

	public static int binarySearchReverse(double[] sortedInReverse, double key, ArrayList results) {
		myassert(sortedInReverse.length > 0);
		Misc.printArray("efficiencies ",  sortedInReverse);
		Misc.println("key " +  key);
		int first = 0;
		int last = sortedInReverse.length-1;
	    while (first < last) {
	        int mid = (first + last) / 2;  // Compute mid point.
	        if (key > sortedInReverse[mid]) {
	            last = mid;     // repeat search in bottom half.
	        } else if (key < sortedInReverse[mid]) {
	            first = mid + 1;  // Repeat search in top half.
	        } else {
	        	results.add(true);
	        	return mid;
	        }
	    }

	    Misc.myassert(first == last);

	    if(key == sortedInReverse[first]) {
		    results.add(true);
	    }else{
		    results.add(false);
	    }

    	return first;//this is where the element should be inserted
	}

	/*
	when exact is true, return minus one when the element is not in the array
	the same as binarySearch, but the array is sorted in reverse order

	if exact value was found: true is inserted in results

	ex:
	10
	7
	6
	insert 11 - return 0
	insert 8 - return 1
	insert 5 - return 3

	*/
	public static int insertAtReverse(double[] sortedInReverse, double key, ArrayList results) {
		int index = binarySearchReverse(sortedInReverse, key, results);
		if((Boolean)results.get(0)) {//exact match found
			return index;
		}

	    //position where the element should be inserted
	    if(key > sortedInReverse[index]) {
	    	return index;//insert at the position of sortedInReverse[first], so that sortedInReverse[first] is after key
	    }else if(key < sortedInReverse[index]) {
	    	return index + 1; //insert after sortedInReverse[first]
	    }else{
	    	Misc.myassert(false);
	    	return -1;
	    }
	}


	public static double round(double d) {
		return (double)Math.round(d*100)/100;
	}

	public static void printTime(long start) {
		Misc.println("took " + (System.currentTimeMillis()-start)/1000 + " sec");
	}

	public static void printTime(long start, String m) {
		Misc.println(m + " took " + (System.currentTimeMillis()-start)/1000 + " sec");
	}

	/*
	 * use in the catch part of the try/catch block
	 */
	public static void exception(Exception e) {
		Misc.error("exception " + e);
		e.printStackTrace(System.err);
		e.printStackTrace(System.out);
		System.exit(0);
	}


	public static double[] DoubleArrayTodouble(Double[] array) {
		double[] dblArray = new double[array.length];
		for(int i=0; i<array.length; i++) {
			dblArray[i] = array[i];
		}
		return dblArray;
	}

	public static int[] integerArrayToint(Integer[] array) {
		int[] intArray = new int[array.length];
		for(int i=0; i<array.length; i++) {
			intArray[i] = array[i];
		}
		return intArray;
	}

	public static void download(String address, String localFileName) {
		Misc.println("saving " + address + " in " + localFileName);
		OutputStream out = null;
		URLConnection conn = null;
		InputStream  in = null;
		try {
			URL url = new URL(address);
			out = new BufferedOutputStream(
				new FileOutputStream(localFileName));
			conn = url.openConnection();
			in = conn.getInputStream();
			byte[] buffer = new byte[1024];
			int numRead;
			long numWritten = 0;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
				numWritten += numRead;
			}
			Misc.println(localFileName + "\t" + numWritten);
		} catch (Exception exception) {
			Misc.exception(exception);
			//exception.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
			}
		}
	}

	public static void createIfNotThere(String dir) {
        if (!(new File(dir)).exists()) {
        	Misc.println("dir does not exist. creating " + dir);
        	//mkdirs: Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.
            boolean success = (new File(dir)).mkdirs();
            if(!success) {
            	Misc.myassert(false, "could not create dir");
            }
        }
	}

	public static String dateForFileName() {
		String pattern = "yyyy-MM-dd-HH-mm-ss";
	    SimpleDateFormat format = new SimpleDateFormat(pattern);
	    return format.format(new Date());
	}
	
	
	/*
	 * method for serialiation
	 * save an object
	 */
    public static void write(String fileName, Serializable obj) {
        try{
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(obj);
            out.flush(); // Always flush the output. 
            out.close(); // And close the stream.
          } catch (java.io.IOException e){
        	  Misc.exception(e);
          }
    }
    
    
	/*
	 * method for serialiation
	 * read a saved object
	 */
    public static Object read(String fileName) {
        try{
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fis);
            Object obj = in.readObject();
            in.close();
            return obj;
          } catch (Exception e){
        	  Misc.exception(e);
        	  return null;
          }
    }
    
    public static PrintStream nullStream() {
    	return new PrintStream(new OutputStream() {public void write(int b) {}});
    }
    
    public static double[] reverse(double[] a) {
    	double[] b = new double[a.length];
    	for(int i=0; i<a.length; i++) {
    		b[b.length-1-i] = a[i];
    	}    	
    	return b;
    }
    
    public static boolean containsDuplicates(double[] a) {
    	HashSet s = new HashSet();
    	for(int i=0; i<a.length; i++) {
    		s.add(a[i]);
    	}
    	//Misc.println("size " + s.size());
    	Misc.myassert(s.size() <= a.length);
    	
    	return s.size() < a.length;
    }

	public static void main (String[] args) {
		double[] hi = {1.41, 1.5, 1.4};
		Misc.println("duplicates = " + containsDuplicates(hi));
		
		System.exit(0);
		testgetArrayCopy();


		HashSet<Integer> s = new HashSet<Integer>();
		s.add(new Integer(11));
		s.add(new Integer(31));
		s.add(new Integer(5));
		Misc.println(getMax(s.toArray(new Integer[0])));



		String hi1 = "hi";
		String hi2 = "hi";
		Misc.println("hi1 " + hi1.hashCode() + " hi2 " + hi2.hashCode());



		int[] a1 = {1,2,3};
		int[] a2 = {1,3,3};
		Misc.println("f. arrays equal " + compareArrays(a1, a2));

		a2[1] = 2;
		Misc.println("t. arrays equal " + compareArrays(a1, a2));

		double b1[][] = new double[2][3];
		b1[1][2] = 3;

		double b2[][] = new double[2][3];
		b1[1][2] = 3;

		Misc.println("t. arrays equal " + compareArrays(b1, b2));

		b1[0][2] = 3;
		Misc.println("f. arrays equal " + compareArrays(b1, b2));

	}
}

