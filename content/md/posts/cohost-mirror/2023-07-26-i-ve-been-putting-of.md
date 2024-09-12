{:title ""
 :date "2023-07-26T13:57:52.805Z"
 :tags ["cohost mirror" "splint" "clojure" "programming" "functional programming" "devblog" "Java"]
 :cohost-id 2212320
 :cohost-url "2212320-i-ve-been-putting-of"}

I’ve been putting off implementing a feature in Splint for months because I worried it would be annoying to write, but I needed it yesterday so I took a stab at it and it basically went as smoothly as I could hope.

The feature? Excluding files or file globs from being checked within specific rules or globally from inside the configuration file. Java’s java.nio.file.PathMatcher makes it really simple. I was worried I would have to implement it myself.

Lots of hoopla for nothing. Feels great to see it work.