{:title "Mild success!"
 :date "2023-08-07T18:14:33.563Z"
 :tags ["cohost mirror" "programming" "rust" "splint" "clojure"]
 :cohost-url "2400662-mild-success"}


**@noahtheduke** posted:
<div style="white-space: pre-line;">I spend a lot of time thinking about my linter’s performance, and I’ve changed some of the ways I program in Clojure to support that. But it also means that I’ve sacrificed some of the benefits of Clojure and I’m not always writing “idiomatic” Clojure.

I dream of moving to a language like Rust or Zig, one that pursues pure speed and efficiency, but I know Clojure and I’m intimately familiar with it, so it would be quite a loss of effort and time to just get back to where I am currently.

And for what? My linter runs fast enough! It can lint 120k lines of Clojure in 15 seconds. Is that Ruff speeds? No but it’s pretty dang good.

Buuuuut it could be faster, and that gnaws at me lol</div>
<hr>

**@noahtheduke** posted:
<div style="white-space: pre-line;">honestly, the big thing preventing me from actually trying this out is having to write a Clojure parser from scratch in whichever language I choose. Clojure's grammar is LL(1) I think, can be done with a fairly simple recursive-descent parser, but there's a bunch of small edge cases that make it kind of annoying (namespaced maps, for example), and then I have to come up with some representation of Clojure's data structures and use those everywhere.

Actually, every part of this is annoying lmao

---

cuz right now I have a pattern-matching macro that takes in Clojure forms and spits out "efficient" functions that perform the matches and bind variables, so I can just write `(defrule lint/if-not-both "docstring" {:pattern '(if (not ?x) ?y ?z) :message "Use `if-not` instead of recreating it." :replace '(if-not ?x ?y ?z)}` and the `defrule` macro will transform the `:pattern` into the efficient matching and binding function and `:replace` will transform into the efficient substitution function, and the mechanics inside are all just clojure functions saying stuff like `(if (map? form) ...)`.

To do this in Rust, for example, I'd have to write a pattern-matching function (def not a macro, fuck rust macros) that takes a string and parses it, and once we're using strings, so much of the niceties of the macro are lost. Maybe there's stuff to be gained, but really how much?

I'm somewhere between talking myself completely out of this and also thinking through it hard enough I want to try. 😑

ANNOYING</div>
<hr>

**@noahtheduke** posted:
<div style="white-space: pre-line;">Not 400 beats after writing that, while watching old episodes of Bob’s Burgers with my wife, I decided to try.

I updated my rust tool chain to latest, `cargo new clojure`, `nvim src/main.rs`, and then sat in wonder and astonishment and total silence for 20 minutes as I pondered how fundamentally I don’t know Rust.

I had this vague feeling that because I’d written a handful of small programs over the last 7 years and done two years of Advent of Code in Rust, I could just dive in, same as how I dive into new Clojure projects.

That is incorrect. I have completely forgotten everything I once knew about Rust lmao.

Use it or lose it, as the saying goes!</div>
<hr>


**@noahtheduke** posted:

I have spent every evening over the last week and a bit working on the parser, because a parser is a very good place to start when linting code.

My code is bad, I don't know why it works after not working in many other different configurations, the whole `mut` thing is magical and kind of dumb, but I have achieved some measure of victory!

### I can parse `clojure/core.clj` (8k lines) in 12 ms!

This is roughly 5x faster than [edamame](https://github.com/borkdude/edamame), the parser I'm using in Splint at the moment, and 6x faster than edamame with my additions (parsing ns-forms to auto-resolve `::` keywords or parsing `defn` forms to make linting easier). I suspect that even with the additions, I'll still be 3-5x faster.

That's fuckin sick. I feel fuckin sick for pulling this off. Now I'm done, right?

meme below the fold

---

me irl:

![Fancy "The" written by Spongebob with empty page](https://i.imgflip.com/3wh0d7.jpg)
