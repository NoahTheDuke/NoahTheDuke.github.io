{:title "pain at work today"
 :date "2024-03-20T20:42:21.383Z"
 :tags ["cohost mirror" "clojure" "programming" "programming is hell" "functional programming" "\"functional\" programming" "malding"]
 :cohost-id 5189760
 :cohost-url "5189760-pain-at-work-today"}

i try to merge to main, the test run on merge fails. i look at the failed test. it's from early 2022, it's 450 lines long, and it starts with defining 20 atoms (mutable variables) in a top-level let block.

forget who wrote this, i'm mad about whoever approved this.