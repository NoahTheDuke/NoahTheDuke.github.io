{:title "Ocaml wishlist"
 :date "2024-08-20T19:52:11.151Z"
 :tags ["cohost mirror" "clojure" "ocaml" "programming languages" "programming language design" "functional programming" "mutation is bad" "Rich Hickey" "The Value of Values"]
 :cohost-url "7369102-ocaml-wishlist"}

On twitter, [someone asked](https://x.com/andreypopp/status/1825393694708494623): "But seriously, what changes would you make to OCaml which would worth it to break backward compatibility?"

[I replied](https://x.com/NoahTheDuke/status/1825528388271919331) with: "Borrow from Clojure! 1) Make (=) / polymorphic equality work on values (not structure). 2) Make "functional updates" of records nicer. 3) Change ref to work like clj atoms, add `swap!` as a built-in function. 4) Remove the mutable keyword, make ref the only mutating interface."

Our very own @prophet responded ([[1](https://x.com/welltypedwitch/status/1825846669394546879)], [[2](https://x.com/welltypedwitch/status/1825846703842328818)], [[3](https://x.com/welltypedwitch/status/1825846778677084221)], [[4](https://x.com/welltypedwitch/status/1825846860436623424)]):

<details open>
<summary>tweets</summary>
<blockquote>
oh interesting, i think i need an explanation for all of these ^^

1) so by "values" you mean non-recursive, well, values, right? so you would want (=) to work on e.g. ints and bools but not lists? (and what about strings? ^^) how would you enforce that though? you can't use type class constraints and i don't think crashing at runtime would fly in ocaml ^^

2) what would nicer functional updates look like? nested updates?

3) i mean, i'm not against STM (and ocaml has kcas i guess) but using it for the default refs seems like quite the performance hit for programs that don't care about shared memory (which is most ocaml code)

4) hmm why? refs add a layer of indirection and values in refs cannot be unboxed so this would have a pretty decent performance impact (especially if you also include 3) ^^)
</blockquote>
</details>

I'm responding here because twitter sucks:

1) By "values", I would ideally mean Rich Hickey's definition from his talk ["The Value of Values"](https://www.youtube.com/watch?v=-6BsiVyC1kM) except that Ocaml has mutability built in with no real protections against it, so it's more complicated than in Clojure. Ideally, a value is an immutable piece of data that is semantically transparent. `1 = 1`, `"foo" = "foo"`, `[1; 2; 3] = [1; 2; 3]` are obvious, but `(IntSet.of_list [1; 2; 3]) = (IntSet.of_list [2; 1; 3])` should also work. Given that we're breaking backwards compatibility, I feel fine saying, "This is technically feasible, so it can and should be done."
(Ocaml's type checking means that you won't have two values of unknown type, so you don't have to handle `1 = "a"`.)
One of my big gripes with Ocaml is that while it is mostly immutable, it has _many_ built-in methods of mutability, either data types like [Bytes](https://ocaml.org/manual/5.2/api/Bytes.html) or making record fields `mutable`. (See #3 below.) These make it harder to write concurrent or async programs, because the mutable data can sneak in on without notice, and it makes it harder to keep track of mutating state. At least in Clojure, the STM functions (`swap!`, etc) have bangs (`!`) and java interop looks funny (`(.find ^Pattern m)` mutates) so you know to be on high alert with them.

2) I'm not sure what it would look like in Ocaml.
Clojure's records implement all of the interfaces for the built-in persistent data structures, so all of the normal associative functions work on them. You can say `(assoc foo :new-key new-val)` to return a new map with the overwritten kv pair, or `(assoc-in foo [:base-key :middle-key :new-key] 123)` which will either update the nested maps or add new maps with the given keys and values. Likewise, you can `(update {:k []} :k conj 123)` to get back `{:k [123]}` because `(update obj k f & args)` calls `f` on the current value and `args`: `(assoc obj k (apply f (get obj k) args))`. And like `assoc`, you can do `update-in` with a list of keys to dive as deep as necessary.
There's _many_ core functions for working with associative collections, and they can be used on plain hashmaps or records or any custom data type that implements the interface (java interfaces on jvm clojure, other methods on other platforms). `{ person with name = "Noah" }` is roughly the same number of characters as `(assoc person :name "Noah")`, but the other stuff is where it gets more annoying/cumbersome, due to the repetition: `{ person with num_friends = person.num_friends + 5 }`.
Additionally, because the Clojure functions have a consistent api (the object is in the first position), they can be threaded: `(-> {:k [] :extra "noah"} (update :k conj 123) (assoc :foo :bar) (dissoc :extra))` produces `{:k [123] :foo :bar}`. The same changes would require `let foo = ... in let foo = ... in let foo = ... in` until your fingers hurt.
And yes, I know there's a big difference between Clojure's hashmaps and Ocaml's records, but it's one of the pain points I ran into a lot working on my little app a couple weeks ago.
(I'm not even touching on how awkward the Map and Set apis are in Ocaml, lol. This is already too long.)

3) and 4) I think that it's a footgun to make the easiest paths not thread safe. Clojure has `atom` which is thread safe, and `volatile!` which is not thread safe but is significantly faster (like `ref` in Ocaml). Modifying atoms uses `swap!` or `reset!` etc and have different argument orders so you can't call them like normal collection functions. Same with volatiles, except they're `vswap!` and `vreset!` etc.
I know that it'd be easy to point to Clojure and say, "With volatiles, sounds like you're no different than Ocaml" and I think to a point that's true, but I think the reliance on awkward functions forces you the programmer to make the decision at every step "this is what I want to keep doing???"
**Mutation is bad but non-thread safe mutation is even worse.** Given the prevalence of libraries like Lwt and now Ocaml 5 bringing in shared memory parallelism, there's no reason to open yourself or others up to thread safety issues.
I understand the performance issues, so I don't want to disallow mutation, but I want it to be really! fucking! annoying!

(I've deliberately not mentioned the other kinds of STM in Clojure, cuz they're rarely used and I think an experiment that failed. If folks want shared memory, they use external services like a real database/kafka/redis, etc.)

If I'd had more space, I would have ended with "5) Switch the sytnax to Clojure's dialect of Lisp" but alas, that will have to wait until another day.
