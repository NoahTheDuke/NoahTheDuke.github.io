{:title ""
 :date "2023-10-06T00:20:32.307Z"
 :tags ["cohost mirror"]
 :cohost-id 3082051
 :cohost-url "3082051-i-learned-a-surprisi"}

I learned a surprising fact about our app today and made a little slack poll to see about else knew it, and when i revealed the answer, the staff engineer on my team was like (very graciously), “you sure about that?” He didn’t have the answer for why it wasn’t true but he was steadfast that it wasn’t. Ominous lol

So I went back over every piece of information and learned that a single flag I didn’t know about before was transparently converting certain of our sql queries from eager to cursor mode, which batches the queries to limit the amount of data in memory at once.

I love to be publicly wrong lmao, just love it. At least I didn’t gloat too much before getting wrecked.