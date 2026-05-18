default:
    @just --list

today := datetime("%Y-%m-%d")
now := datetime("%Y-%m-%dT%H:%M")
new_post_stem := join("content", "md", "posts", today)
new_page_stem := join("content", "md", "pages")

# Create a new post file
new-post *title:
    #!/usr/bin/env bash
    slug="$(./slugify {{title}})"
    touch {{new_post_stem}}-$slug.md
    echo -en "{:title \"{{title}}\"\n :date \"{{now}}\"\n :at-uri nil\n :tags []}" > {{new_post_stem}}-$(./slugify {{title}}).md

# Create a new "page" file
new-page *title:
    #!/usr/bin/env bash
    slug="$(./slugify {{title}})"
    touch {{new_page_stem}}/$slug.md
    echo -en "{:title \"{{title}}\"}\n" > {{new_page_stem}}/$slug.md

build:
    clojure -M:build

serve:
    clojure -X:serve

repl:
    clojure -M:repl/rebel
