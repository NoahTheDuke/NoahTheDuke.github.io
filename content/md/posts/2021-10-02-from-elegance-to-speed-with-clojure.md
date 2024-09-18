{:title "From Elegance to Speed, with Clojure"
 :date "2021-10-02T22:33"
 :tags ["clojure" "personal" "programming"]}

I recently came across a [blogpost](http://johnj.com/from-elegance-to-speed.html) about rewriting an "elegant" function in Clojure into an optimized function in Common Lisp.

In it, John Jacobsen discusses how they went from this Clojure code:

```clojure
(defn smt-8 [times]
  (->> times
       (partition 8 1)
       (map (juxt identity
                  (comp (partial apply -)
                        (juxt last first))))
       (filter (comp (partial > 1000) second))))
```

to this Common Lisp code:

```clojure
(loop for x below (- (the fixnum array-size) 8)
     if (< (- (the fixnum (elt *s* (+ (the fixnum x) 8)))
              (the fixnum (elt *s* (the fixnum x))))
           1000)
     collect x)
```

and achieved a "nearly 300x" increase in speed. It's a fun post, finding a better algorithm and diving into the generated assembler and making slow, steady progress on reducing slowdowns.

I took a small, playful offense to the comparison of the unoptimized Clojure and the heavily optimized Common Lisp, but instead of just [commenting on it](https://news.ycombinator.com/item?id=28724008), I decided to try my hand at optimizing the Clojure code, to see what heights can be achieved!

I am not a great writer and find it hard to write extemporaneously, preferring to let the code speak for me. John is an excellent writer and I don't wish to overshadow their work with overwrought prose. Therefore, I am going to mostly focus on my code and try to describe the steps along the way.

# Set up

All of my code has been written and run on a 2014 MacBook Pro with a 2.8 GHz Intel Core i7 processor and 16 Gb DDR3 ram. I am running Clojure 1.10.2, javac 1.8.0\_292.

I've adapted Clojure's core function `time` to display the results to match John's:

```clojure
(defmacro timing [& forms]
  `(let [start# (System/nanoTime)
         result# (do ~@forms)]
     (str "duration "
          (/ (double (- (System/nanoTime) start#)) 1000000.0)
          " msec... result " result#)))
```

And how I've adapted John's data generation:

```clojure
; 10 million
(def initial-size 10000000)

(defn time-sequence [n]
  (take n (iterate #(+ % (rand-int 1000)) 0)))

(def *s* (time-sequence initial-size))
```

> [!NOTE]
> Before we dive into code and times, I want to preface that I don't really understand how John is determining `kHz` and `MHz` when discussing performance. I'd like to be able to more easily compare our two sets of numbers (so that for a given version I can figure out the ratio needed to compare), but alas, the provided Clojure times are fuzzy, so I am stuck relying strictly on the Common Lisp "msec" output.

# Initial version

To start, running `(timing (count (smt-8 *s*)))` gives these times:

```clojure
duration 17154.473751 msec... result 2025
duration 17163.81631 msec... result 2025
duration 17149.326539 msec... result 2025
```

That's 17 full seconds. I don't need John's Clojure times to know this is bad, hah. There are some immediate improvements to be made, so let's dive right in instead of lollygagging here.

# Loose Change

To whit, the functional programming functions should be excised. `partial`, `juxt`, `apply`, and `comp` have their place but should be used sparingly instead of as the go-to:

- `partial` always creates a variadic function, collects the missing arguments, and then uses `apply` to pass those in when called. This requires multiple additional calls and overhead. Much better to write an anonymous function (using either `#()` or `fn` form) and specify the placement of the missing argument.
- For a proof-of-concept, `juxt` allows for expressive computations. However, we should be wary of `(juxt identity ...)` because that indicates we're adding overhead (extra function calls) for only slight readability gains: `(juxt identity ...)` is equivalent to `(fn [input] [input ...])`.
- `apply` is perfect for when the number of inputs is unknown, but in a function like ours, we know the number of inputs and should just write them out directly.
- Lastly, I just don't like `comp` very much, hah. It has uses in functional composition, but in almost all cases, I prefer to see the composition written explicitly.

All of this together means that the `map` fn can be easily rewritten as `(fn [input] [input (- (last input) (first input))])`, and the `filter` fn as `#(> 1000 (second %))`:

```clojure
(defn try-1 [times]
  (->> times
       (partition 8 1)
       (map (fn [input] [input (- (last input) (first input))]))
       (filter #(> 1000 (second %)))))

(timing (count (try-1 *s*)))
```

This gives us:

```clojure
duration 15057.01869 msec... result 2025
duration 15084.098025 msec... result 2025
duration 15141.594631 msec... result 2025
```

A small speed up, but 2 seconds saved is a win in my book. What's our next step?

# Looping Louie

Ah yes, of course. The first suggestion any time speed is required in Clojure: convert your sequences to `` loop`s. This requires inlining both the `map `` and `filter` transformations, which makes our code harder to understand and more brittle, but we're here for speed lol.

> [!NOTE]
> I'm still relying on `partition` and `first`/`last` because I think it's important to show how each step of the process changes 1) the shape of the code and 2) the speed. My process for writing all of this code, of course, was not so smooth. I have some experience writing "fast" Clojure and dove right into some of the more unwieldy versions, but backfilling these early versions is a good refresher as well as a reminder of just how much is required to make Clojure performant.

```clojure
(defn try-2 [times]
  (loop [coll (partition 8 1 times)
         ret []]
    (if (seq coll)
      (let [cur (first coll)
            diff (- (last cur) (first cur))]
        (recur
          (next coll)
          (if (< diff 1000)
            (conj ret diff)
            ret)))
      ret)))

(timing (count (try-2 *s*)))
```

This gives us:

```clojure
duration 14301.27021 msec... result 2025
duration 13896.824518 msec... result 2025
duration 13665.55618 msec... result 2025
```

Another small improvement but still far behind even the first version of the Common Lisp implementation.

# Hinting at a Solution

Our next area for exploration is three-fold:

1) Like John, we can gain a sizeable speed increase by changing the algorithm. `parition` is slow, and only looking at the first and last of each partition can be done purely through indexing.
2) We'll switch from a sequence to a vector. Vectors allow for O(1) access, which we need.
3) Type-hinting our variables tells Clojure *how* to compile the code, bypassing a lot of unnecessary checks. This lets us also use the `nth` method on the `times` object itself, instead of calling the generic `nth` function that has to do extra work to end up calling `.nth` anyways. And by type-hinting `array-size`, we can make sure that all of the math is done `long` to `long`, instead of boxed and unboxed math.

(I have changed the return value because I don't see the need for the extra information.)

```clojure
; To maintain the same list, just now in a vector
(def *s-vec* (vec *s*))

(defn try-3 [^IPersistentVector times]
  (let [limit (- ^long array-size 8)]
    (loop [idx 0
           ret (transient [])]
      (if (< idx limit)
        (recur
          (inc idx)
          (if (< (- (.nth times (+ idx 8))
                    (.nth times idx))
                 1000)
            (conj! ret idx)
            ret))
        (persistent! ret)))))

(timing (count (try-3 *s-vec*)))
```

This gives us:

```clojure
duration 366.385397 msec... result 248
duration 345.759279 msec... result 248
duration 393.763997 msec... result 248
```

366 msec! 248 count? The duration seems off and the result count is definitely off. What the hell.

It turns out that John's algorithm has a bug. When you partition a list into sets of 8, selecting first and last elides the index of those two elements. Because `nth` is zero-based indexing, that means the first element will be at index 0 and the last element will be at index 7, not index 8. (The same is true in Common Lisp's `elt`, but I haven't run that code to find out what changes.)

Fixing the bug fixes the result, but doesn't change the duration(!) much:

```clojure
(defn try-3-1 [^IPersistentVector times]
  (let [limit (- ^long array-size 8)]
    (loop [idx 0
           ret (transient [])]
      (if (< idx limit)
        (recur
          (inc idx)
          (if (< (- (.nth times (+ idx 7))
                    (.nth times idx))
                 1000)
            (conj! ret idx)
            ret))
        (persistent! ret)))))

(timing (count (try-3-1 *s-vec*)))
```

Gives us:

```clojure
duration 311.822859 msec... result 2025
duration 311.952356 msec... result 2025
duration 335.687037 msec... result 2025
```

# Up, up, and array!

The final change comes from using a Java primitive directly: types Arrays. If we use a `long-array` instead of a vector, we can use `aget` which compiles to native indexing (`((long[])times)[RT.intCast(Numbers.add(idx, 8L))]`), which is *quite* fast:

```clojure
(def ^longs *s-array* (long-array *s*))

(defn try-4 [^longs times]
  (let [limit (- ^long array-size 8)]
    (loop [idx 0
           ret (transient [])]
      (if (< idx limit)
        (recur
          (+ idx 1)
          (if (< (- ^long (aget times (+ idx 8))
                    ^long (aget times idx))
                 1000)
            (conj! ret idx)
            ret))
        (persistent! ret)))))

(timing (count (try-4 *s-array*)))
```

This gives us:

```clojure
duration 33.565959 msec... result 2025
duration 31.210151 msec... result 2025
duration 33.146611 msec... result 2025
```
That's very nice to see. Quite satisfying.

# 10 vs 100

Wait a minute, John changes the size of his dataset! We've been working with 10 million entries so far. How much do we lose when moving up a factor of 10?

Unsurprisingly, the duration increases by slightly less than a factor of 10. Here's the output for `try-3-1` and `try-4` at 100 million entries (the earlier functions are much too slow and not worth waiting the 10+ minutes for):

```clojure
; try-3-1
duration 18781.001752 msec... result 20310
duration 17746.880043 msec... result 20310
duration 19173.168311 msec... result 20310

; try-4
duration 293.336084 msec... result 20310
duration 298.760106 msec... result 20310
duration 295.547215 msec... result 20310
```

# Final thoughts

And there you have it. After reducing the speed as much as I think is reasonable (if not wholly possible), we are within striking distance of Common Lisp! Time for bed.
