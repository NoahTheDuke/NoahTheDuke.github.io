{:title ""
 :date "2023-03-15T18:52:49.736Z"
 :tags ["cohost mirror" "clojure" "linters" "splint" "parsing" "ZA̡͊͠͝LGΌ ISͮ̂҉̯͈͕̹̘̱ TO͇̹̺ͅƝ̴ȳ̳ TH̘Ë͖́̉ ͠P̯͍̭O̚​N̐Y̡ H̸̡̪̯ͨ͊̽̅̾̎Ȩ̬̩̾͛ͪ̈́̀́͘ ̶̧̨̱̹̭̯ͧ̾ͬC̷̙̲̝͖ͭ̏ͥͮ͟Oͮ͏̮̪̝͍M̲̖͊̒ͪͩͬ̚̚͜Ȇ̴̟̟͙̞ͩ͌͝S̨̥̫͎̭ͯ̿̔̀ͅ" "regex"]
 :cohost-url "1183207-quick-follow-up-beca"}


**@noahtheduke** posted:
<div style="white-space: pre-line;">I want another parser because I want access to comments. Without comments, I can't parse magic comments, meaning I can't enable or disable rules inline, only globally. That's annoying and not ideal. However, every solution I've dreamed up has some deep issue.

---

All times below are about parsing the 8.5k lines in `clojure/core.clj`.

* [Edamame](https://github.com/borkdude/edamame) is our current parser and it's quite fast (40-60ms) but it drops comments. I've forked it to try to add them, but that would mean handling them in every other part of the parser, such as syntax-quote and maps and sets, making dealing with those objects really hard. :sob:

* [Rewrite-clj](https://github.com/clj-commons/rewrite-clj) is much slower (180ms) only exposes comments in the zip api, meaning I have to operate on the zipper objects with zipper functions (horrible and slow). It's nice to rely on Clojure built-ins instead of `(loop [zloc zloc] (z/next* ...))` nonsense.

* [parcera](https://github.com/carocad/parcera) looked promising, but the pre-processing in `parcera/ast` is slow (240ms) and operating on the Java directly is deeply cumbersome. The included grammar also makes some odd choices and I don't know ANTLR4 well enough to know how to fix them (such as including the `:` in keyword strings). And maybe this isn't such a big deal but matching against strings is annoying. Means I'm writing something other than Clojure code.</div>
<hr>


**@noahtheduke** posted:

Quick follow-up because I meant to post that last night and accidentally left it in my drafts.

I came up with the idea of parsing `#_:splint/ignore` the way clj-kondo does it: attaching it as metadata to the following form and then check for the metadata when rules checking. I modified my fork of edamame and it works great, but it's a bit more bulky now. Seems to add a very slight increase in parsing time (2-5ms) which is an acceptable increase.

I posted in #edamame on Clojurian slack about it, and Borkdude (maintainer of edamame and clj-kondo) recommended using `str/replace` to convert `#_:splint/ignore` to `^:splint/ignore`, which would remove the need for a fork while still attaching the metadata. It works, but it's much more error-prone; regex is a wild beast and cannot be contained.

I think I'm going to stick with my fork for now, but I can change my mind at any time relatively cheaply.
