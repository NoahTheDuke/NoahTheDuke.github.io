{:title "splint reaches 1.0"
 :date "2023-03-22T16:27:07.412Z"
 :tags ["cohost mirror" "splint" "linters" "clojure" "linter" "programming" "devblog"]
 :cohost-id 1216432
 :cohost-url "1216432-splint-reaches-1-0"}

i don't like waiting around for version 1.0. [0ver](https://0ver.org/) is funny but annoying. BE NOT AFRAID. so i've released [splint 1.0](https://cljdoc.org/d/io.github.noahtheduke/splint/1.0.1/doc/home).

it has disabling rules, it doesn't choke on any code i've thrown at it so far, and the set of rules it has is pretty great. there's loads more work to be done, such as moving most of the "lint" rules to "style", which will invariably break folks config lol, but why wait?

the specific updates this time are:

* Add `--parallel` and `--no-parallel` to support running it single-threaded, defaults to true.
* `style/def-fn`: Prefer `(let [z f] (defn x [y] (z y)))` over `(def x (let [z f] (fn [y] (z y))))`. (Based on the clj-kondo linter.)
* `lint/try-splicing`: Prefer `(try (do ~@body) (finally ...))` over `(try ~@body (finally ...))`.
* `lint/body-unquote-splicing`: Prefer `(binding [max mymax] (let [res# (do ~@body)] res#))` over `(binding [max mymax] ~@body)`.