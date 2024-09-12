{:title "splint update"
 :date "2023-02-28T04:10:16.636Z"
 :tags ["cohost mirror" "clojure" "linting" "devlog"]
 :cohost-id 1103922
 :cohost-url "1103922-splint-update"}

I’ve named my new clojure linter splint, a portmanteau of spat and lint. Spat is the name of the s-expression pattern matching/regex library I wrote, the name also a portmanteau from s-expression and pattern.

Why another linter? Because I am dissatisfied with the current crop of them.

---

I voiced my issues with clj-kondo last time so I won’t re-litigate them. Kibit is incredibly, unusably slow, taking roughly 45 minutes to lint 60k lines of code. Eastwood is too clever for its own good and not actually extensive enough to be greatly useful.

My goal is to write something that most anyone can contribute to, that runs “fast enough”, and that covers _a lot_ of ground.

Like I did nearly three years ago with fixture-riveter, I’m once again taking heavy inspiration from an existing Ruby library/framework. This time, Rubocop. I love how cops are written, I love the documentation, I love the simplicity of the configuration, and I love the massive scale of available lints.

What does this look like for me and for Splint then? Not much at the moment. I’m still missing some core functionality (such as configuration), I only have ~60 rules, and I can’t actually write certain compelling and interesting rules yet.

But I do have a powerful and interesting pattern matching library, stand alone rules that are easy to write and easy to read, and a runner that can process those same 60k lines of code in about 10 seconds. That feels pretty good to me so far.

Hopefully, I can finish up this primary work soon and see how the greater community takes to it.

Small example of how a rule is defined:

```clojure
(defrule let-if
  "`if-let` exists so use it. Suggestions can be wrong as there's no code-walking to
  determine if `result` binding is used in falsy branch.

Examples:

# bad
  (let [result (some-func)] (if result (do-stuff result) (other-stuff)))

# good
  (if-let [result (some-func)] (do-stuff result) (other-stuff))
  "
  {:pattern '(let [?result ?given] (if ?result ?truthy ?falsy))
   :message "Use the built-in function instead of recreating it."
   :replace '(if-let [?result ?given] ?truthy ?falsy)})
```