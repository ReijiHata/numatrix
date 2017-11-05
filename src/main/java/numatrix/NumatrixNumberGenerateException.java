package numatrix;

/**
 * Numatrixで発生する例外です.
 *
 */
public class NumatrixNumberGenerateException extends Exception {

  /**
   * {@link Exception}に例外メッセージを渡します.
   *
   * @param msg 例外メッセージ
   */
  public NumatrixNumberGenerateException(String msg) {
    super(msg);
  }
}
