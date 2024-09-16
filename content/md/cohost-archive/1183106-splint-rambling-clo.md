{:title "Splint rambling: Clojure Parsers"
 :date "2023-03-15T18:36:20.459Z"
 :tags ["cohost mirror" "clojure" "linters" "splint" "parsing"]
 :cohost-url "1183106-splint-rambling-clo"}

I want another parser because I want access to comments. Without comments, I can't parse magic comments, meaning I can't enable or disable rules inline, only globally. That's annoying and not ideal. However, every solution I've dreamed up has some deep issue.

---

All times below are about parsing the 8.5k lines in `clojure/core.clj`.

* [Edamame](https://github.com/borkdude/edamame) is our current parser and it's quite fast (40-60ms) but it drops comments. I've forked it to try to add them, but that would mean handling them in every other part of the parser, such as syntax-quote and maps and sets, making dealing with those objects really hard. :sob:

* [Rewrite-clj](https://github.com/clj-commons/rewrite-clj) is much slower (180ms) only exposes comments in the zip api, meaning I have to operate on the zipper objects with zipper functions (horrible and slow). It's nice to rely on Clojure built-ins instead of `(loop [zloc zloc] (z/next* ...))` nonsense.

* [parcera](https://github.com/carocad/parcera) looked promising, but the pre-processing in `parcera/ast` is slow (240ms) and operating on the Java directly is deeply cumbersome. The included grammar also makes some odd choices and I don't know ANTLR4 well enough to know how to fix them (such as including the `:` in keyword strings). And maybe this isn't such a big deal but matching against strings is annoying. Means I'm writing something other than Clojure code.
