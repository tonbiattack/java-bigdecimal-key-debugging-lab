# デバッグ記録: BigDecimal のスケール差による税率検索失敗

## 対象の不具合

税率テーブルに `0.10` を登録し、呼び出し側が同じ数値を `0.100` として渡したとき、税率を見つけて税額を計算する契約が破られる。境界では `IllegalArgumentException` が発生し、最終状態として期待した税額 `10.00` は得られない。

| 観測点 | 期待値 | バグ状態の実際値 |
| --- | --- | --- |
| 境界応答または例外 | 税額 `10.00` を返す | `IllegalArgumentException: 税率が登録されていません: 0.100` |
| 最終状態 | 計算結果 `10.00` | 計算結果なし |
| 保持対象 | 登録時と同じスケールの `0.10` でも `10.00` | `0.10` では計算できる |

## 再現条件

バグ状態のコミットは `61f4b9ec4e1b4508b8ba9c83d23a3ca7b2cfe3a8` です。

```bash
git checkout 61f4b9e
./run-tests.sh
```

実際の出力は次のとおりです。

```text
Exception in thread "main" java.lang.IllegalArgumentException: 税率が登録されていません: 0.100
    at lab.InvoiceCalculator.lambda$calculateTax$0(InvoiceCalculator.java:14)
    at java.base/java.util.Optional.orElseThrow(Optional.java:403)
    at lab.InvoiceCalculator.calculateTax(InvoiceCalculator.java:14)
    at lab.TaxRateTableTest.sameNumericRateWithDifferentScaleIsAccepted(TaxRateTableTest.java:17)
    at lab.TaxRateTableTest.main(TaxRateTableTest.java:7)
```

## 調査

| 確認対象 | 観測結果 | 判断 |
| --- | --- | --- |
| 入力 | 登録キーは `new BigDecimal("0.10")`、検索キーは `new BigDecimal("0.100")`。数値は同じだが scale は 2 と 3。 | 入力値の数値そのものではなく表現の差がある。 |
| 境界出力 | 検索結果が空になり、`InvoiceCalculator` の `orElseThrow` が例外を送出する。 | 表面的な成功応答に隠れた下流状態ではなく、境界で契約違反を確認できる。 |
| 最終状態 | 税額オブジェクトは生成されない。登録済みの `0.10` を `0.10` で検索した制御ケースは通過する。 | 登録処理と計算処理全体ではなく、検索キーの同値性が焦点になる。 |
| ログまたはデバッガー | 追加のログやデバッガーは不要だった。スタックトレースと最小の入力差分で、失敗箇所は `HashMap.get` を経由する `TaxRateTable.findTaxPercent` に限定できた。 | 非同期処理、環境変数、タイムゾーン、外部サービスは原因候補から除外した。 |
| 実装、設定、仕様 | `TaxRateTable` は `HashMap<BigDecimal, BigDecimal>` に対して、正規化なしで `put` と `get` を行っていた。Java の `Map` はキー照合を `equals` に基づいて定義し、`BigDecimal` は数値が同じでも scale が異なると `equals` では同一にならない。 | 直接原因は、業務上の数値同値性を `HashMap` のキー同値性へ変換していなかったこと。 |

## 原因

`BigDecimal` は数値と scale を持つ。Java の公式 API は、異なる scale で同じ数値を表す値を同じ cohort と説明し、自然順序では同値として扱う一方、`equals` は数値と表現の両方を要求すると記載している。[1] また、`Map` のキー照合は `equals` を基準に定義される。[2]

この再現では、登録時の `0.10` と検索時の `0.100` は `compareTo` なら同値だが、`HashMap` のキー照合では同一キーにならない。そのため `rates.get(requestedRate)` が `null` となり、税率未登録の例外に変換された。

## 修正

`TaxRateTable` の境界で `stripTrailingZeros()` を使い、登録時と検索時の双方を同じ表現へ正規化した。業務契約は「税率の数値が同じならスケールに依存せず検索できる」であり、正規化を一箇所に閉じ込めることで、呼び出し側へ `BigDecimal` の表現規約を漏らさずに契約を満たす。

修正コミットは `2955484391a070165f033929dfab18f5d2757e8e` である。`0.10` と `0.100` を同じ canonical key に変換する変更だけであり、税額計算の式や例外の意味は変更していない。

## 回帰確認

```bash
git checkout main
./run-tests.sh
```

実測結果:

```text
PASS: all tests
```

変更対象のスケール差ケースは成功し、保持対象である登録時と同じスケールのケースも成功した。テストは外部依存関係を持たず、Java 21 の `javac` と `java` だけで実行した。

## 設計上の制約

この修正は税率テーブルのキーに限って数値同値性を採用する。金額の表示桁、丸め規則、通貨単位、税込・税抜の業務ルールを定義するものではない。また、`BigDecimal` を `SortedMap` や `SortedSet` で使う場合の自然順序と `equals` の不一致は別の設計論点であり、このラボでは扱わない。
