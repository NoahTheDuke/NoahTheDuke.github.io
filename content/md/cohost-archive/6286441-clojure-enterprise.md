{:title "Clojure: Enterprise Edition reply"
 :date "2024-06-05T19:18:59.939Z"
 :tags ["cohost mirror" "clojure" "dynamic typing" "lisp"]
 :cohost-url "6286441-clojure-enterprise"}


**@chreke** posted:
<div style="white-space: pre-line;">“Records” in dynamically typed languages keeps being a source of vexation for me. For example, I really like the idea of Lisp systems, but the fact that Clojure (and several other dynamically typed programming languages) default to representing domain objects as maps freaks me out a little bit.

To be fair, I’ve never worked in a big Lisp code base—my experience is only limited to experiments—but I have worked in large Erlang code bases with lots of business logic, where I would often run into issues with the lack of types for domain objects.

To illustrate my point, consider the following example—you’re asked to extend the following pseudocode to send an email after a user has finished their registration:

```
(def register-user [user]
  (if (valid-password? (:password user)
      (save-to-db user)
  ...)
```

The question is, what is `user`? It looks like it will be a map that contains a `:password` key, but does it also have an email address? If so, is it under the key `:email` or `:email-address`? Is the email a string or is it some kind of “email object”? If there is an email address, is it mandatory? Has it been validated in a previous step or do we need to go back and add validation somewhere? 

Clojure / Lisp devs, how do you deal with this? `spec`? Docs? I know "the Lisp way" is to do interactive and iterative development, but at some point I would assume you end up in a situation where you can't look at what's inside the `user` variable. (Or maybe this isn't a real problem and I'm just too ML-pilled)</div>
<hr>


**@noahtheduke** posted:

(I wrote the below in a [comment on the original post](https://cohost.org/chreke/post/6283639-clojure-enterprise#comment-99e8e159-2d3d-40e6-9486-de21cabea332))

I work in enterprise and hobby Clojure, so I have some experience with this question.

This is probably the primary issue with Clojure at scale. You have to either be strict in what kind of objects you create (peer review, extensive testing), or you mimic a type system in the runtime with assertions and validation checks. Both of these can be cumbersome and lead to issues which are hard to notice ahead of time and harder to debug.

However! These issues just aren't that big of a deal.

Most domain objects are bounded and known by developers, they have common keys. Your example of a `user` object might seem opaque because you're looking at a single function, but if you look at the codebase as a whole, you'll see what the other keys are. But also, take a step back and ask yourself, why do you need to know what the other keys are? How does it matter at this moment? If you don't need them right now, then `user` here really is just a map that has the key `:password` and that's good enough. This will let you write tests that simply call `(register-user {:password "hello123"})` instead of creating a whole test.

However, if you do need a whole "user" object and want to have some sort of documentation, there are a couple other solutions:
1) You can reach for [defrecord](https://clojuredocs.org/clojure.core/defrecord) which will act like a regular map but has defined keys (in addition to other niceties). That doesn't help you in the instance where your function takes a `user` with no docstring, unless you know to look for a `(defrecord User [first-name last-name user-name password])` in some other file, but it is generally helpful and once you know, you're not going to forget.
2) You can write `assert` calls yourself, using `defn`'s built-in `:pre` and `:post` assertion metadata to make sure you have the correct types or shapes when executing a given function.
3) You can use one of the many validation libraries such as spec or [malli](https://github.com/metosin/malli/) (my preferred library) or [Schema](https://github.com/plumatic/schema). These allow you to define the shape of your data and then either manually call the validation function (eg `(assert (s/valid? ::user user) "expected a user")`) or instrument a given function (check inputs/outputs at runtime before passing to the original function).

Coming from a static type background, it's easy to reach for these cuz they do provide a lot of the same features as static type systems except at run time, but all except records are slower and I think you'd be surprised how infrequently there are type or data issues in Clojure programs.

Because Clojure is built on a mindset of "many functions that operate on few types", you rarely run into the issue of performing an operation on the wrong type. They do happen, but it's not like calling `.append()` when you meant to call `.push()` because one is a vector and the other is a linked list. So here, you have a `user` and need to operate on it. You use `assoc`, `update`, `dissoc`, `get`, etc the same as if you had an `organization` or a `chat-room` or a `game-state`. All of the verbs are the same because they're all maps. Vectors (when given integer keys) work the same way: `(assoc [0 1 2] 1 100)` returns `[0 100 2]`. If you have invariants, you can write custom functions like `update-password` which will perform validation, but generally you'll instead write `(defn check-password [old-password new-password] ...)` and then write `(update user :password check-password new-password)` which still relies on the basic verb for the user object and performs the check locally to the specific data.

There's a common refrain/meme in the Clojure community about pushing side-effecting code to the boundaries, and there you can use validation libraries, but that your "business logic" should be pure functions which can be tested with unit tests or generative tests or even just in your large-scale integration tests because you wrote it at the repl and verified as you wrote it that everything worked correctly and can trust that it won't change out from under you (if the input is immutable, then other parts of the code can't touch a given function).

In [jinteki.net's codebase](https://github.com/mtgred/netrunner/), we don't use any validation libraries and perform almost no validation at run-time. We have a couple records to define the core objects and then don't worry about it. A `card` is a `card` is a `card` so there's never any confusion about what a `card` is or likewise what a `player` is or even what `state` has on it at any given point. And yet, we rarely have type errors or NPEs, mostly just horrible logic bugs from years of tech debt and poor technical decisions long ago (but that's a rant for another day lol). Even when I've performed [+9136 -7965](https://github.com/mtgred/netrunner/pull/5288) PRs, they went off mostly without a hitch.

At my job, we use json schema (legacy code) and malli (new code) for validating our endpoints, and I've introduced malli for some domain objects too, due to the sheer size of the code base. It's not caught on super well because for the most part, it's not needed. Our domain objects are relatively simple, our names are consistent, docstrings say what they need or provide, and our massive integration test suite -- while slow -- covers all of the ground one could hope for.

The places where it matters are the files that have 50+ functions, each expecting a different custom object ("I need to merge and pivot these two custom objects, so i'll write a new function and have it return a third custom object, but i won't write any docstrings or comments. i'm a genius") and tangled together. Adding some malli schemas and then even just using those in docstrings removes 90% of the issues.

I like Typescript and I've liked the little bits of Ocaml that I've tried (but fucking up polymorphic equality is such a mistake that I just can't keep it up lol), and I really like when there's head-of-time checking for object validity. TS is killer in that regard. It's a bummer that Typed Clojure never caught on or got good enough. That being said, I don't miss it much.
