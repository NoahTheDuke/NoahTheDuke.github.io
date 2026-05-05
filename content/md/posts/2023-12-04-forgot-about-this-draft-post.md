{:title "Forgot about this draft post"
 :date "2023-12-04T22:55:10.872Z"
 :tags ["cohost mirror" "jinteki.net" "clojure" "devblog" "oops i forgot about this draft"]
 :cohost-url "3401833-forgot-about-this-dr"}

I wrote this draft months ago right before bed, planning on coming back to it in the morning. Now I don’t remember what my breakthrough was. Damn. The description below of jnet’s problems is well worth reading if you give a shit, but never resolves lol.

Original post:

# I’ve had a breakthrough about jinyeki.net’s engine

---

first, a description of the problems. There are many:

1. We handle all instructions that could have player input with nested callbacks. We use clojure macros to make it easier, but it’s fundamentally callback hell. Means we have to manually pass the callbacks around and have to verify every single branch calls the callback at the end (or passes it along). Also means we can’t queue up multiple instructions or handle multiple callbacks in a row, we have to write recursive functions to step over them one at a time.

2. We have no core game loop. Instead, each input from a player calls an action, an engine function which handles everything relevant to that input. This makes our action code messy and makes it hard to propagate updates to parts of the game state that aren’t directly touched by the given action.

3. Additionally, we don’t update values on cards by card abilities (X gains +1 Y) immediately, we create an object that says “x gains y” and then delegate the update to a loop that recalculates _everything_, all potential updated values. This is extremely costly, so we only do it after handling the whole action. This leads to inconsistency issues and having to force recalculate parts adhoc as situations arise.

4. The engine is not a state machine. It says “I’m in phase X”, but it doesn’t block players or functions from doing whatever most of the time. Means we have to work around any function being callable from anywhere else with little guarantees that the game will be in the right phase.
