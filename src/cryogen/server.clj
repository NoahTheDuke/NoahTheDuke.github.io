(ns cryogen.server
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [compojure.core :refer [GET defroutes]]
   [compojure.route :as route]
   [cryogen-core.compiler :refer [compile-assets-timed]]
   [cryogen-core.config :refer [resolve-config]]
   [cryogen-core.io :refer [path]]
   [cryogen-core.plugins :refer [load-plugins]]
   [cryogen-core.util :as util]
   [cryogen-core.watcher :refer [start-watcher! start-watcher-for-changes!]]
   [clygments.core :as clygments]
   [net.cgrand.enlive-html :as enlive]
   [ring.server.standalone :as ring-server]
   [ring.util.codec :refer [url-decode]]
   [emoji.core :as e]
   [ring.util.response :refer [file-response redirect]]))

(defn transform-html
  "Creates an enlive-snippet from `html-string` then removes
  the newline nodes"
  [html-string]
  (->> (enlive/html-snippet html-string)
       (walk/postwalk
         (fn [node]
           (cond
             ;; empty nodes
             (seq? node)
             (remove #(and (string? %) (re-matches #"\n\h*" %)) node)
             ;; code blocks
             (= "listingblock" (get-in node [:attrs :class]))
             (let [pre (-> node :content first :content first)
                   n (first (:content pre))
                   lang (keyword (get-in n [:attrs :data-lang]))
                   content (-> (:content n)
                               (first)
                               (clygments/highlight lang :html))]
               (->> (enlive/html-snippet content)
                    (assoc n :content)
                    (list)
                    (assoc node :content)))
             :else
             node)))))

(alter-var-root
  #'util/trimmed-html-snippet
  (fn [_f] transform-html))

(defn postprocess-article [article _params]
  (println (e/emojify (:content article)))
  (update article :content e/emojify))

(defn init [fast?]
  (println "Init: fast compile enabled = " (boolean fast?))
  (load-plugins)
  (compile-assets-timed
    {:postprocess-article-html-fn #'postprocess-article})
  (let [ignored-files (:ignored-files (resolve-config))]
    (run!
      #(if fast?
         (start-watcher-for-changes! % ignored-files compile-assets-timed {})
         (start-watcher! % ignored-files compile-assets-timed))
      ["content" "themes"])))

(defn wrap-subdirectories
  [handler]
  (fn [request]
    (let [{:keys [clean-urls blog-prefix public-dest]} (resolve-config)
          req-uri (subs (url-decode (:uri request)) 1)
          res-path (if (or (str/ends-with? req-uri "/")
                           (str/ends-with? req-uri ".html")
                           (-> (str/split req-uri #"/")
                               (last)
                               (str/includes? ".")
                               (not)))
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
      (or (file-response res-path {:root public-dest})
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
  [{:keys [fast] :as opts}]
  (ring-server/serve
    handler
    (merge {:init (partial init fast)} opts)))

(defn -main [& args]
  (serve {:port 1024, :fast ((set args) "fast")}))
