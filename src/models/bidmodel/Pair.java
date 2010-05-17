package models.bidmodel;

public class Pair<T, S> implements Comparable 
{
  private T first;
  private S second;
  
  public Pair(T f, S s)
  { 
    first = f;
    second = s;   
  }

  public T getFirst()
  {
    return first;
  }

  public S getSecond() 
  {
    return second;
  }
  
  public void setFirst(T t) {
	  first = t;
  }
  

  public void setSecond(S t) {
	  second = t;
  }

  public String toString()
  { 
    return "(" + first.toString() + ", " + second.toString() + ")"; 
  }
  

  public int compareTo(Object o) {
	  if(o instanceof Pair && ((Pair) o).getFirst() instanceof Double) {
		  return ((Comparable) getFirst()).compareTo(((Comparable) ((Pair) o).getFirst()));
	  } else {
		  System.out.println("foo");
		  return 0;
	  }
		
  }
  
}
