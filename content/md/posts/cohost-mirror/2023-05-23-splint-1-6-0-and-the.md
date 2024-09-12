{:title "splint 1.6.0 and then of course 1.6.1 lol"
 :date "2023-05-23T20:10:28.197Z"
 :tags ["cohost mirror" "splint" "clojure" "linter" "linters" "devblog" "functional programming"]
 :cohost-id 1545480
 :cohost-url "1545480-splint-1-6-0-and-the"}

Small bug fix this time:

* Fix merging defaults for `--parallel` and `--output` so they can be set by `.splint.edn` local config.
* Also wrote short descriptions for all empty config.edn rule descriptions.

I of course fucked up the adhoc deploy script I built, so when I pushed 1.6.0, I didn't actually tag it and didn't commit any of the changes and really just messed it all up, so I cut another release immediately. I wish I could just write the code and will it onto my user's computers.