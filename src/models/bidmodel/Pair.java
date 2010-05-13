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

  public String toString()
  { 
    return "(" + first.toString() + ", " + second.toString() + ")"; 
  }
  
  public int compareTo(Pair<Comparable, S> p) {
	  return p.getFirst().compareTo(getFirst());
  }

  public int compareTo(Object o) {
	  return 0;
  }
  
}
