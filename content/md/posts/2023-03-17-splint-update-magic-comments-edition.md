{:title "splint update: magic comments edition"
 :date "2023-03-17T04:16:19.355Z"
 :tags ["cohost mirror" "clojure" "splint" "linter" "linters" "Fuck computers"]
 :cohost-url "1190796-splint-update-magic"}

I did it, nerds. I added support in [splint v0.1.119](https://cljdoc.org/d/io.github.noahtheduke/splint/0.1.119) for magic comments aka directives aka clj-kondo style `#_:splint/disable` or `#_{:splint/disable [lint/plus-one]}`. This means that folks can now pepper their code-base with stuff that has very limited utility! I'm very excited to see this become a piece of infrastructure when it certainly should not.

I'm up to 3300 lines of Clojure, which is fucking sick. This is maybe the most code I've ever written from scratch for a single project. I'd love for other people to contribute, but as of right now, only one person has tried, and they just fixed a typo in the readme.

If you use Clojure, please check this out, I would _love_ to get your feedback.
