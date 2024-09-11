(ns cryogen.server
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clygments.core :as clygments]
   [compojure.core :refer [defroutes GET]]
   [compojure.route :as route]
   [cryogen-core.compiler :refer [compile-assets-timed]]
   [cryogen-core.config :refer [resolve-config]]
   [cryogen-core.io :refer [path]]
   [cryogen-core.plugins :refer [load-plugins]]
   [cryogen-core.util :as util]
   [cryogen-core.watcher :refer [start-watcher! start-watcher-for-changes!]]
   [emoji.core :as e]
   [net.cgrand.enlive-html :as enlive]
   [ring.server.standalone :as ring-server]
   [ring.util.codec :refer [url-decode]]
   [ring.util.response :refer [file-response redirect]]
   [selmer.filters :refer [add-filter!]])
  (:import
   (java.io File)
   (java.time ZoneId ZonedDateTime)))

(def biel-mean-time (ZoneId/of "UTC+01:00"))
(def beat-length 86.4)

(defn beat [^java.util.Date timestamp]
  (when timestamp
    (let [instant (.toInstant timestamp)
          ts (ZonedDateTime/ofInstant instant biel-mean-time)]
      (->> (/ (+ (* 3600 (.getHour ts))
                 (* 60 (.getMinute ts))
                 (.getSecond ts))
              beat-length)
           (format "%.2f")))))

(comment
  (beat (java.util.Date.)))

(add-filter! :.beat #'beat)

(defn transform-html
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
            ;; language-tagged code blocks
            (= :pre (:tag node))
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
                node))
            :else
            node)))))

(comment
  (do (transform-html "<p>I recently came across a <a href=\"http://johnj.com/from-elegance-to-speed.html\">blogpost</a> about rewriting an &quot;elegant&quot; function in Clojure into an optimized function in Common Lisp. In it, John Jacobsen discusses how they went from this Clojure code:</p>\n<pre><code class=\"clojure\">(defn smt-8 [times]\n  (-&gt;&gt; times\n       (partition 8 1)\n       (map (juxt identity\n                  (comp (partial apply -)\n                        (juxt last first))))\n       (filter (comp (partial &gt; 1000) second))))\n</code></pre>\n<p>to this Common Lisp code:</p>\n<pre><code class=\"clojure\">(loop for x below (- (the fixnum array-size) 8)\n     if (&lt; (- (the fixnum (elt *s* (+ (the fixnum x) 8)))\n              (the fixnum (elt *s* (the fixnum x))))\n           1000)\n     collect x)\n</code></pre>\n")
      nil))

(alter-var-root
  #'util/trimmed-html-snippet
  (fn [_f] transform-html))

(defn postprocess-article [article _params]
  (println (e/emojify (:content article)))
  (update article :content e/emojify))

(def resolved-config (delay (resolve-config)))

(def extra-config-dev
  "Add dev-time configuration overrides here, such as `:hide-future-posts? false`"
  {:postprocess-article-html-fn #'postprocess-article})

(defn init [fast?]
  (load-plugins)
  (compile-assets-timed extra-config-dev)
  (let [ignored-files (-> @resolved-config :ignored-files)]
    (run!
      #(if fast?
         (start-watcher-for-changes! % ignored-files compile-assets-timed extra-config-dev)
         (start-watcher! % ignored-files compile-assets-timed))
      ["content" "themes"])))

(defn wrap-subdirectories
  [handler]
  (fn [request]
    (let [{:keys [clean-urls blog-prefix public-dest]} @resolved-config
          req-uri (.substring (url-decode (:uri request)) 1)
          res-path (if (or (.endsWith req-uri "/")
                           (.endsWith req-uri ".html")
                           (-> (str/split req-uri #"/")
                               last
                               (str/includes? ".")
                               not))
                     (condp = clean-urls
                       :trailing-slash (path req-uri "index.html")
                       :no-trailing-slash (if (or (= req-uri "")
                                                  (= req-uri "/")
                                                  (= req-uri
                                                     (if (str/blank? blog-prefix)
                                                       blog-prefix
                                                       (subs blog-prefix 1))))
                                            (path req-uri "index.html")
                                            (path (str req-uri ".html")))
                       :dirty (path (str req-uri ".html")))
                     req-uri)]
        (or (let [rsp (file-response res-path {:root public-dest})
                  body (:body rsp)]
              ;; Add content-type; it cannot be derived from the extension if `:[no-]trailing-slash`
              (cond-> rsp
                      (and body
                           (instance? File body)
                           (str/ends-with? (.getName body) ".html"))
                      (assoc-in [:headers "Content-Type"] "text/html")))
            (handler request)))))

(defroutes routes
  (GET "/" [] (redirect (let [config (resolve-config)]
                          (path (:blog-prefix config)
                                (when (= (:clean-urls config) :dirty)
                                  "index.html")))))
  (route/files "/")
  (route/not-found "Page not found"))

(def handler (wrap-subdirectories routes))

(defn serve
  "Entrypoint for running via tools-deps (clojure)"
  [{:keys [fast join?] :as opts}]
  (ring-server/serve
    handler
    (merge
      {:join? (if (some? join?) join? true)
       :init (partial init fast)
       :open-browser? true
       :auto-refresh? fast ; w/o fast it would often try to reload the page before it has been fully compiled
       :refresh-paths [(:public-dest @resolved-config)]}
      opts)))

(defn -main [& args]
  (serve {:port 3000 :fast ((set args) "fast")}))

(comment
  (def srv (serve {:join? false, :fast false}))
  (.stop srv)
  ,)
