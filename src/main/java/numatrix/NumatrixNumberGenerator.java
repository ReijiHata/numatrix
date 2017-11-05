package numatrix;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * コンストラクタで与えられたこのインスタンスのIDごとに一意な数値を生成します.<br>
 * このクラスのインスタンスは{@link NumatrixNumberGeneratorFactory}が継承されたクラスのインスタンスによって生成されます。<br>
 * <br>
 * このクラスが生成する数値は64ビットを３分割して、それぞれに[タイムスタンプ・連続した番号・このインスタンスのID]で構成されます。<br>
 * このインスタンスのIDはコンストラクタで初期化します。
 * マイナス値を出力しない場合、先頭の1ビットは必ず0になります。マイナスの出力の可否は{@link #isOutMinus()}をオーバーライドして変更します。<br>
 * <br>
 * タイムスタンプは、{@link #getBaseTime()}から返される時間から経過した秒数です。<br>
 * 連続した番号が最大値になった時に更新されます。<br>
 * この秒数が{@link #getTimestampBitLength()}で決められたビット長を超えた場合は、それ以降このインスタンスは使用できません。<br>
 * <br>
 * 連続した番号は0から始まります。最大値は、タイムスタンプとこのインスタンスのIDのビット長（マイナスを出力しない場合は先頭の1ビットも含む）に依存します。<br>
 * 最大値を大きくしたい場合、このインスタンスのIDを小さくするか、{@link #getTimestampBitLength()}をオーバーライドしてタイムスタンプのビット長を短くします。
 */
public class NumatrixNumberGenerator {

  private final int instanceId;
  private final int uniqueueIdBitLength;
  private long maxTimestamp;
  private int seqNumAndUniqueueIdBitLength;
  private int maxSeqNum = Integer.MIN_VALUE;
  private int currentSeqNum = Integer.MAX_VALUE;
  private long currentTimestamp;

  /**
   * 指定されたIDで生成します.
   *
   * @param generatorId このインスタンスのID
   */
  public NumatrixNumberGenerator(int generatorId) {
    instanceId = generatorId;
    int uniqueueIdBitCount = 1;
    while (0 < (generatorId >>> uniqueueIdBitCount)) {
      uniqueueIdBitCount++;
    }
    uniqueueIdBitLength = uniqueueIdBitCount;
  }

  /**
   * このインスタンスを初期化します.
   *
   * @throws NumatrixNumberGenerateException 数値の構成に必要なサイズが64ビットを超えた場合
   */
  private final void init() throws NumatrixNumberGenerateException {
    maxTimestamp = (1L << getTimestampBitLength()) - 1;
    int seqNumBitLength = (isOutMinus() ? 64 : 63) - uniqueueIdBitLength - getTimestampBitLength();
    if (seqNumBitLength <= 0) {
      throw new NumatrixNumberGenerateException("number stracture size over 64bits.");
    }
    seqNumAndUniqueueIdBitLength = uniqueueIdBitLength + seqNumBitLength;
    maxSeqNum = (1 << seqNumBitLength) - 1;
  }

  /**
   * 生成される数値にマイナス値を許可するかを値で返します.<br>
   * マイナス値を許可させたい場合は、オーバーライドして戻り値を{@code true}に変更してください。
   *
   * @return マイナス値を許可する場合は{@code true}、そうでない場合は{@code false}を返します。
   */
  public boolean isOutMinus() {
    return false;
  }

  /**
   * 数値を構成するタイムスタンプのビット長を返します.<br>
   * 32ビット以下に変更したい場合は、オーバーライドして戻り値を変更してください。
   *
   * @return 数値を構成するタイムスタンプのビット長
   */
  public int getTimestampBitLength() {
    return 32;
  }

  /**
   * 数値を構成するタイムスタンプの基準となる時間を返します.<br>
   * 初期値から変更したい場合は、オーバーライドして戻り値を変更してください。
   *
   * @return 数値を構成するタイムスタンプの基準となる時間
   */
  public Date getBaseTime() {
    try {
      return new SimpleDateFormat("yyyyMMdd").parse("2017113");
    } catch (ParseException e) {
      throw new RuntimeException();
    }
  }

  /**
   * 数値を構成するタイムスタンプを返します.<br>
   * タイムスタンプは基準となる時間からの秒数で表します。
   *
   * @return 数値を構成するタイムスタンプ
   */
  private final long makeTimestamp() {
    return (System.currentTimeMillis() - getBaseTime().getTime()) / 1000L;
  }

  /**
   * 現在のタイムスタンプを返します.
   *
   * @return 現在のタイムスタンプ
   */
  public final long getCurrentTimestamp() {
    return currentTimestamp;
  }

  /**
   * 数値を生成します.
   *
   * @return 生成された数値
   * @throws NumatrixNumberGenerateException 数値の構成に必要なサイズが64ビットを超えた場合
   */
  public final long generate() throws NumatrixNumberGenerateException {
    long timestamp = currentTimestamp;
    if (currentSeqNum > maxSeqNum) {
      if (maxSeqNum < 0) {
        init();
      }
      currentSeqNum = 0;
      while (timestamp == (currentTimestamp = makeTimestamp())) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          ;
        }
      }
      if (currentTimestamp > maxTimestamp) {
        throw new NumatrixNumberGenerateException(
            "timestamp element of the number structure is over maximum.");
      }
      timestamp = currentTimestamp;
    }
    int seqNum = currentSeqNum++;
    long timestampFild = ((long) timestamp) << (seqNumAndUniqueueIdBitLength);
    long seqNumFild = ((long) seqNum) << uniqueueIdBitLength;
    return timestampFild | seqNumFild | (long) instanceId;
  }

}
