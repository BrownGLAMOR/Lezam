package agents;

/**
 * ObjectPair stores two associated objects, that can be retrieved separately.
 * It has similar functionality to Pair<Object, Object> class in c++ and
 * javac Pair<Object, Object> class.
 * Code was inspired from an example on the web.
 *
 * @param <T>
 * @param <S>
 */
public class Pair<T, S>
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
}