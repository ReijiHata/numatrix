package numatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Keep element of each thread, and another thread can reuse element when that thread disappears.
 *
 * @param <E> The element type of this pool
 */
public class RecyclePool<E> {

  private final List<E> elementList = new ArrayList<>();
  private final WeakHashMap<Thread, E> weakElementMap = new WeakHashMap<>();

  /**
   * Appends the specified element to the this pool.
   *
   * @param element element to be appended to this pool
   */
  public void add(E element) {
    synchronized (elementList) {
      elementList.add(element);
      weakElementMap.put(Thread.currentThread(), element);
    }
  }

  /**
   * Returns the number of elements in this pool.
   *
   * @return the number of elements in this pool
   */
  public int size() {
    return elementList.size();
  }

  /**
   * Returns the element keeping by current thread. if current thread does not keep element then,
   * keeping and returning the element kept by disappear thread. or {@code null} if all elements of
   * this pool are kept by other threads.
   *
   * @return the element kept by current thread
   */
  public E get() {
    Thread currentThread = Thread.currentThread();
    E myElement = weakElementMap.get(currentThread);
    if (myElement != null) {
      return myElement;
    }
    synchronized (elementList) {
      if (elementList.size() <= weakElementMap.size()) {
        return null;
      }
      for (E element : elementList) {
        if (!weakElementMap.containsValue(element)) {
          weakElementMap.put(currentThread, element);
          myElement = element;
          break;
        }
      }
      return myElement;
    }
  }

  /**
   * Execute garbage collection for reusing elements kept by the thread to be disappear.
   */
  public void refresh() {
    for (int retry = 0; retry <= 10; retry++) {
      System.gc();
      if (elementList.size() > weakElementMap.size()) {
        return;
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        ;
      }
    }
  }

}
