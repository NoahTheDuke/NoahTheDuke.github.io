(ns switch
  (:require
   [babashka.fs :as fs]
   [clj-yaml.core :as yaml]
   [clojure.string :as str]))

(def files
  (fs/glob "." "*.md" {:recursive false}))

(doseq [f files
        :let [contents (slurp (str f))
              rdr (java.io.PushbackReader. (java.io.StringReader. contents))
              m (read rdr)]]
  (spit (str f)
    (str "---\n"
         (str/trim (yaml/generate-string m {:dumper-options
                                            {:flow-style :block
                                             :indent 4
                                             :indicator-indent 2}}))
         "\n---"
         (str/join "\n" (line-seq (java.io.BufferedReader. rdr))))))
