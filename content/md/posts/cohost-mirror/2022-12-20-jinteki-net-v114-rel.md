{:title "jinteki.net v114 release notes and thoughts"
 :date "2022-12-20T21:12:01.915Z"
 :tags ["cohost mirror" "jinteki.net" "jnet" "netrunner" "clojure"]
 :cohost-id 666939
 :cohost-url "666939-jinteki-net-v114-rel"}

In [release v114](https://github.com/mtgred/netrunner/releases/tag/114), Francesco Pellegrini fixed a whole mess of bugs, added `set-mark` command, and added some tests to cover bug reports we couldn't reproduce. I added compiling css and cljs in Github Actions, to catch basic compiler errors.

## Release Notes

```
    Compile css and cljs in github runner by @NoahTheDuke in #6822
    fix Riot Suppression damage not being a cost by @francescopellegrini in #6829
    fix Thule Subsea :agenda-stolen cost by @francescopellegrini in #6828
    fix Deep Dive's second access cost not being :click by @francescopellegrini in #6836
    fix Adjusted Matrix's ability cost to :lose-click by @francescopellegrini in #6835
    Fix ice abilities not being a cost by @francescopellegrini in #6834
    fix Hostile Architecture missing :once :per-turn by @francescopellegrini in #6839
    Bump minimatch from 3.0.4 to 3.1.2 by @dependabot in #6823
    Bump decode-uri-component from 0.2.0 to 0.2.2 by @dependabot in #6744
    Fix Dedicated Technician Team test by @francescopellegrini in #6841
    add set-mark command by @francescopellegrini in #6842
    add missing :waiting-prompt on draw subs by @francescopellegrini in #6847
    fix Hafrún icon missing faction color by @francescopellegrini in #6846
    add test for Thunder Art Gallery on 0 credits by @francescopellegrini in #6850
    fix World Tree interaction with facedown cards by @francescopellegrini in #6849
    fix Meridian infinite approach when added to score area by @francescopellegrini in #6843
```

---

## Thoughts for the future

In the past, I've asked the community for areas that jnet doesn't cover ([Engine shortcomings](https://github.com/mtgred/netrunner/issues/6497)) or tracked pie-in-the-sky ideas in the github wiki ([BHAGs](https://github.com/mtgred/netrunner/wiki/BHAGs)). Both of these are good, but they're hidden away. I want to talk about things I am planning or working on for the future because it feels good and because I think talking about them will help me crystalize what needs to be done.

Every so often I get a bee in my bonnet about one aspect or another and I try to fully finish it before I run out of attention or energy or time, but it can be challenging.

I think my next goal is to write some clj-kondo hooks or maybe write a parser/compiler for ability maps. They're a mess and require paying attention to so many things. It's exhausting. I want to tear all this shit apart but it always takes time.

Okay, enough ranting for now.