(ns cryogen-core.rss
  (:require
   [clj-rss.core :as rss]
   [clojure.string :as str]
   [cryogen-core.io :as cryogen-io]
   [text-decoration.core :refer [cyan]])
  (:import
   java.util.Date))

(set! *warn-on-reflection* true)

(defn posts-to-items [site-url posts]
  (mapv
    (fn [{:keys [uri title content-dom date enclosure author description]}]
      (let [link (str (if (str/ends-with? site-url "/") (apply str (butlast site-url)) site-url) uri)]
        (merge {:guid        link
                :link        link
                :title       title
                :description description
                :author      author
                :pubDate     date}
               (when enclosure
                 {:enclosure enclosure}))))
    posts))

(defn make-channel [config posts]
  (apply
   #(rss/channel-xml
     false
     {:title         (:site-title config)
      :link          (:site-url config)
      :description   (:description config)
      :lastBuildDate (Date.)}
     %)
    (posts-to-items (:site-url config) posts)))

(defn make-filtered-channels [{:keys [rss-filters blog-prefix] :as config} posts-by-tag]
  (doseq [f rss-filters]
    (let [uri (cryogen-io/path "/" blog-prefix (str (name f) ".xml"))]
      (println "\t-->" (cyan uri))
      (cryogen-io/create-file uri (make-channel config (get posts-by-tag f))))))
