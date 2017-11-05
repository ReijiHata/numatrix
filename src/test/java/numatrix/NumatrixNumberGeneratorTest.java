package numatrix;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class NumatrixNumberGeneratorTest {

  @Test
  public void インスタンスIDが大きすぎる場合に例外が発生するテスト() throws NumatrixNumberGenerateException {
    new NumatrixNumberGenerator(1073741823).generate();
    try {
      new NumatrixNumberGenerator(1073741824).generate();
      fail();
    } catch (NumatrixNumberGenerateException e) {
      assertThat(e.getMessage(), equalTo("number stracture size over 64bits."));
    }
  }

  @Test
  public void タイムスタンプが最大値を超えてしまった場合に例外が発生するテスト() throws NumatrixNumberGenerateException {
    NumatrixNumberGenerator testTarget = new NumatrixNumberGenerator(65535);
    final long baseTime = testTarget.getBaseTime().getTime();
    new MockUp<System>() {
      @Mock
      public long currentTimeMillis() {
        return 4294967295000L + baseTime;
      }
    };
    for (int i = 0; i <= 32767; i++) {
      testTarget.generate();
    }
    try {
      new MockUp<System>() {
        @Mock
        public long currentTimeMillis() {
          return 4294967296000L + baseTime;
        }
      };
      testTarget.generate();
      fail();
    } catch (NumatrixNumberGenerateException e) {
      assertThat(e.getMessage(),
          equalTo("timestamp element of the number structure is over maximum."));
    }
  }

  @Test
  public void 連続した番号を使い切ってタイムスタンプが更新されるテスト() throws NumatrixNumberGenerateException {
    int generatorId = 65535;
    NumatrixNumberGenerator testTarget = new NumatrixNumberGenerator(generatorId);

    long timestamp = (System.currentTimeMillis() - testTarget.getBaseTime().getTime()) / 1000L;
    for (int i = 0; i <= 32767; i++) {
      long numatrixNum = testTarget.generate();
      if (i == 0 && testTarget.getCurrentTimestamp() != timestamp) {
        timestamp++;
      }
      assertThat(numatrixNum & generatorId, equalTo((long) generatorId));
      assertThat(numatrixNum & 2147418112, equalTo(((long) i) << 16));
      assertThat(numatrixNum & 9223372034707292160L, equalTo(timestamp << 31));
    }
    long numatrixNum = testTarget.generate();
    assertThat(numatrixNum & generatorId, equalTo((long) generatorId));
    assertThat(numatrixNum & 2147418112, equalTo(0L));
    assertThat(numatrixNum & 9223372034707292160L, equalTo((timestamp + 1) << 31));
  }

  @Test
  public void マイナス出力によって出力できる範囲が大きくなるテスト() throws NumatrixNumberGenerateException {
    int generatorId = 65535;
    final NumatrixNumberGenerator testTarget = new NumatrixNumberGenerator(generatorId) {

      @Override
      public boolean isOutMinus() {
        return true;
      }
    };
    final long timestamp = 286331153;
    new MockUp<System>() {
      final long baseTime = testTarget.getBaseTime().getTime();

      @Mock
      public long currentTimeMillis() {
        return (timestamp * 1000) + baseTime;
      }
    };
    for (int i = 0; i <= 65535; i++) {
      long numatrixNum = testTarget.generate();
      assertThat(numatrixNum & generatorId, equalTo((long) generatorId));
      assertThat(numatrixNum & 4294901760L, equalTo(((long) i) << 16));
      assertThat(numatrixNum & -4294967296L, equalTo(timestamp << 32));
    }

  }

}
