{:title "Mild success!"
 :date "2023-08-07T18:14:33.563Z"
 :tags ["cohost mirror" "programming" "rust" "splint" "clojure"]
 :cohost-url "2400662-mild-success"}

I have spent every evening over the last week and a bit working on the parser, because a parser is a very good place to start when linting code.

My code is bad, I don't know why it works after not working in many other different configurations, the whole `mut` thing is magical and kind of dumb, but I have achieved some measure of victory!

### I can parse `clojure/core.clj` (8k lines) in 12 ms!

This is roughly 5x faster than [edamame](https://github.com/borkdude/edamame), the parser I'm using in Splint at the moment, and 6x faster than edamame with my additions (parsing ns-forms to auto-resolve `::` keywords or parsing `defn` forms to make linting easier). I suspect that even with the additions, I'll still be 3-5x faster.

That's fuckin sick. I feel fuckin sick for pulling this off. Now I'm done, right?

meme below the fold

---

me irl:

![Fancy "The" written by Spongebob with empty page](https://i.imgflip.com/3wh0d7.jpg)
