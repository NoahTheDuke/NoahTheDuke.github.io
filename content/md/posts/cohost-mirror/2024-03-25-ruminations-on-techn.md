{:title "Ruminations on technical debt in jnet (sent from my iPhone)"
 :date "2024-03-25T01:36:52.963Z"
 :tags ["cohost mirror" "jinteki.net" "netrunner" "android: netrunner" "programming" "venting" "i hate programming" "functional programming"]
 :cohost-id 5258703
 :cohost-url "5258703-ruminations-on-techn"}

<jinteki.net> has a lot of problems, some which are easy to solve and some which are not. There’s a difference between bugs and tech debt. Bugs are things like “misplaced parameters”, or “forgot to check an additional predicate”. Technical debt is “the core system is built on callbacks” or “there is no game loop”.

I had a wonderful conversation with someone who is attempting to implement the Netrunner rules as strictly as possible. (She’s gone so far as to implement Netrunner’s R&D location system instead of using a simple vector!) In our many back and forths, I am confident she learned nothing from me lol but I learned a lot from her, both about Netrunner and about how to structure a game engine for Netrunner.

jinteki.net is not a good game engine. It works, in part due to the incredible amount of work we’ve all put in over the years, but (to be slightly rude) it has no right to work as well as it should. It’s full of cludges, ad-hoc systems, layers of ideas half implemented on top of each other like dirt strata.

It also has complete implementations of over 2,000 individual cards, and is the primary means of netrunner play post covid. To attempt to start over is incredibly daunting. Not only am I bad at blank projects, but I’m also not sure that a clean slate would benefit nearly as much as working to improve what we already have.

That said, speaking with Lucy gave me excitement like I haven’t experienced about jnet in years, and has filled me with a desire to Make Things Better. For starters, treating “effects” as pure data that’s passed around to handle replacements and interrupts is great.

But if I were to add that, I’d have to make sure it cleanly applies to all TWO THOUSAND cards, in addition to the 20k lines of engine code. That’s fuckin annoying.

There’s no real goal here, just combo venting and giddy excitement.