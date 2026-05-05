{:title "Performance Chasing"
 :date "2023-07-27T21:00:36.056Z"
 :tags ["cohost mirror" "programming" "rust" "rust-lang" "Rewrite it in Rust!" "Zig" "clojure"]
 :cohost-url "2235168-performance-chasing"}

I spend a lot of time thinking about my linter’s performance, and I’ve changed some of the ways I program in Clojure to support that. But it also means that I’ve sacrificed some of the benefits of Clojure and I’m not always writing “idiomatic” Clojure.

I dream of moving to a language like Rust or Zig, one that pursues pure speed and efficiency, but I know Clojure and I’m intimately familiar with it, so it would be quite a loss of effort and time to just get back to where I am currently.

And for what? My linter runs fast enough! It can lint 120k lines of Clojure in 15 seconds. Is that Ruff speeds? No but it’s pretty dang good.

Buuuuut it could be faster, and that gnaws at me lol

---

honestly, the big thing preventing me from actually trying this out is having to write a Clojure parser from scratch in whichever language I choose. Clojure's grammar is LL(1) I think, can be done with a fairly simple recursive-descent parser, but there's a bunch of small edge cases that make it kind of annoying (namespaced maps, for example), and then I have to come up with some representation of Clojure's data structures and use those everywhere.

Actually, every part of this is annoying lmao

---

cuz right now I have a pattern-matching macro that takes in Clojure forms and spits out "efficient" functions that perform the matches and bind variables, so I can just write `(defrule lint/if-not-both "docstring" {:pattern '(if (not ?x) ?y ?z) :message "Use `if-not` instead of recreating it." :replace '(if-not ?x ?y ?z)}` and the `defrule` macro will transform the `:pattern` into the efficient matching and binding function and `:replace` will transform into the efficient substitution function, and the mechanics inside are all just clojure functions saying stuff like `(if (map? form) ...)`.

To do this in Rust, for example, I'd have to write a pattern-matching function (def not a macro, fuck rust macros) that takes a string and parses it, and once we're using strings, so much of the niceties of the macro are lost. Maybe there's stuff to be gained, but really how much?

I'm somewhere between talking myself completely out of this and also thinking through it hard enough I want to try. 😑

ANNOYING