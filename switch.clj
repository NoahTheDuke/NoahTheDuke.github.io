(ns switch
  (:require
   [babashka.fs :as fs]
   [clj-yaml.core :as yaml]
   [clojure.string :as str]))

(def files
  (fs/glob "content/md" "**.md" {:recursive true}))

(doseq [f files
        :when (str/includes? (str f) "about.md")
        :let [contents (slurp (str f))
              rdr (java.io.PushbackReader. (java.io.StringReader. contents))
              ; m (read rdr)
              ]]
  (prn (yaml/parse-string contents))
  #_(when (map? m)
    (spit (str f)
      (format "---\n%s\n---\n\n%s\n" 
              (str/trim (yaml/generate-string m {:dumper-options
                                                 {:flow-style :block
                                                  :indent 4
                                                  :indicator-indent 2}}))
              (str/trim (str/join \newline (line-seq (java.io.BufferedReader. rdr))))))))
