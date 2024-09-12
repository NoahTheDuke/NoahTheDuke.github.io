(ns cryogen.cohost
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [medley.core :as m]
   [selmer.parser :as selmer]))

(def posts
  (->> (json/parse-string (slurp "/Users/noah/personal/cohost-dl/out/noahtheduke/posts.json") true)
       (map #(assoc % :share (boolean (seq (:shareTree %)))))
       (map #(assoc % :authors (concat (map (fn [p] (:handle p)) (:relatedProjects %))
                                       [(:handle (:postingProject %))])))
       (map #(update % :tags (fn [tags]
                               (->> tags
                                    (map (fn [t] (str/escape t {\" "\\\""})))
                                    (cons "cohost mirror")
                                    vec))))
       (map #(update % :headline str/escape {\" "\\\""}))))

(comment
  (->> posts
       (remove :pinned)
       (remove :share)
       (map #(dissoc % :postingProject :astMap))
       (m/find-first #(str/includes? (:headline %) "bogos"))
       ; (m/find-first #(= "4039109-1-death-stranding-2" (:filename %)))
       ))

(defmulti block "convert block to html" :type)

(defmethod block "markdown"
  block--markdown
  [block]
  (-> block :markdown :content str/trim not-empty))

(defmethod block "attachment"
  block--attachment
  [block]
  (let [post-filename (::filename block)
        {alt :altText
         id :attachmentId
         kind :kind
         file-url :fileURL} (:attachment block)
        [_ filename] (str/split file-url (re-pattern id))
        filename (str/replace (subs filename 1) "%20" " ")
        img-path (io/file "content" "img" "cohost-mirror" post-filename filename)
        src-path (fs/expand-home (fs/path "~" "personal" "cohost-dl" "out" "rc" "attachment" id filename))]
    (when-not (fs/exists? src-path)
      (println post-filename))
    (when (and (not (fs/exists? img-path))
               (fs/exists? src-path))
      (println "copying" (str img-path))
      (io/make-parents img-path)
      (fs/copy src-path img-path {:replace-existing true}))
    (case kind
      "image" (format "![%s](/img/cohost-mirror/%s/%s)" alt post-filename filename)
      "audio" nil)))

(defmethod block "ask"
  block--ask
  [block]
  (format "**@%s** asked:\n> %s"
          (or (-> block :ask :askingProject :handle)
              "Anonymous")
          (-> block :ask :content str/trim)))

(defn combine-blocks [post]
  (let [body (->> post
                  :blocks
                  (map #(assoc % ::filename (:filename post)))
                  (keep block)
                  (str/join "\n\n")
                  (str/trim)
                  (not-empty))]
    (if body
      (assoc post ::body body)
      (-> post
          (dissoc :headline)
          (assoc ::body (:headline post))))))

(do
  (def template (slurp (io/file "resources" "cohost_template.md")))

  (defn render-post [post]
    (let [rendered-post (str (str/trim (selmer/render template post))
                             "\n\n"
                             (::body post))
          post-filename (format "%s-%s.md"
                                (first (str/split (:publishedAt post) #"T"))
                                (subs (:filename post)
                                      (inc (count (str (:postId post))))))
          post-path (io/file "content" "md" "posts" "cohost-mirror" post-filename)]
      (io/make-parents post-path)
      (spit post-path rendered-post)))

  (->> posts
       (remove (some-fn :pinned :share))
       (map combine-blocks)
       (run! render-post)
       ))
