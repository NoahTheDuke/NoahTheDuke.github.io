{:title ""
 :date "2024-07-26T20:31:32.058Z"
 :tags ["cohost mirror" "clojure" "ocaml" "writing ugly code on purpose because i miss s-expressions"]
 :cohost-id 7047993
 :cohost-url "7047993-i-know-it-s-not-the"}

I know it's not the style, but I like to think of Ocaml like a Clojure that has more syntax, and just wrap everything in parentheses. Every function call, every assignment, every if block, everything.

```ocaml
module Cli = Chatlib.Cli

let main () =
  (let options = (Cli.parse Sys.argv) in
    (Cli.OptionBag.pp options))

let () = (main ())
```