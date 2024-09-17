{:title "Splint updates! Oops it's been 8 months edition"
 :date "2024-02-20T17:18:30.600Z"
 :tags ["cohost mirror" "clojure" "splint" "programming" "functional programming" "devblog"]
 :cohost-url "4532528-splint-updates-oops"}

I forgot to post about the updates back in December, so I'm gonna write about everything from 1.11 through 1.14.

The biggest changes since last summer are a `global` top-level entry in `.splint.edn` that applies to all files, supporting the upcoming Clojure 1.12 interop syntax (`(^[] String/toUpperCase "noah")`), and simplifying the internal logic of the pattern DSL so that the short forms are expanded to their full forms before processing.

Paired with the `global` entry in `.splint.edn`, I implemented `:excludes`, which will skip specified files or directories in `global` or in specific rules. The paths can be specified with Java's [java.nio.file.FileSystem/getPathMatcher](https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-) globs or regexes, as well as `:re-find` and `:string`. [Details here.](https://cljdoc.org/d/io.github.noahtheduke/splint/1.14.0/doc/configuration#excluding-files)

Additionally, there are performance increases to roughly 20 existing rules. I went through all of the rules that used `:on-match` in combination with `when` to move those checks into the DSL, avoiding the costly aggregation of star args (`?*`) and forcing early exits where possible. I also simplified the `?|` matcher so it's much much faster. I'd love to be able to speed up `?*` and `?+`, but those are gonna be harder to optimize given the reliance on continuations.

My only thought is that I could check to see if a given `?*` or `?+` pattern is the final pattern in a given sequence, and if so, not use continuations but some sort of slice? Like how I did before switching to continuations last summer.

Full change logs below the fold.

---


## v1.11 - 2023-12-11

### New Rules

- `lint/underscore-in-namespace`: Prefer `(ns foo-bar)` to `(ns foo_bar)`.
- `performance/dot-equals`: Prefer `(.equals "foo" bar)` to `(= "foo" bar)`. Only cares about string literals right now.
- `performance/single-literal-merge`: Prefer `(assoc m :a 1 :b 2)` to `(merge m {:a 1 :b 2})`. Has `:single` and `:multiple` styles, either a single `assoc` call or threaded multiple calls.
- `performance/into-transducer`: Prefer `(into [] (map f) coll)` to `(into [] (map f coll))`.
- `style/trivial-for`: Prefer `(map f items)` over `(for [item items] (f item))`.
- `style/reduce-str`: Prefer `(clojure.string/join coll)` over `(reduce str coll)`.

### Added

- `global` top-level `.splint.edn` config that applies to all rules.
- Support `:excludes` in both `global` and rules-specific configs. Accepts a vector of [java.nio.file.FileSystem/getPathMatcher](https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-) globs or regexes. When in `global`, matching files are removed from being processed at all. When in a specific rule, the rule is disabled before matching files are checked.

### Changed

- Added `min`, `max`, and `distinct?` to `lint/redundant-call`.
- Change `style/set-literal-as-fn` default to `false`. It's not idiomatic and I don't know that it's any faster either.

### Fixed

- Project file now accepts all reader macros.
- `--auto-gen-config`, adds test.

## v1.12 - 2024-02-09

### Added

- `re-find:` and `string:` syntaxes for path `:excludes`. `re-find` uses `clojure.core/re-find`, so the regex doesn't have to match the entire file path, just any portion. `string` uses `clojure.string/includes?`, so a fixed string anywhere in the file path.

### Changed

- Updated [edamame][edamame] to v1.4.25 in support of the new Clojure 1.12 `^[]`/`:param-tags` feature.

## v1.13 - 2024-02-14

### New Rules

- `lint/prefer-method-values`: Prefer `(^[] String/toUpperCase "noah")` to `(.toUpperCase "noah")`.
- `lint/require-explicit-param-tags`: Prefer `(^[File] File/mkdir (io/file \"a\"))` to `(File/mkdir (io/file \"a\"))`. Prefer `(^[String String] File/createTempFile \"abc\" \"b\")` to `(^[_ _] File/createTempFile \"abc\" \"b\")`. Has `:missing`, `:wildcard`, and `:both` styles, which check for lack of any `:param-tags`, usage of `_` in a `:param-tags`, and both. Defaults to `:wildcard`.

### Changed

- Add support for `lint/prefer-method-values` in `performance/dot-equals`.
- Switch `new_rule.tmpl` to use `deftest`. `defexpect` is a thin wrapper and has the annoying "if given two non-expect entries, wrap in expect", which doesn't work when we use custom expect macros.

## v1.14.0 - 2024-02-19

### Changed

- General performance increases in rules:

  - `lint/body-unquote-splicing`
  - `lint/if-else-nil`
  - `lint/underscore-in-namespace`
  - `lint/warn-on-reflection`
  - `metrics/parameter-count`
  - `naming/conversion-function`
  - `naming/predicate`
  - `naming/record-name`
  - `naming/single-segment-namespace`
  - `style/def-fn`
  - `style/eq-zero`
  - `style/prefer-clj-string`
  - `style/prefer-condp`
  - `style/reduce-str`
  - `style/single-key-in`
  - `style/tostring`
  - `style/useless-do`

- Remove documentation about `?_` short form, as it's covered by the existing `?` and `_` binding rules.
- Expand `?foo` short-forms in patterns to their `(? foo)` special form. Simplifies matching functions, makes the pattern DSL more consistent. Now `?|foo` will throw immediately instead of part-way through macroexpansion.
- Updated pattern docs with a small example at the top.
- Simplified `?|` matcher logic to use a set, as that's faster than creating multiple `read-form` patterns in a let block and checking each one.

### Fixed

- Correctly suggest `Obj/staticMethod` when given `(. Obj (staticMethod))` in `lint/dot-class-usage`.
- Only suggest `naming/conversion-functions` when there's no `-` in the part before `-to-`. (Will warn on `f-to-g`, will not warn on `expect-f-to-c`.)
- Correctly render args in `lint/assoc-fn`.
