package numatrix;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import org.junit.runner.RunWith;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class RecyclePoolTest {

  @Test
  public void test() throws Exception {
    final RecyclePool<Object> testTarget = new RecyclePool<>();
    assertThat(testTarget.size(), is(0));
    assertThat(testTarget.get(), nullValue());
    testTarget.refresh();

    Object element1 = new Object();
    testTarget.add(element1);
    assertThat(testTarget.get(), is(element1));
    assertThat(testTarget.size(), is(1));

    final Object element2 = new Object();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Object> future = executor.submit(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        assertThat(testTarget.get(), nullValue());
        testTarget.add(element2);
        return testTarget.get();
      }
    });
    assertThat(future.get(), is(element2));
    assertThat(testTarget.size(), is(2));
    executor = Executors.newSingleThreadExecutor();
    future = executor.submit(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        testTarget.refresh();
        return testTarget.get();
      }
    });
    assertThat(future.get(), is(element2));
    assertThat(testTarget.size(), is(2));
  }

}
