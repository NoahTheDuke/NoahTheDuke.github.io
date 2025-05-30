(ns cryogen-core.compiler
  (:require
   [clj-commons.format.exceptions :refer [print-exception]]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.zip :as zip]
   [cryogen-core.config :refer [resolve-config]]
   [cryogen-core.infer-meta :refer [using-inferred-metadata]]
   [cryogen-core.io :as cryogen-io]
   [cryogen-core.klipse :as klipse]
   [cryogen-core.markup :as markup]
   [cryogen-core.rss :as rss]
   [cryogen-core.sass :as sass]
   [cryogen-core.schemas :as schemas]
   [cryogen-core.sitemap :as sitemap]
   [cryogen-core.toc :as toc]
   [cryogen-core.util :as util :refer [blue cyan green red yellow]]
   [cryogen-core.zip-util :as zip-util]
   [medley.core :as m]
   [net.cgrand.enlive-html :as enlive]
   [schema.core :as s]
   [selmer.filters :refer [add-filter!]]
   [selmer.parser :refer [cache-off!]])
  (:import
   (java.net URLEncoder)
   (java.time ZoneId ZonedDateTime)
   (java.util Date Locale)))


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

(do (add-filter! :.beat #'beat)
    nil)

(defn sort-by-lower-case-name [tags]
  (sort-by (comp str/lower-case str :name) tags))

(do (add-filter! :sort-by-lowercase-name #'sort-by-lower-case-name)
    nil)

(cache-off!)

(def content-root "content")

(defn url-encode
  "Url encode path element. Encodes spaces as %20 instead of +,
  because some webservers pass + through to the file system"
  [str]
  (-> str
      (URLEncoder/encode "UTF-8")
      (str/replace "+" "%20")))

(defn only-changed-files-filter
  "Returns a page/post filter that only accepts these files:

  - no filtering if `changeset` is empty (as it means this is the first build)
  - recompile the post/page that has changed since the last compilation
  - recompile everything if a template HTML file has changed

  Arguments:
  - `changeset` - sequence of `java.io.File` of relative paths within the
    project, such as `File[content/asc/pages/about.asc]`, as provided by
    [[cryogen-core.watcher/find-changes]]

  Downsides: Tags, archives, index etc. will only know about the recompiled
  post and not the others. Previous/next links on neighbour posts won't be
  updated either."
  [changeset]
  (let [changed-paths      (->> changeset (map #(.getPath %)) set)
        theme-html-change? (some
                            (fn [^java.io.File file]
                              (and (str/starts-with?
                                    (.getPath file) "themes/")
                                   (str/ends-with?
                                    (.getName file) ".html")))
                            changeset)]
    (if (empty? changeset)
      (constantly true)
      (fn [^java.io.File page]
        (or
         theme-html-change?
         (contains? changed-paths (.getPath page)))))))

(defn find-entries
  "Returns a list of files under the content directory according to the
  implemented Markup protocol and specified root directory. It defaults to
  looking under the implemented protocol's subdirectory, but fallsback to look
  at the content directory."
  [root mu ignored-files]
  (let [assets (cryogen-io/find-assets
                (cryogen-io/path content-root (markup/dir mu) root)
                (markup/exts mu)
                ignored-files)]
    (if (seq assets)
      assets
      (cryogen-io/find-assets
       (cryogen-io/path content-root root)
       (markup/exts mu)
       ignored-files))))

(defn find-posts
  "Returns a list of markdown files representing posts under the post root."
  [{:keys [post-root ignored-files]} mu]
  (find-entries post-root mu ignored-files))

(defn find-pages
  "Returns a list of markdown files representing pages under the page root."
  [{:keys [page-root ignored-files]} mu]
  (find-entries page-root mu ignored-files))

(defn page-file-name
  "Creates a full page file name from a file name. `uri-type` is any of the uri types specified in config, e.g., `:post-root-uri`."
  ([file-name params]
   (page-file-name file-name nil params))
  ([file-name uri-type {:keys [blog-prefix clean-urls] :as params}]
   (let [page-uri (get params uri-type)
         uri-end  (condp = clean-urls
                    :trailing-slash (str/replace file-name #"(index)?\.html" "/")
                    :no-trailing-slash (str/replace file-name #"(index)?\.html" "")
                    :dirty file-name)]
     (cryogen-io/path "/" blog-prefix page-uri uri-end))))

(defn page-uri
  "Creates a URI from file name. `uri-type` is any of the uri types specified in config, e.g., `:post-root-uri`."
  ([file-name params]
   (page-uri file-name nil params))
  ([file-name uri-type params]
   (let [page-filename (page-file-name file-name uri-type params)]
     (str/join "/" (map url-encode (str/split page-filename #"/" -1))))))

(defn read-page-meta
  "Returns the clojure map from the top of a markdown page/post"
  [page rdr]
  (try
    (let [metadata (read rdr)]
      (s/validate schemas/MetaData metadata)
      metadata)
    (catch Exception e
      (throw (ex-info (ex-message e)
                      (assoc (ex-data e) :page page))))))

(defn- using-embedded-metadata [page config markup]
  (with-open [rdr (java.io.PushbackReader. (io/reader page))]
    (let [re-root     (re-pattern (str "^.*?(" (:page-root config) "|" (:post-root config) ")/"))
          page-fwd    (str/replace (str page) "\\" "/")  ;; make it work on Windows
          page-name   (if (:collapse-subdirs? config) (.getName page) (str/replace page-fwd re-root ""))
          file-name   (str/replace page-name (util/re-pattern-from-exts (markup/exts markup)) ".html")
          page-meta   (read-page-meta page-name rdr)
          content     ((markup/render-fn markup) rdr (assoc config :page-meta page-meta))
          content-dom (util/trimmed-html-snippet content)]
      {:file-name   file-name
       :page-meta   page-meta
       :content-dom content-dom})))

(def page-content
  "Returns a map with the given page's file-name, metadata and content parsed from
  the file with the given markup."
  (memoize
   (fn [^java.io.File page config markup]
     (try
       (using-embedded-metadata page config markup)
       (catch Throwable embedded-fail
         (try
           (using-inferred-metadata page markup config)
           (catch Throwable inferred-fail
             (throw (ex-info "Could not compile content of page"
                             {:page (.getName page)
                              :embedded-fail embedded-fail
                              :inferred-fail inferred-fail})))))))))

(defn add-toc
  "Adds :toc to article, if necessary"
  [{:keys [content-dom toc toc-class] :as article} config]
  (update
   article
   :toc
   #(when %
      (toc/generate-toc content-dom
                        {:list-type toc
                         :toc-class (or toc-class (:toc-class config) "toc")}))))

(defn merge-meta-and-content
  "Merges the page metadata and content maps"
  [file-name page-meta content-dom]
  (merge
   (update-in page-meta [:layout] #(str (name %) ".html"))
   {:file-name   file-name
    :content-dom content-dom}))

(defn parse-page
  "Parses a page/post and returns a map of the content, uri, date etc."
  [page config markup]
  (let [{:keys [file-name page-meta content-dom]} (page-content page config markup)]
    (-> (merge-meta-and-content file-name (update page-meta :layout #(or % :page)) content-dom)
        (merge
         {:type          :page
          :uri           (page-uri file-name :page-root-uri config)
          :page-index    (:page-index page-meta)
          :tags          (-> (:tags page-meta) distinct vec)
          :klipse/global (:klipse config)
          :klipse/local  (:klipse page-meta)})
        (add-toc config))))

(defn parse-post
  "Return a map with the given post's information."
  [page config markup]
  (let [{:keys [file-name page-meta content-dom]} (page-content page config markup)
        date            (if (:date page-meta)
                          (.parse (java.text.SimpleDateFormat. (:post-date-format config)) (:date page-meta))
                          (util/parse-post-date file-name (:post-date-format config)))
        archive-fmt     (java.text.SimpleDateFormat. ^String (:archive-group-format config) (Locale/getDefault))
        formatted-group (.format archive-fmt date)]
    (-> (merge-meta-and-content file-name (update page-meta :layout #(or % :post)) content-dom)
        (merge
         {:type                    :post
          :date                    date
          :formatted-archive-group formatted-group
          :parsed-archive-group    (.parse archive-fmt formatted-group)
          :uri                     (page-uri file-name :post-root-uri config)
          :tags                    (-> (:tags page-meta) distinct vec)
          :klipse/global           (:klipse config)
          :klipse/local            (:klipse page-meta)})
        (add-toc config))))

(defn read-posts
  "Returns a sequence of maps representing the data from markdown files of posts.
   Sorts the sequence by post date."
  [config]
  (->> (markup/markups)
       (mapcat
        (fn [mu]
          (->>
           (find-posts config mu)
           (pmap #(parse-post % config mu))
           (remove #(= (:draft? %) true)))))
       (sort-by :date)
       reverse
       (drop-while #(and (:hide-future-posts? config) (.after ^Date (:date %) (Date.))))))

(defn read-pages
  "Returns a sequence of maps representing the data from markdown files of pages.
  Sorts the sequence by post date."
  [config]
  (->> (markup/markups)
       (mapcat
        (fn [mu]
          (->>
           (find-pages config mu)
           (map #(parse-page % config mu)))))
       (sort-by :page-index)))

(defn tag-post
  "Adds the uri and title of a post to the list of posts under each of its tags"
  [tags post]
  (reduce (fn [tags tag]
            (update-in tags [tag] (fnil conj []) (select-keys post [:uri :title :content-dom :date :enclosure :description])))
          tags
          (:tags post)))

(defn group-by-tags
  "Maps all the tags with a list of posts that contain each tag"
  [posts]
  (reduce tag-post {} posts))

(defn group-for-archive
  "Groups the posts by month and year for archive sorting"
  [posts]
  (->> posts
       (map #(select-keys % [:title :uri :date :formatted-archive-group :parsed-archive-group]))
       (group-by :formatted-archive-group)
       (map (fn [[group posts]]
              {:group        group
               :parsed-group (:parsed-archive-group (get posts 0))
               :posts        (map #(select-keys % [:title :uri :date]) posts)}))
       (sort-by :parsed-group)
       reverse))

(defn group-for-author
  "Groups the posts by author. If no post author if found defaults `default-author`."
  [posts default-author]
  (->> posts
       (map #(select-keys % [:title :uri :date :formatted-archive-group :parsed-archive-group :author]))
       (map #(update % :author (fn [author] (or author default-author))))
       (group-by :author)
       (map (fn [[author posts]]
              {:author author
               :posts  posts}))))

(defn tag-info
  "Returns a map containing the name and uri of the specified tag"
  [config tag]
  {:name (name tag)
   :file-path (page-file-name (str (name tag) ".html") :tag-root-uri config)
   :uri  (page-uri (str (name tag) ".html") :tag-root-uri config)})

(defn add-prev-next
  "Adds a :prev and :next key to the page/post data containing the metadata of the prev/next
  post/page if it exists.
  If a page does not have the sort key (:page-index or :date), do not add prev/next.
  If the prev/next page does not have the sort key, do not add it."
  [sort-by-key pages]
  (map (fn [[prev target next]]
         (if (get target sort-by-key)
           (assoc target
                  :prev (if (and prev (get prev sort-by-key)) (dissoc prev :content-dom) nil)
                  :next (if (and next (get next sort-by-key)) (dissoc next :content-dom) nil))
           target))
       (partition 3 1 (flatten [nil pages nil]))))

(defn group-pages
  "Separates the pages into links for the navbar and links for the sidebar"
  [pages]
  (let [{navbar-pages  true
         sidebar-pages false} (group-by #(boolean (:navbar? %)) pages)]
    (map (partial sort-by :page-index) [navbar-pages sidebar-pages])))

(defn write-html
  "When `clean-urls` is set to:
  - `:trailing-slash` appends `/index.html`.
  - `:no-trailing-slash` appends `.html`.
  - `:dirty` just spits."
  [file-uri {:keys [blog-prefix clean-urls]} data]
  (condp = clean-urls
    :trailing-slash (cryogen-io/create-file-recursive
                     (cryogen-io/path file-uri "index.html") data)
    :no-trailing-slash (cryogen-io/create-file
                        (if (or (= blog-prefix file-uri) (= "/" file-uri))
                          (cryogen-io/path file-uri "index.html")
                          (str file-uri ".html"))
                        data)
    :dirty (cryogen-io/create-file file-uri data)))

(defn- print-debug-info [data]
  (println "DEBUG:")
  (pprint data))

(defn content-dom->html [{dom :content-dom :as article}]
  (-> article
      (dissoc :content-dom)
      (assoc :content (util/enlive->html-text dom))))

(defn htmlize-content [{:keys [postprocess-article-html-fn] :as params}]
  (letfn [(postprocess-article [article]
            (if postprocess-article-html-fn
              (postprocess-article-html-fn article params)
              article))
          (htmlize-article [article]
            (-> article
                content-dom->html
                postprocess-article))]
    (cond
      (contains? params :posts) (update params :posts (partial map htmlize-article))
      (contains? params :post) (update params :post htmlize-article)
      (contains? params :page) (update params :page htmlize-article)
      :else params)))

(defn render-file
  "Wrapper around `selmer.parser/render-file` with pre-processing"
  [file-path params]
  (selmer.parser/render-file
   file-path
   (htmlize-content params)))

(defn compile-pages
  "Compiles all the pages into html and spits them out into the public folder"
  [{:keys [blog-prefix page-root-uri debug?] :as params} pages]
  (when-not (empty? pages)
    (println (blue "compiling pages"))
    (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix page-root-uri))
    (doseq [{:keys [uri] :as page} pages]
      (println "-->" (cyan uri))
      (when debug?
        (print-debug-info page))
      (write-html uri
                  params
                  (render-file (str "/html/" (:layout page))
                               (merge params
                                      {:active-page     "pages"
                                       :home            false
                                       :selmer/context  (cryogen-io/path "/" blog-prefix "/")
                                       :page            page
                                       :uri             uri}))))))

(defn compile-posts
  "Compiles all the posts into html and spits them out into the public folder"
  [{:keys [blog-prefix post-root-uri debug?] :as params} posts]
  (when (seq posts)
    (println (blue "compiling posts"))
    (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix post-root-uri))
    (doseq [{:keys [uri] :as post} posts]
      (println "-->" (cyan uri))
      (when debug?
        (print-debug-info post))
      (write-html uri
                  params
                  (render-file (str "/html/" (:layout post))
                               (merge params
                                      {:active-page    "posts"
                                       :selmer/context (cryogen-io/path "/" blog-prefix "/")
                                       :post           post
                                       :uri            uri}))))))

(defn compile-tags
  "Compiles all the tag pages into html and spits them out into the public folder"
  [{:keys [blog-prefix tag-root-uri] :as params} posts-by-tag]
  (when-not (empty? posts-by-tag)
    (println (blue "compiling tags"))
    (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix tag-root-uri))
    (doseq [[tag posts] posts-by-tag
            :let [{:keys [name uri file-path]} (tag-info params tag)]]
      (println "-->" (cyan uri))
      (write-html file-path
        params
        (render-file "/html/tag.html"
                     (merge params
                            {:active-page     "tags"
                             :selmer/context  (cryogen-io/path "/" blog-prefix "/")
                             :name            name
                             :posts           posts
                             :uri             uri}))))))

(defn compile-tags-page
  "Compiles a page with links to each tag page. Spits the page into the public folder"
  [{:keys [blog-prefix] :as params}]
  (println (blue "compiling tags page"))
  (let [uri (page-uri "tags.html" params)]
    (write-html uri
                params
                (render-file "/html/tags.html"
                             (merge params
                                    {:active-page     "tags"
                                     :selmer/context  (cryogen-io/path "/" blog-prefix "/")
                                     :uri             uri})))))

(defn content-until-more-marker
  "Returns the content until the <!--more--> special comment,
  closing any unclosed tags. Returns nil if there's no such comment."
  [content-dom]
  (some-> (zip/xml-zip {:content content-dom})
          (zip-util/cut-tree-vertically zip-util/more-marker?)
          (zip/node)
          :content))

(defn preview-dom [blocks-per-preview content-dom]
  (->> (or (content-until-more-marker content-dom)
           content-dom)
       (take blocks-per-preview)
       (take-while #(not (#{:hr :h1 :h2 :h3 :h4 :h5 :h6 :h7 :h8} (:tag %))))))

(defn create-preview
  "Creates a single post preview"
  [blocks-per-preview post]
  (let [content-dom (preview-dom blocks-per-preview (:content-dom post))]
    (-> post
        (assoc :content-dom content-dom)
        (assoc :full-post (= (:content-dom post) content-dom)))))

(defn create-previews
  "Returns a sequence of vectors, each containing a set of post previews"
  [posts posts-per-page blocks-per-preview]
  (->> posts
       (map #(create-preview blocks-per-preview %))
       (partition-all posts-per-page)
       (map-indexed (fn [i v] {:index (inc i) :posts v}))))

(defn create-preview-links
  "Turn each vector of previews into a map with :prev and :next keys that contain the uri of the
  prev/next preview page"
  [previews params]
  (mapv (fn [[prev target next]]
          (merge target
                 {:prev (if prev (page-uri (cryogen-io/path "p" (str (:index prev) ".html")) params) nil)
                  :next (if next (page-uri (cryogen-io/path "p" (str (:index next) ".html")) params) nil)}))
        (partition 3 1 (flatten [nil previews nil]))))

(defn compile-preview-pages
  "Compiles a series of pages containing 'previews' from each post"
  [{:keys [blog-prefix posts-per-page blocks-per-preview] :as params} posts]
  (when-not (empty? posts)
    (let [previews (-> posts
                       (create-previews posts-per-page blocks-per-preview)
                       (create-preview-links params))]
      (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix "p"))
      (doseq [{:keys [index posts prev next]} previews]
        (write-html
         (page-uri (cryogen-io/path "p" (str index ".html")) params)
         params
         (render-file "/html/previews.html"
                      (merge params
                             {:active-page     "preview"
                              :home            false
                              :timeline        true
                              :selmer/context  (cryogen-io/path "/" blog-prefix "/")
                              :posts           posts
                              :prev-uri        prev
                              :next-uri        next})))))))

(defn add-description
  "Add plain text `:description` to the page/post for use in meta description etc."
  [{:keys [blocks-per-preview description-include-elements]
    :or   {description-include-elements #{:p :h1 :h2 :h3 :h4 :h5 :h6}}}
   page]
  (update
   page :description
   #(cond
      (false? %) nil  ;; if set via page meta to false, do not set
      % %    ;; if set via page meta, use it
      :else (->> (enlive/select
                  (preview-dom blocks-per-preview (:content-dom page))
                  [(set description-include-elements)])
                 (util/enlive->plain-text)))))

(defn compile-index
  "Compiles the index page into html and spits it out into the public folder"
  [{:keys [blog-prefix debug? home-page] :as params}]
  (println (blue "compiling index"))
  (let [uri (page-uri "index.html" params)]
    (when debug?
      (print-debug-info meta))
    (write-html uri
                params
                (render-file (str "/html/" (:layout home-page))
                             (merge params
                                    {:active-page       "home"
                                     :home              true
                                     :selmer/context    (cryogen-io/path "/" blog-prefix "/")
                                     :uri               uri
                                     (:type home-page)  home-page})))))

(defn compile-archives
  "Compiles the archives page into html and spits it out into the public folder"
  [{:keys [blog-prefix] :as params} posts]
  (println (blue "compiling archives"))
  (let [uri (page-uri "archives.html" params)]
    (write-html uri
                params
                (render-file "/html/archives.html"
                             (merge params
                                    {:active-page     "archives"
                                     :archives        true
                                     :groups          (group-for-archive posts)
                                     :selmer/context  (cryogen-io/path "/" blog-prefix "/")
                                     :uri             uri})))))

(defn compile-cohost-archive
  "Compiles all the pages into html and spits them out into the public folder"
  [{:keys [blog-prefix page-root-uri debug? blocks-per-preview raw-pages] :as params}
   cohost-posts]
  (when-not (empty? cohost-posts)
    (println (blue "compiling cohost archive"))
    (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix page-root-uri))
    (doseq [{:keys [uri] :as post} cohost-posts]
      (println "-->" (cyan uri))
      (when debug?
        (print-debug-info post))
      (write-html uri
        params
        (render-file (str "/html/" (:layout post))
                     (merge params
                            {:active-page     "posts"
                             :home            false
                             :selmer/context  (cryogen-io/path "/" blog-prefix "/")
                             :post            post
                             :uri             uri}))))
    (let [archive-root (m/find-first :cohost-root raw-pages)
          page-previews (->> cohost-posts
                             (remove :cohost-root)
                             (map #(create-preview blocks-per-preview %))
                             (map content-dom->html))]
      (println "-->" (cyan (:uri archive-root)))
      (write-html (:uri archive-root)
        params
        (render-file "/html/cohost-archive.html"
                     (merge params
                            {:active-page     "pages"
                             :home            false
                             :selmer/context  (cryogen-io/path "/" blog-prefix "/")
                             :page            archive-root
                             :cohost-previews page-previews
                             :uri             (:uri archive-root)}))))))

(defn tag-posts
  "Converts the tags in each post into links"
  [config posts]
  (map #(update-in % [:tags] (partial map (partial tag-info config))) posts))

(defn- content-dir?
  "Checks that the dir exists in the content directory."
  [dir]
  (.isDirectory (io/file (cryogen-io/path content-root dir))))

(defn- markup-entries [post-root page-root]
  (let [entries (for [mu (markup/markups)
                      t  (distinct [post-root page-root])]
                  [(cryogen-io/path (markup/dir mu) t) t])]
    (apply concat entries)))

(defn copy-resources-from-markup-folders
  "Copy resources from markup folders. This does not copy the markup entries."
  [{:keys [post-root page-root] :as config}]
  (let [folders (->> (markup-entries post-root page-root)
                     (filter content-dir?))]
    (cryogen-io/copy-resources
     content-root
     (merge config
            {:resources     folders
             :ignored-files (map #(util/re-pattern-from-exts (markup/exts %)) (markup/markups))}))))

(defn compile-assets
  "Generates all the html and copies over resources specified in the config.

  Params:
   - `overrides-and-hooks` - may contain overrides for `config.edn`; anything
      here will be available to the page templates, except for the following special
                parameters:
     - `:extend-params-fn` - a function (`params`, `site-data`) -> `params` -
                             use it to derive/add additional params for templates
     - `:postprocess-article-html-fn` - a function (`article`, `params`) -> `article`
                             called after the `:content` has been rendered to HTML and
                              right before it is written to the disk. Example fn:
                              `(fn postprocess [article params] (update article :content selmer.parser/render params))`
     - `:update-article-fn` - a function (`article`, `config`) -> `article` to update a
                            parsed page/post via its `:content-dom`. Return nil to exclude the article.
     - `changeset` - as supplied by
                   [[cryogen-core.watcher/start-watcher-for-changes!]] to its callback
                   for incremental compilation, see [[only-changed-files-filter]]
                   for details

  Note on terminology:
   - `article` - a post or page data (including its title, content, etc.)
   - `config` - the site-wide configuration ± from `config.edn` and the provided overrides
   - `params` - `config` + content such as `:pages` etc.
   - `site-data` - a subset of the site content such as `:pages`, `:posts` - see the code below"
  ([] (compile-assets {}))
  ([overrides]
   (println (green "compiling assets..."))
   (when-not (empty? overrides)
     (println (yellow "overriding config.edn with:"))
     (pprint overrides))
   (let [{:keys [^String site-url blog-prefix rss-name recent-posts keep-files theme]
          :as   config} (resolve-config overrides)
         all-posts        (->> (read-posts config)
                               (map klipse/klipsify)
                               (map (partial add-description config))
                               (remove nil?))
         grouped-posts (group-by #(get % :unlisted? false) all-posts)
         unlisted-posts (get grouped-posts true [])
         posts (get grouped-posts false [])
         posts (add-prev-next :date posts)
         posts-by-tag (group-by-tags posts)
         posts        (tag-posts config posts)
         latest-posts (->> posts (take recent-posts) vec)
         pages        (->> (read-pages config)
                           (map klipse/klipsify)
                           (map (partial add-description config)))
         home-page    (m/find-first :home? pages)
         other-pages  (->> pages
                           (remove #{home-page})
                           (add-prev-next :page-index))
         cohost-pages (->> (read-posts (assoc config
                                              :post-root "cohost-archive"
                                              :post-root-uri "cohost-archive"))
                           (tag-posts config)
                           (map klipse/klipsify)
                           (map (partial add-description config)))
         [navbar-pages
          sidebar-pages] (group-pages other-pages)
         other-pages (remove :cohost-root other-pages)
         params (merge
                 config
                 {:today         (Date.)
                  :title         (:site-title config)
                  :active-page   "home"
                  :tags          (map (partial tag-info config) (keys posts-by-tag))
                  :latest-posts  latest-posts
                  :navbar-pages  navbar-pages
                  :sidebar-pages sidebar-pages
                  :home-page     (if home-page
                                   home-page
                                   (assoc (first latest-posts) :layout "home.html"))
                  :archives-uri  (page-uri "archives.html" config)
                  :index-uri     (page-uri "index.html" config)
                  :timeline-uri  (page-uri (cryogen-io/path "p" "1.html") config)
                  :tags-uri      (page-uri "tags.html" config)
                  :rss-uri       (cryogen-io/path "/" blog-prefix rss-name)
                  :site-url      (if (.endsWith site-url "/") (.substring site-url 0 (dec (count site-url))) site-url)
                  :raw-posts posts
                  :raw-pages pages})]

     (assert (not (:posts params))
             (str "Sorry, you cannot add `:posts` to params because this is"
                  " used internally at some places. Pick a different keyword."))

     (selmer.parser/set-resource-path!
      (util/file->url (io/as-file (cryogen-io/path "themes" theme))))
     (cryogen-io/set-public-path! (:public-dest config))
     (cryogen-io/wipe-public-folder keep-files)
     (println (blue "compiling sass"))
     (sass/compile-sass->css! config)
     (println (blue "copying theme resources"))
     (cryogen-io/copy-resources-from-theme config)
     (println (blue "copying resources"))
     (cryogen-io/copy-resources "content" config)
     (copy-resources-from-markup-folders config)
     (compile-pages params other-pages)
     (compile-posts params (concat posts unlisted-posts))
     (compile-tags params posts-by-tag)
     (compile-tags-page params)
     (compile-preview-pages params posts)
     (compile-index params)
     (compile-archives params posts)
     (compile-cohost-archive params cohost-pages)
     (println (blue "generating site map"))
     (->> (sitemap/generate site-url config)
          (cryogen-io/create-file (cryogen-io/path "/" blog-prefix "sitemap.xml")))
     (println (blue "generating main rss"))
     (->> (rss/make-channel config posts)
          (cryogen-io/create-file (cryogen-io/path "/" blog-prefix rss-name)))
     (when (:rss-filters config)
       (println (blue "generating filtered rss")))
     (rss/make-filtered-channels config posts-by-tag))))

(defn compile-assets-timed
  "See the docstring for [[compile-assets]]"
  ([] (compile-assets-timed {}))
  ([config]
   (time
    (try
      (compile-assets config)
      (catch Exception e
        (if (or (instance? IllegalArgumentException e)
                (instance? clojure.lang.ExceptionInfo e))
          (println (red "Error:") (yellow (.getMessage e)))
          (print-exception e)))))))

(comment
  (def *config (resolve-config {}))

  ;; Build and copy only styles & theme
  (do
    (sass/compile-sass->css! *config)
    (cryogen-io/copy-resources-from-theme *config))

  ;; Build a single page (quicker than all)
  (compile-assets
    ;; Insert the prefix and suffix of the only file you _want_ to process
   {:ignored-files [#"^(?!2019-12-12-nrepl-).*\.asc"]})

  nil)
