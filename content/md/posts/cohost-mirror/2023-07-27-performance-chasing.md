{:title "Performance Chasing"
 :date "2023-07-27T21:00:36.056Z"
 :tags ["cohost mirror" "programming" "rust" "rust-lang" "Rewrite it in Rust!" "Zig" "clojure"]
 :cohost-id 2235168
 :cohost-url "2235168-performance-chasing"}

I spend a lot of time thinking about my linter’s performance, and I’ve changed some of the ways I program in Clojure to support that. But it also means that I’ve sacrificed some of the benefits of Clojure and I’m not always writing “idiomatic” Clojure.

I dream of moving to a language like Rust or Zig, one that pursues pure speed and efficiency, but I know Clojure and I’m intimately familiar with it, so it would be quite a loss of effort and time to just get back to where I am currently.

And for what? My linter runs fast enough! It can lint 120k lines of Clojure in 15 seconds. Is that Ruff speeds? No but it’s pretty dang good.

Buuuuut it could be faster, and that gnaws at me lol