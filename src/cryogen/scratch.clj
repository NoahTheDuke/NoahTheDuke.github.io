(ns cryogen.scratch
  (:require
    [clj-java-decompiler.core :refer [decompile]]
    [criterium.core :refer [benchmark report-result quick-bench]])
  (:import
   (clojure.lang IPersistentVector)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(defmacro timing [& forms]
  `(let [start# (System/nanoTime)
         result# (do ~@forms)]
     (str "duration "
          (/ (double (- (System/nanoTime) start#)) 1000000.0)
          " msec... result " result#)))

(defn time-sequence [n]
  (take n (iterate #(+ % (rand-int 1000)) 0)))

(defn time-seq-loop [^long n]
  (loop [y 0
         ret (transient [])]
    (if (< (.count ^IPersistentVector ret) n)
      (let [higher (+ y (long (rand-int 1000)))]
        (recur
          higher
          (conj! ret higher)))
      (persistent! ret))))

; 10 million
(do (def initial-size (long 1e6))
    (def s-init (seq (time-seq-loop initial-size)))
    (def ^IPersistentVector s-vec (vec s-init))
    (def ^longs s-array (long-array s-init)))

(defn smt-8 [times]
  (->> times
       (partition 8 1)
       (map (juxt identity
                  (comp (partial apply -)
                        (juxt last first))))
       (filter (comp (partial > 1000) second))))

; 10 million
; duration 17154.473751 msec... result 2025
; duration 17163.81631 msec... result 2025
; duration 17149.326539 msec... result 2025

(comment
  (count (partition 8 1 s-init))
  (report-result (benchmark (count (smt-8 s-init)) {}))
  )

(defn try-1 [times]
  (->> times
       (partition 8 1)
       (map (fn [input] [input (- (last input) (first input))]))
       (filter #(> 1000 (second %)))))

; 10 million
; duration 15057.01869 msec... result 2025
; duration 15084.098025 msec... result 2025
; duration 15141.594631 msec... result 2025

(comment
  (report-result (benchmark (count (try-1 s-init)) {}))
  )

(defn try-2 [times]
  (loop [coll (partition 8 1 times)
         ret []]
    (if (seq coll)
      (let [cur (first coll)
            diff (- (last cur) (first cur))]
        (recur
          (next coll)
          (if (< diff 1000)
            (conj ret (first cur))
            ret)))
      ret)))

; 10 million
; Execution time mean : 13.073442 sec

(comment
  (report-result (benchmark (count (try-2 s-vec)) {}))
  )

(defn try-3 [^IPersistentVector times]
  (let [limit (- ^long initial-size 8)]
    (loop [idx 0
           ret (transient [])]
      (if (< idx limit)
        (recur
          (inc idx)
          (if (< (- (.nth times (+ idx 7))
                    (.nth times idx))
                 1000)
            (conj! ret (.nth times idx))
            ret))
        (persistent! ret)))))

; 10 million
; Execution time mean : 367.067754 ms

(comment
  (take 10 (try-3 s-vec))
  (report-result (benchmark (count (try-3 s-vec)) {}))
  )

(defn try-4
  [^longs times]
  (let [limit (- ^long initial-size 7)]
    (loop [idx 0
           ret (transient [])]
      (if (< idx limit)
        (recur
          (+ idx 1)
          (if (< (- ^long (aget times (+ idx 7))
                    ^long (aget times idx))
                 1000)
            (conj! ret ^long (aget times idx))
            ret))
        (persistent! ret)))))

; 10 million
; Execution time mean : 25.103292 ms

(comment
  (take 10 (try-4 s-array))
  (quick-bench (count (try-4 s-array)))
  (report-result (benchmark (count (try-4 s-array)) {}))
  )

(defn transducers-try
  [^IPersistentVector times]
  (into []
        (keep (fn [idx]
                (when (< (- (nth times (+ idx 7))
                            (nth times idx))
                         1000)
                  (nth times idx))))
        (range (- (count times) 8))))

; 10 million
; Execution time mean : 418.438222 ms

(comment
  (report-result (benchmark (count (transducers-try s-vec)) {}))
)

; (def try-2-result (try-2 s-init))
; (def try-3-1-result (try-3 s-vec))
; (def try-4-result (try-4 s-array))
; (def trans-result (transducers-try s-vec))
(defn unrolled
  [^longs arr]
  (let [l (unchecked-subtract-int (alength arr) 7)]
    (loop [idx (int 0) agg ()]
      (if (< idx l)
        (recur
          (unchecked-inc-int idx)
          (if (> 1000 (- (aget arr (unchecked-add-int idx 7)) (aget arr idx)))
            (.cons agg (aget arr idx))
            agg))
        (reverse agg)))))

(comment
  (= (try-4 s-array) (unrolled s-array))
  (count (unrolled s-array))
  (quick-bench (count (unrolled s-array)))
  (report-result (benchmark (count (unrolled s-array)) {}))

  (def alpha [:a :b :c :d :e :f :g :h :i :j :k :l :m :n :o :p :q :r :s :t :u :v :w :x :y :z])
  (let [limit (- (count alpha) 7)]
    (loop [idx 0 agg []]
      (if (<= idx limit)
        (recur
          (inc idx)
          (conj agg (subvec alpha idx (+ idx 7))))
        agg)))
  )
