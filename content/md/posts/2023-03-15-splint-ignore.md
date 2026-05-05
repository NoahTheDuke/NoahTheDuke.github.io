{:title "splint/ignore"
 :date "2023-03-15T18:52:49.736Z"
 :tags ["cohost mirror" "clojure" "linters" "splint" "parsing" "ZA̡͊͠͝LGΌ ISͮ̂҉̯͈͕̹̘̱ TO͇̹̺ͅƝ̴ȳ̳ TH̘Ë͖́̉ ͠P̯͍̭O̚​N̐Y̡ H̸̡̪̯ͨ͊̽̅̾̎Ȩ̬̩̾͛ͪ̈́̀́͘ ̶̧̨̱̹̭̯ͧ̾ͬC̷̙̲̝͖ͭ̏ͥͮ͟Oͮ͏̮̪̝͍M̲̖͊̒ͪͩͬ̚̚͜Ȇ̴̟̟͙̞ͩ͌͝S̨̥̫͎̭ͯ̿̔̀ͅ" "regex"]
 :cohost-url "1183207-quick-follow-up-beca"}

Quick follow-up because I meant to post that last night and accidentally left it in my drafts.

I came up with the idea of parsing `#_:splint/ignore` the way clj-kondo does it: attaching it as metadata to the following form and then check for the metadata when rules checking. I modified my fork of edamame and it works great, but it's a bit more bulky now. Seems to add a very slight increase in parsing time (2-5ms) which is an acceptable increase.

I posted in #edamame on Clojurian slack about it, and Borkdude (maintainer of edamame and clj-kondo) recommended using `str/replace` to convert `#_:splint/ignore` to `^:splint/ignore`, which would remove the need for a fork while still attaching the metadata. It works, but it's much more error-prone; regex is a wild beast and cannot be contained.

I think I'm going to stick with my fork for now, but I can change my mind at any time relatively cheaply.
