{:title ""
 :date "2024-03-28T05:29:31.376Z"
 :tags ["cohost mirror" "clojure" "functional programming" "jinteki.net" "jnet" "netrunner" "android: netrunner" "devblog" "thank you drugs" "thrugs"]
 :cohost-url "5308825-https-github-com-m"}

https://github.com/mtgred/netrunner/pull/7346

this shit is another hefty one. i rewrote the primary data structure for costs. will be much simpler to track costs, add modifiers later, keep normal and additional costs distinct, and intelligently handle stealth costs.

once this is done, i'm gonna implement system-wide additional cost checking, allowing you to reject paying for any and all additional costs.

once i'm done with this, i'm not sure what i'm gonna do next. maybe rejigger prompts like i've been dreaming of for 6 years.
