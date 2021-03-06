package numatrix;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

public class NumatrixNumberGeneratorFactoryTest {

  final int jvmId = 2;
  final int maxGenetarotCount = 64;
  final int maxJvmCount = 4;

  abstract class AbstractNumatrixNumberGeneratorFactory extends NumatrixNumberGeneratorFactory {
    @Override
    protected int getJvmId() {
      return jvmId;
    }

    @Override
    protected int getMaxGeneratorCount() {
      return maxGenetarotCount;
    }

    @Override
    protected int getMaxJvmCount() {
      return maxJvmCount;
    }
  }

  final NumatrixNumberGeneratorFactory factory1 = new AbstractNumatrixNumberGeneratorFactory() {
    @Override
    protected int getTypeId() {
      return 1;
    }
  };

  final NumatrixNumberGeneratorFactory factory2 = new AbstractNumatrixNumberGeneratorFactory() {
    @Override
    protected int getTypeId() {
      return 2;
    }
  };

  NumatrixNumberGenerator type1Generator1;
  NumatrixNumberGenerator type1Generator2;
  NumatrixNumberGenerator type2Generator1;

  @Test
  public void 複数のファクトリから個別にインスタンスをGETできる() throws Exception {
    new MockUp<NumatrixNumberGenerator>() {
      @Mock
      void $init(Invocation inv, int paramGeneratorId, int paramGeneratorIdBitCount) {
        assertThat(paramGeneratorIdBitCount, equalTo(8));
        switch (inv.getInvocationCount()) {
          case 1:
            assertThat(paramGeneratorId, equalTo(128));
            type1Generator1 = inv.getInvokedInstance();
            break;
          case 2:
            assertThat(paramGeneratorId, equalTo(128));
            type2Generator1 = inv.getInvokedInstance();
            break;
          case 3:
            assertThat(paramGeneratorId, equalTo(129));
            type1Generator2 = inv.getInvokedInstance();
            break;
          default:
            fail();
        }
      }
    };

    NumatrixNumberGenerator generator;
    generator = factory1.getGenerator();
    assertThat(generator, is(type1Generator1));
    generator = factory2.getGenerator();
    assertThat(generator, is(type2Generator1));
    generator = factory1.getGenerator();
    assertThat(generator, is(type1Generator1));
    generator = factory2.getGenerator();
    assertThat(generator, is(type2Generator1));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Callable<NumatrixNumberGenerator> task = new Callable<NumatrixNumberGenerator>() {
      @Override
      public NumatrixNumberGenerator call() throws Exception {
        return factory1.getGenerator();
      }
    };
    Future<NumatrixNumberGenerator> future = executor.submit(task);
    assertThat(future.get(), is(type1Generator2));

  }

  @Test
  public void プールに空きがない場合にリフレッシュしてGETする() throws NumatrixNumberGenerateException {
    final NumatrixNumberGenerator generator = new NumatrixNumberGenerator(1, 1);
    new MockUp<RecyclePool<NumatrixNumberGenerator>>() {
      @Mock
      int size() {
        return maxGenetarotCount;
      }

      boolean refreshed = false;

      @Mock
      NumatrixNumberGenerator get() {
        return refreshed ? generator : null;
      }

      @Mock
      void refresh() {
        refreshed = true;
      }
    };
    assertThat(factory1.getGenerator(), is(generator));
  }

  @Test
  public void プールに空きがない場合の例外発生() {
    new MockUp<RecyclePool<NumatrixNumberGenerator>>() {
      @Mock
      int size() {
        return maxGenetarotCount;
      }

      @Mock
      NumatrixNumberGenerator get() {
        return null;
      }

    };
    try {
      factory1.getGenerator();
      fail();
    } catch (NumatrixNumberGenerateException e) {
      assertThat(e.getMessage(), equalTo("generator instance count is over maximum. "
          + "please reduce threads that using this class."));
    }
  }

  @Test
  public void jvmIDが大きすぎる場合() throws NumatrixNumberGenerateException {
    new AbstractNumatrixNumberGeneratorFactory() {
      @Override
      protected int getTypeId() {
        return 3;
      }

      @Override
      protected int getJvmId() {
        return 3;
      }
    }.getGenerator();
    try {
      new AbstractNumatrixNumberGeneratorFactory() {
        @Override
        protected int getTypeId() {
          return 4;
        }

        @Override
        protected int getJvmId() {
          return 4;
        }
      }.getGenerator();
      fail();
    } catch (NumatrixNumberGenerateException e) {
      assertThat(e.getMessage(),
          equalTo("JVM ID is invalid. " + "please implement to lower getJmvId returns."));
    }
  }

}
