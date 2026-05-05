{:title "splint release: autocorrection bby"
 :date "2024-09-08T20:15:07.420Z"
 :tags ["cohost mirror" "clojure" "splint" "devblog" "programming" "functional programming"]
 :cohost-url "7599350-splint-release-auto"}

**Big feature:** Safety and Autocorrection

Every rule has been marked as safe or unsafe. Safe rules don't generate false positives and any suggested alternatives can be used directly. Unsafe rules may generate false positives or their suggested alternatives may contain errors.

Rules that are safe may also perform autocorrection, which is tracked in `defrule` with `:autocorrect`. Rules may only perform autocorrection if they're safe.

This release is supposed to also support interactively choosing which forms to autocorrect, but I forgot to add the cli flag, lmao. I'll cut a point release to include that tomorrow.

Other changes:

* Support Clojure 1.12
* Fix style/redundant-str-call and style/redundant-call to handle built-in -> and other similar forms.
