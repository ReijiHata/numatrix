# 概要
numatrixは並列で動作する分散環境で一意な64ビットの数値を生成するフレームワークです。
# 特徴
分散したJVMごとに一意なIDを持つことで、それぞれが独立しながら全体で一意な数値を生成できます。
それぞれが独立しているため、非常に高速で動作します。
スレッドセーフで、スレッドごとに並列で動作します。
## 仕様
numatrixは64ビットを３つのフィールドに分解し、それぞれに値を割り当てます。
1. タイムスタンプ
このフィールドのビット数は初期設定で32ビットです。
値は**基準となる時間**から経過した秒数です。基準となる時間は初期設定で2017年11月3日0時です。
値は数値を発行を開始した時に決定され、次の**連続した番号**が0に戻った時に更新されます。
2. 連続した番号
このフィールドのビット数は前の**タイムスタンプ**と次の**環境で一意なID**が消費するビット数の余りです。
値は0から開始し、数値を発行するたびにインクリメントされます。このフィールドのビット数で表すことのできる数値を越えると0に戻り、前の**タイムスタンプ**の値を更新します。
3. 環境で一意なID
このフィールドのビット数は、使用する環境の最大JVM数と最大スレッド数によって決定されます。
値は環境で必ず一意になるように発行された数値です。

先頭のビットは符合を表すので、マイナス値を出力しない場合は0で固定され、使用されません。
# 使い方
## NumatrixNumberGeneratorFactoryを実装
NumatrixNumberGeneratorFactoryクラスを継承したクラスを作成します。
```
package numatrix;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class NumatrixNumberGeneratorFactoryImple extends NumatrixNumberGeneratorFactory {

  @Override
  protected int getTypeId() {
    return 0;
  }

  @Override
  protected int getJvmId() {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream("プロパティファイルのパス"));
  } catch (IOException e) {
      throw new RuntimeException(e);
  }
    return Integer.parseInt(properties.getProperty("jvmid"));
  }

  @Override
  protected int getMaxJvmCount() {
    return 100;
  }

  @Override
  protected int getMaxGeneratorCount() {
    return 300;
  }

}

```
### getTypeId()の実装
このメソッドの返却値ごとに一意な数値が生成できます。
複数の一意な数値を運用する場合、このメソッドを実装する複数のクラスを作成し、使い分けてください。
### getJvmId()の実装
数値を一意としたい環境でこの実装クラスを使うJVMごとに一意な値を返却してください。
返却値は0から順番に使用してください。
### getMaxJvmCount()の実装
数値を一意としたい環境でこの実装クラスを使うJVMの上限数を返却してください。
### getMaxGeneratorCount()の実装
JVMごとに生成されるNumatrixNumberGeneratorインスタンスの上限数を返却してください。
NumatrixNumberGeneratorインスタンスは、一意な数値を生成するスレッドごとに割り当てられます。
## NumatrixNumberGeneratorインスタンスから数値を生成
NumatrixNumberGeneratorFactoryの実装クラスのインスタンスから、NumatrixNumberGeneratorインスタンスを取得し、一意な数値を生成します。
```
public class Sample {

  public static void main(String[] args) throws NumatrixNumberGenerateException {
    NumatrixNumberGeneratorFactoryImple factory = new NumatrixNumberGeneratorFactoryImple();
    NumatrixNumberGenerator generator = factory.getGenerator();
    long uniqueueNumber = generator.generate();

  }

}
```
- NumatrixNumberGeneratorインスタンスの取得
getGenerator()の返却値のNumatrixNumberGeneratorインスタンスは、それを呼んだスレッドごとに異なります。
getGenerator()を呼んだスレッドの参照がなくなり、ガベージコレクションによって消滅できるようになった場合、そのスレッドのNumatrixNumberGeneratorインスタンスは別のスレッドに再利用されます。
- 一意な数値の生成
NumatrixNumberGenerator#generate()で一意な数値を生成できます。
# 注意事項
**numatrixは2017年から約136年後に利用できなくなります。**
回避方法はカスタマイズで説明します。
# カスタマイズ
NumatrixNumberGeneratorクラスを継承したクラスを作成し、各メソッドをオーバーライドすることでカスタマイズできます。
NumatrixNumberGeneratorFactoryの実装クラスでgetGenerator()メソッドをオーバライドして、継承したクラスのインスタンスを返却するように実装します。
```
public class NumatrixNumberGeneratorFactoryImple extends NumatrixNumberGeneratorFactory {

  class NumatrixNumberGeneratorEx extends NumatrixNumberGenerator {
    // TODO NumatrixNumberGeneratorをカスタマイズ
    public NumatrixNumberGeneratorEx(int generatorId, int generatorIdBitLength) {
      super(generatorId, generatorIdBitLength);
    }

  }

  @Override
  protected NumatrixNumberGenerator makeGenerator(int generatorId, int generatorIdBitLength) {
    return new NumatrixNumberGeneratorEx(generatorId, generatorIdBitLength);
  }
```
## 一意な数値の秒間最大出力数の増加（性能向上）
64ビットを３分割したフィールドの１つの**連続した番号**の最大値を超えると、タイムスタンプフィールドの値を更新しなければなりませんが、タイムスタンプフィールドは１秒単位で更新されるため、
**連続した番号の最大値 = スレッド毎の一意な数値の秒間最大出力数**
となります。
連続した番号のビット数は、他のフィールドが消費するビット数の余りなので、**他のフィールドのビット数を小さくすることで最大値を大きくできます。**
### マイナス値の出力
マイナス値の出力を許可することで、64ビットの先頭1ビットが利用できるようになり、連続した番号フィールドのビット数を増やすことができます。
```
public class NumatrixNumberGeneratorEx extends NumatrixNumberGenerator {

  public NumatrixNumberGeneratorEx(int generatorId, int generatorIdBitLength) {
    super(generatorId, generatorIdBitLength);
  }

  @Override
  public boolean isOutMinus() {
    return true;
  }

}
```
### タイムスタンプフィールドのビット数変更
タイムスタンプフィールドのビット数を減らすことで、連続した番号フィールドのビット数を増やすことができます。
タイムスタンプフィールドのビット数は初期値で32ビットで、最大約136年の秒数を表すことができますが、ビット数を１つ減らすたびに、タイムスタンプの最大値は半減し、利用できる期限が短くなります。
```
public class NumatrixNumberGeneratorEx extends NumatrixNumberGenerator {

  public NumatrixNumberGeneratorEx(int generatorId, int generatorIdBitLength) {
    super(generatorId, generatorIdBitLength);
  }

  @Override
  public int getTimestampBitLength() {
    // 32ビットから減らす
    return 31;
  }

}
```
## 利用できる期限（2017年から136年）の延長
64ビットを３分割したフィールドの１つの**タイムスタンプ**は、フィールドのビット数（初期設定で32ビット）で表すことのできる数値を超えることができません。
タイムスタンプは基準時間（初期設定で2017年11月3日0時）から経過した秒数なので、**基準時間を変更することで利用できる期限を延長できます。**
### 基準時間の変更
基準時間を変更します。
基準時間はnumatrixの利用を開始する時間より前の時間を定義してください。
```
public class NumatrixNumberGeneratorEx extends NumatrixNumberGenerator {

  public NumatrixNumberGeneratorEx(int generatorId, int generatorIdBitLength) {
    super(generatorId, generatorIdBitLength);
  }

  @Override
  public Date getBaseTime() {
    try {
      // 基準時間の変更
      return new SimpleDateFormat("yyyyMMdd").parse("20171111");
    } catch (ParseException e) {
      throw new RuntimeException();
    }
  }

}
```
