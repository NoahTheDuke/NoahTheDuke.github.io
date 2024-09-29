default:
    @just --list

today := datetime("%Y-%m-%d")
now := datetime("%Y-%m-%dT%H:%M")
new_post_stem := join("content", "md", "posts", today)

# Create a new post file, open it in neovim
new-post *title:
    #!/usr/bin/env bash
    slugify () {
        echo "$@" | iconv -c -t ascii//TRANSLIT | sed -E 's/[~^]+//g' | sed -E 's/[^a-zA-Z0-9]+/-/g' | sed -E 's/^-+|-+$//g' | tr A-Z a-z
    }
    touch {{new_post_stem}}-$(slugify {{title}}).md
    echo -en "{:title \"{{title}}\"\n :date \"{{now}}\"\n :tags []}" > {{new_post_stem}}-$(slugify {{title}}).md
