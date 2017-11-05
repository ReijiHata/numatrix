package numatrix;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link #getTypeId()}の実装毎に一意な数値を生成する{@link NumatrixNumberGenerator}を生成する抽象クラスです.<br>
 * {@link NumatrixNumberGenerator}のインスタンスは{@link #getGenerator()}で取得できます。
 *
 */
public abstract class NumatrixNumberGeneratorFactory {

  private static final Map<Integer, RecyclePool<NumatrixNumberGenerator>> generatorPoolMap =
      new HashMap<>();

  private static final Map<Integer, AtomicInteger> generatorNumberMap = new HashMap<>();

  /**
   * 実装クラスの区分を返します.<br>
   * 区分は実装クラス毎に固定で一意でなければなりません。
   *
   * @return 区分
   */
  protected abstract int getTypeId();

  /**
   * このクラスを使用するJVMのIDを返します.<br>
   * JVMのIDは同じクラスを使用するJVM毎に一意でなければなりません。
   *
   * @return このクラスを使用するJVMのID
   */
  protected abstract int getJvmId();

  /**
   * クラス毎の{@link NumatrixNumberGenerator}インスタンス数の上限値を返します.<br>
   *
   * @return クラス毎の{@link NumatrixNumberGenerator}インスタンス数の上限値
   */
  protected abstract int getMaxGeneratorCount();

  /**
   * {@link NumatrixNumberGenerator}インスタンスを生成します.<br>
   * {@link NumatrixNumberGenerator}を継承したクラスを生成させたい場合、オーバーライドして戻り値を変更してください。
   *
   * @param generatorId {@link NumatrixNumberGenerator}インスタンスのID
   * @return {@link NumatrixNumberGenerator}インスタンス
   */
  protected NumatrixNumberGenerator makeGenerator(int generatorId) {
    return new NumatrixNumberGenerator(generatorId);
  }

  /**
   * {@link NumatrixNumberGenerator}インスタンスを返します.
   *
   * @return {@link NumatrixNumberGenerator}インスタンス
   * @throws NumatrixNumberGenerateException {@link NumatrixNumberGenerator}インスタンスを生成できない場合
   */
  public final NumatrixNumberGenerator getGenerator() throws NumatrixNumberGenerateException {
    RecyclePool<NumatrixNumberGenerator> generatorPool;
    synchronized (generatorPoolMap) {
      if ((generatorPool = generatorPoolMap.get(getTypeId())) == null) {
        generatorPool = new RecyclePool<NumatrixNumberGenerator>();
        generatorPoolMap.put(getTypeId(), generatorPool);
      }
    }
    NumatrixNumberGenerator generator = generatorPool.get();
    if (generator != null) {
      return generator;
    }
    synchronized (generatorPool) {
      if (generatorPool.size() >= getMaxGeneratorCount()) {
        generatorPool.refresh();
        generator = generatorPool.get();
        if (generator != null) {
          return generator;
        }
        throw new NumatrixNumberGenerateException("generator instance count is over maximum. "
            + "please reduce threads that using this class.");
      }
      AtomicInteger generatorNumber;
      synchronized (generatorNumberMap) {
        if ((generatorNumber = generatorNumberMap.get(getTypeId())) == null) {
          generatorNumber = new AtomicInteger();
          generatorNumberMap.put(getTypeId(), generatorNumber);
        }
      }
      int generatorId = (getJvmId() * getMaxGeneratorCount()) + generatorNumber.getAndIncrement();
      generator = makeGenerator(generatorId);
      generatorPool.add(generator);
    }
    return generator;
  }
}
