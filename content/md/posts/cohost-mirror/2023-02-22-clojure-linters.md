{:title "Clojure Linters"
 :date "2023-02-22T04:35:02.665Z"
 :tags ["cohost mirror" "clojure" "linting"]
 :cohost-id 1054973
 :cohost-url "1054973-clojure-linters"}

Clojure’s best linter, [clj-kondo][1], is pretty dang great. It covers a lot of ground: checks syntax, provides light type checking, tracks variables and usage, an entire hook system for linting/reading custom macros statically (without code execution), etc. it’s quite extensive.

[1]: https://github.com/clj-kondo/clj-kondo

# However,,,

---

However, it’s written in incredibly imperative Clojure due to the nature of the domain problem, and for performance reasons, all of the lints it implements are written imperatively as well, exactly at the place where they should be checked, instead of being abstracted into a common interface.

Additionally, Borkdude, the maintainer, has found  clj-kondo in the annoying position of being popular, as it is the basis for many other community tools such as clojure-lsp. He must be conservative in what he introduces, conservative in what he deprecates or removes, and resistant to any big changes overall.

This makes modifying clj-kondo for any reason really tiresome lol. There are a number of ideas I have for making it better but after talking with Borkdude I don’t think they’ll be viable for all of the above reasons.

That’s why I’ve built my own linter. It’s in the very early stages, but I’m growing more and more fond of it.

That being said, I end every day exhausted beyond recognition, and can’t face an evening barely coherent at my computer, so progress is very very slow.