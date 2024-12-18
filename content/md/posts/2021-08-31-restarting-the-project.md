{:title "Restarting the project"
 :date "2021-08-31T12:01"
 :tags ["clojure" "personal" "programming"]}

I started writing a little blog in the summer of 2017, when I had really hit a local maxima happiness. A year into my software career, feeling medium good about Avuity, feeling very good about May, it seemed right to talk about it, even privately. I'm very glad I wrote all those things down. It makes me quite happy to read [Marriage](/posts/2017-07-30-marriage/) and see the seeds of good decisions, even if the execution took a lot of work.

Why start blogging again now? Because in between writing code every day, I feel like I have *thoughts* and *reactions* to things I read and try. I read all of [Test-Driven Development](https://www.oreilly.com/library/view/test-driven-development/0321146530/) in a single day, and like [99 Bottles of OOP](https://sandimetz.com/99bottles) before it, I walked away with my brain on fire. I've begun reading [Domain-Driven Design](https://www.domainlanguage.com/ddd/), and I can feel it poking at my brain in the same way. I don't want to lose this feeling, nor do I want to lose the insights these books are providing.

It's very easy to grind through a tough problem (personally or professionally) and then move onto the next problem without 1) reviewing what I (we, in the context of teams) learned or what challenged me, or 2) putting any effort into practicing or preparing for the future. Mindfulness when learning and practicing goes *a long way* towards cementing valuable lessons, much in the same way adding and reviewing flash cards might. (I've had my time with Anki, I don't think I need to do that right now.)

Do I actually have anything interesting to say? Hard to tell. I would love to write about my experience with Clojure. Working in this language has been deeply formative in ways that I doubt I'll experience from working in Typescript every day.

In [Hell is Other REPLs](https://hyperthings.garden/posts/2021-06-20/hell-is-other-repls.html), Colin talks about strongly opinionated languages "policing whatever private understanding you might have about the *meaning* of your code". I love that statement, and it's a feeling I've had many times when working with Haskell or Rust. Clojure isn't included in Colin's list, and I'm not sure that it's in mine, but it has fundamentally altered the way I think about programming problems, through its steadfast resistant to mutation. As it turns out, my "private understanding" isn't as all-encompassing as I'd like it to be. I forget keys and function calls, I return the wrong piece of data, I don't have the capabilities to hold the whole program in my head. Clojure demands that I keep things locally knowable: data cannot be changed across the codebase, so if a given function has no return, it can be safely ignored *wherever* it is.

The Haskeller would say, "We have the same and more! Come bask in the glory of pure functional happiness where not even `atoms` can save you, and where strict typing will guide your every keystroke!" However, they miss the greatest strength of all: by eliding all of those pesky types, one must rely only on the base forms which in turn forces the programmer to use and design functions that only operate on those base forms. Strings, numbers, lists, vectors, maps, maybe the occasional set?

> [!NOTE]
> Yes, Clojure has `records` and one can dip a toe into the [Kingdom of Nouns](http://steve-yegge.blogspot.com/2006/03/execution-in-kingdom-of-nouns.html) by `:gen-class`-ing a Java class (ew), but the former is merely a formality on top of a map, and the latter is rightfully shunned by all good and true people.

I had a further point but was derailed by fucking around with the `NOTE:` block above.
