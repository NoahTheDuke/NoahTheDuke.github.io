{:title "What is this data structure?"
 :date "2024-03-05T04:41:30.424Z"
 :tags ["cohost mirror" "data structures" "programming" "functional programming"]
 :cohost-url "4896592-what-is-this-data-st"}

I re-discovered a data structure I used many years ago from a game engine. It’s like a queue but is well built for recursive or nested work. The name I know it as is the Pipeline.

The data structure is `{ processed: queue, added: queue }`. It has the methods `enqueue(…args: any[]): pipeline`, `peek(): any`, and `pop(): pipeline`.

When you call `enqueue`, you pass in any number of items, which are added to the back of the `added` internal queue. Like a classic queue, if `enqueue` is called multiple times in a row, each set of items is added at the end.

When you call `pop`, the contents of `added` are prepended to the contents of `processed`, and `added` is cleared, then the pipeline is returned.

When you call `peek`, you get the first item in `added` or `processed`.

Okay so that’s the implementation. What does it look like? Here’s how it works in Clojure:

```clojure
(-> (make-pipeline) ;; {:processed <-()-< :added <-()-<}
    (queue 1 2 3)  ;; {:processed <-()-< :added <-(1 2 3)-<}
    (queue 4 5 6)  ;; {:processed <-()-< :added <-(1 2 3 4 5 6)-<}
    (pop)  ;; {:processed <-(2 3 4 5 6)-< :added <-()-<}
    (queue 7 8 9)  ;; {:processed <-(2 3 4 5 6)-< :added <-(7 8 9)-<}
    (pop)) ;; {:processed <-(8 9 2 3 4 5 6)-< :added <-()-<}
```

Does that make sense? I hope so.

I know it as a pipeline, name taken from the game engine I learned it from years ago, but I’ve never seen it since. It’s very useful when processing sets of things with potential items that will have their own nested sets of things that must be accomplished first, each of which may also etc etc.

What is it?????
