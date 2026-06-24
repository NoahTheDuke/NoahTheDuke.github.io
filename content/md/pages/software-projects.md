{:title "Software Projects"}

## My Own Projects

### [Splint](https://cljdoc.org/d/io.github.noahtheduke/splint)

[Splint](https://cljdoc.org/d/io.github.noahtheduke/splint) is a Clojure linter focused on style and code shape. Inspired by [rubocop](https://rubocop.org/), I wrote this after being frustrated with contributing style lints to the primary Clojure linter [clj-kondo](https://github.com/clj-kondo/clj-kondo), which is more focused on static analysis. It's quite fast and fairly comprehensive, covering a vast number of style and "probably incorrect" linting rules, without performing the same in-depth analysis as clj-kondo.

### [Lazytest](https://cljdoc.org/d/io.github.noahtheduke/lazytest)

Alessandra Sierra wrote a successor to the built-in Clojure test "framework" `clojure.test` back in 2010, but quickly abandoned it. I came across it in 2023, found that while the code was half-formed, the foundational ideas were superb. [Lazytest](https://cljdoc.org/d/io.github.noahtheduke/lazytest) is the culmination of that work, a proper Clojure BDD test framework that supports first-class test suites, proper separation of assertions and reporting, extensions, and doctests.

### [cond+](https://tangled.org/noahbogart.com/cond-plus)

I learned Racket before I learned Clojure, and while I agree with most of the design decisions in Clojure, the changes to `cond` are among my least favorite. Because I missed them, I wrote [cond-plus](https://tangled.org/noahbogart.com/cond-plus) to let me continue to use Racket's `cond` in my Clojure code.

### [fluent-clj](https://github.com/NoahTheDuke/fluent-clj/)

[Project Fluent](https://projectfluent.org/) is a research project by Mozilla, attempting to build a new localization system that better supports translators and writing natural-sounding translations.

In my work on jnet, I spent a fair amount of time dealing with issues with the Clojure-based translation system we were using, growing increasingly frustrated by weird syntax errors and dodgy issues. Project Fluent is a natural (heh) fit for jnet as it allows us to keep our translations separated by file, give each translation a proper name, and pass in named arguments.

To support this, I wrote [fluent-clj](https://github.com/NoahTheDuke/fluent-clj/), a Clojure & Clojurescript wrapper around existing java and javascript libraries. `fluent-clj` unifies the APIs and includes a formatter, which helps keep translations neat. It is currently in use in jnet, serving 10 different languages and providing thousands of lines of translations.

## Open Source

### [Netrunner](https://github.com/mtgred/netrunner)

[jinteki.net](https://jinteki.net) (jnet) is an open source and free implementation of the asymmetric card game Netrunner (formerly Android: Netrunner). I joined the project in early 2018 when I was bored between jobs, learning Clojure to implement some new cards that had been released. That year, FFG cancelled the official game and the community-led [Null Signal Games](nullsignal.games) came together to continue working on it, and during that turmoil most of the jnet team stopped contributing. I became the lead dev, and worked on it extensively until the birth of my second kid in 2022, and now I am closer to a Dev Alumni, mostly reading PRs and contributing feedback to the cadre of devs who continue to work on it more actively.

### [fixture-riveter](https://github.com/Batterii/fixture-riveter)

Back in 2020, I was working at a typescript job that had a ton of orm-style objects. We had built a janky set of helper functions to create scenarios, but they were hard to manage and wrangle. In a moment of clarity, I remembered [factory_bot](https://github.com/thoughtbot/factory_bot) from my days at a Ruby On Rails job, and knew we could do similar. I spent probably too much time working on this, mirroring the `factory_bot` api and functionality closely, wrangling the typescript types to handle each variation and input (instead of finding a more typescript-natural api), and even solving a long-standing bug from the `factory_bot` library. Of all the things I contributed to that job, this is my favorite and the one I am most proud of. I no longer work there so I cannot speak to it nowadays but I look back on it fondly.
