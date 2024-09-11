(ns convert
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [shell process exec]]
   [clojure.java.io :as io]))

(def files (fs/glob "content" "**.adoc"))

(doseq [f files
        :let [base (fs/strip-ext f)]]
  (shell "asciidoc" "-b" "docbook" (str f))
  (shell "pandoc" "-f" "docbook" "-t" "markdown_strict" (str base ".xml") "-o" (str base ".md"))
  (fs/move (str base ".md") (io/file "content" "md" "posts") {:replace-existing true})
  (shell "rm" (str base ".xml"))
  )
