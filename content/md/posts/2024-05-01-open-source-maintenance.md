{:title "open source maintenance"
 :date "2024-05-01T15:36:29.776Z"
 :tags ["cohost mirror" "just" "vim" "neovim" "open source" "open source software" "programming" "rust" "rust-lang"]
 :cohost-url "5793881-open-source-maintena"}

i am the maintainer of the vim syntax highlighting file for just, [vim-just](https://github.com/NoahTheDuke/vim-just), and while i did the heavy lifting to write up the initial version back in 2021, i haven't cared much about keeping up with the latest changes and have let others make changes as they do.

last year, someone offered to help out so i added them to the repo. i suspect it's their first time being a "contributor" on an open source project cuz they've spent the last year absolutely dedicated to the task of improving both the syntax highlighting and the bespoke syntax test runner we have that's written in rust. the project isn't that exciting and doesn't require that much effort but they've stayed up to date (to the day) of new syntaxes added to just and have been poking and prodding the test runner to make it faster and more idiomatic, including writing their own custom version of vim's built-in `:ToHtml` as it was removed in neovim. today, they mentioned "but we don't want code like this in production" which cracked me up because there's no "production" to be had here! doesn't matter, they give a shit and so i give a shit with them.

there's no real point here, it's just fun to see someone really dive in and own something. i know open source maintenance is a touchy subject after the xz/liblzma stuff, but i still think it's worthwhile to give people a chance to make their mark and spend their effort on cool things.
