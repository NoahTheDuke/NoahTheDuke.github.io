{:title ""
 :date "2022-10-31T00:05:15.672Z"
 :tags ["cohost mirror" "clojure" "rant" "open source"]
 :cohost-id 161270
 :cohost-url "161270-i-commented-on-someo"}

I commented on someone else’s post about open source community norms and brought up Rich Hickey’s Open Source is Not About You. I said  that I admired the stance after initially thinking it rude. However,,,

I can’t help but wonder what Clojure could look like if it was less preciously held by those in charge. I don’t want Rust levels of development speed, that’s too much and too fast for my taste. But it dampens my enthusiasm for the language and my faith in the team when they respond with such disinterest in things that don’t materially impact them directly.

Sharp edges and places of friction are ignored or brushed away as unimportant, like stack traces or error messages in general. Bugs that have available patches can be left to bitrot, sometimes for years (concat stack overflow patch), and bugs without patches can be ignored or treated as too prevalent to fix (keyword regex, type hinting symbol vs vector in defn).

I like that they care about stability and have a core ideal of additive change, but at a certain point it feels closer to “once written, code cannot change” which I think leads to worse decision making over time. The bigger the stakes, the harder it is to make any decision.

I love Clojure and I want to continue to use it for the rest of my career; that’s why it hurts to see them choose to act like this. People like Ambrose Bonnaire-Seargent (typed clojure) shouldn’t feel like they have to write their own library to fix the mistakes in Clojure itself because the core team are too stuck in their ways to merge his patches.

Breaks my damn heart.