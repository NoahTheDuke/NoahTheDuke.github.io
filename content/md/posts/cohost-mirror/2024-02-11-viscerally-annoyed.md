{:title "Viscerally annoyed"
 :date "2024-02-11T20:51:23.007Z"
 :tags ["cohost mirror" "ocaml" "amgry" "functional programming" "programming"]
 :cohost-id 4445263
 :cohost-url "4445263-viscerally-annoyed"}

I’m trying to learn Ocaml. It’s going okay, learning new languages is hard, and learning new warts is the hardest part. I came across one that’s left me so viscerally annoyed that I might stop learning the language altogether.

I’m struggling to write a real post about this lol. I keep deleting my words.

I’m annoyed that argument order evaluation is deliberately unspecified in the language spec and the primary compiler evaluates arguments right to left.

“Annoyed” feels like it’s underselling the emotions I experience when I think about the implications of that decision. It violates a fundamental expectation I have about writing code: given that the text is written left to right (and thus top to bottom), the code will be evaluated left to right top to bottom.

Ocaml is an imperative programming language with fantastic functional programming features. It has built in mutable collections! It allows side effects freely! It has no ability to track or check those side effects! The language designers/compiler authors have no reason to assume that a given program is “pure”, free of side effects, and must assume that all programs are side effecting!

Can you imagine this logic being applied in other spaces? “We allow for reordering of sequential let bindings.” Or “we don’t guarantee that f(g(x)) will evaluate g(x) first.”

Get the fuck out of here.

And the justification is performance?????????