# Java BigDecimal key equality debugging lab

`BigDecimal` を `Map` のキーに使ったとき、同じ数値を異なるスケールで受け取ると別キーとして扱ってしまう不具合を、標準 Java だけで再現します。

## 前提

- Java 21 以上
- Maven や外部依存関係は不要

## 実行

```bash
./run-tests.sh
```

バグ再現コミットでは、`0.10` と `0.100` が同じ税率を表すという契約に対して、税率検索が空になります。修正後は同じテストが回帰テストとして成功します。

## 構成

- `src/main/java/lab/TaxRateTable.java`: 税率テーブル
- `src/main/java/lab/InvoiceCalculator.java`: 税額計算の境界
- `src/test/java/lab/TaxRateTableTest.java`: 失敗テストと回帰テスト
- `docs/debugging-record-ja.md`: 実測に基づく調査記録
