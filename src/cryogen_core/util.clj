(ns cryogen-core.util
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [hiccup.core :as hiccup]
   [net.cgrand.enlive-html :as enlive]
   [clygments.core :as clygments]
   [cryogen-core.flexmark :as flexmark]
   [cryogen-core.markup :refer [render-fn]])
  (:import
   (java.util.regex Pattern)))

(defn filter-html-elems
  "Recursively walks a sequence of enlive-style html elements depth first
  and returns a flat sequence of the elements where (pred elem)"
  [pred html-elems]
  (reduce (fn [acc {:keys [content] :as elem}]
            (into (if (pred elem) (conj acc elem) acc)
                  (filter-html-elems pred content)))
          [] html-elems))

(defn conj-some
  "Like conj, but ignores xs that are nil."
  ([] [])
  ([coll] coll)
  ([coll & xs]
   (apply conj coll (remove nil? xs))))

(defn enlive->hiccup
  "Transform enlive style html to hiccup.
  Can take (and return) either a node or a sequence of nodes."
  [node-or-nodes]
  (cond (string? node-or-nodes) node-or-nodes
        (empty? node-or-nodes) nil
        (sequential? node-or-nodes) (map enlive->hiccup node-or-nodes)
        :else (let [{:keys [tag attrs content]} node-or-nodes]
                (conj-some [tag] (not-empty attrs) (enlive->hiccup content)))))

(defn enlive->html-text [node-or-nodes]
  (->> node-or-nodes
       (enlive/emit*)
       (str/join)))

(defn enlive->plain-text [node-or-nodes]
  (->> node-or-nodes
       (enlive/texts)
       (str/join)))

(defn highlight-code-block [node]
  (let [n (first (:content node))]
    (if (and (= 1 (count (:content node)))
             (= :code (:tag n))
             (-> n :attrs :class))
      (let [klass (-> n :attrs :class)
            lang (keyword klass)
            content (-> (:content n)
                        (first)
                        (clygments/highlight lang :html))
            code (-> (enlive/html-snippet content)
                     (first)
                     :content)]
        {:tag :code
         :attrs {:class (str klass " highlight")
                 :data-lang klass}
         :content code})
      node)))

(defn make-alert [a-type body-contents]
  (first
   (enlive/html
    (into [:div {:class (str "markdown-alert markdown-alert-" (str/lower-case a-type))}
           [:p {:class "markdown-alert-title"}
            (let [icon (str "alert-" (str/lower-case a-type))]
              [:svg {:class (str "alert " icon)
                     :width "16"
                     :height "16"}
               [:use {:xlink:href (str "/img/icons.svg#" icon)}]])
            (str/capitalize a-type)]]
          body-contents))))

(defn alert-block [node]
  (let [block-content (:content node)]
    (if (= :p (:tag (first block-content)))
      (let [quote-str (-> block-content first :content first)]
        (if (string? quote-str)
          (let [[_full a-type] (re-find #"\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]\n" quote-str)
                quote-str (str/replace quote-str #"\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]\n" "")]
            (if a-type
              (make-alert a-type (-> (vec block-content)
                                     (update-in [0 :content] #(->> (next %)
                                                                   (cons quote-str)
                                                                   (vec)))))
              node))
          node))
      node)))

(defn trimmed-html-snippet
  "Creates an enlive-snippet from `html-string` then removes
  the newline nodes"
  [html-string]
  (->> (enlive/html-snippet html-string)
       (walk/postwalk
        (fn [node]
          (cond
            ; ;; empty nodes
            (seq? node)
            (remove #(and (string? %) (re-matches #"\n\h*" %)) node)
            (= :pre (:tag node))
            (highlight-code-block node)
            (= :blockquote (:tag node))
            (alert-block node)
            #_node
            :else
            node)))))

(comment
  (let [render (render-fn (flexmark/markdown))]
    (trimmed-html-snippet (render
                           (java.io.StringReader. "
> [!NOTE]
> EDIT: I WROTE ALL THIS FUCKING SHIT AND FORGOT TO CREDIT THE AUTHOR OF MY JAVASCRIPT CODE! FUCK MY STUPID LIFE! I'M SORRY JONATHAN!") nil))))

(defn hic=
  "Tests whether the xs are equivalent hiccup."
  [x & xs]
  (apply = (map #(hiccup/html %) (cons x xs))))

(defn file->url
  "Converts a java.io.File to a java.net.URL."
  [^java.io.File f]
  (.. f (getAbsoluteFile) (toURI) (toURL)))

(defn parse-post-date
  "Parses the post date from the post's file name and returns the corresponding java date object"
  ;; moved from cryogen-core.compile
  [^String file-name date-fmt]
  (let [fmt (java.text.SimpleDateFormat. date-fmt)
        c (count date-fmt)]
    (try (if-let [last-slash (str/last-index-of file-name "/")]
      (.parse fmt (.substring file-name (inc last-slash) (+ last-slash (inc c))))
      (.parse fmt (.substring file-name 0 c)))
         (catch Exception e
           (throw (ex-info "Failed to parse date from filename"
                          {:date-fmt date-fmt
                           :file-name file-name}
                          e))))))

(defn re-pattern-from-exts
  "Creates a properly quoted regex pattern for the given file extensions"
  ;; moved from cryogen-core.compiler
  [exts]
  (re-pattern (str "(" (str/join "|" (map #(str/replace % "." "\\.") exts)) ")$")))

(defn white [s] (str "\033[37m" s "\033[0m"))
(defn yellow [s] (str "\033[33m" s "\033[0m"))
(defn default [s] (str "\033[39m" s "\033[0m"))
(defn bg-red [s] (str "\033[41m" s "\033[0m"))
(defn bg-cyan [s] (str "\033[46m" s "\033[0m"))
(defn green [s] (str "\033[32m" s "\033[0m"))
(defn bg-green [s] (str "\033[42m" s "\033[0m"))
(defn cyan [s] (str "\033[36m" s "\033[0m"))
(defn bg-black [s] (str "\033[40m" s "\033[0m"))
(defn bg-magenta [s] (str "\033[45m" s "\033[0m"))
(defn bg-yellow [s] (str "\033[43m" s "\033[0m"))
(defn red [s] (str "\033[31m" s "\033[0m"))
(defn blue [s] (str "\033[34m" s "\033[0m"))
(defn bg-default [s] (str "\033[49m" s "\033[0m"))
(defn bg-white [s] (str "\033[47m" s "\033[0m"))
(defn magenta [s] (str "\033[35m" s "\033[0m"))
(defn bg-blue [s] (str "\033[44m" s "\033[0m"))
(defn black [s] (str "\033[30m" s "\033[0m"))
