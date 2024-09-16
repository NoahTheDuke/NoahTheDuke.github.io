(ns cryogen.cohost
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [ring.util.codec :as c]
   [medley.core :as m]
   [selmer.parser :as selmer]))

(def good-posts
  #{;; original list
    "3252059-money-handling-when"
    "3566741-thoughts-on-being-po"
    "2842001-pixar-movie-rankings"
    "4225466-gender-feelings"
    "3201474-at-this-point-i-don"
    ;; new additions
    "4867618-to-be-a-pedant"
    "7392136-cw-suicidal-ideatio"
    "6365357-i-dropped-my-iphone"
    "6120035-just-look-at-this-fu"
    "6002566-only-through-repetit"
    "5500275-mental-math-pseudo-r"
    "5434180-in-the-comment-of-th"
    "5174669-looks-bad-tastes-gr"
    "5168099-i-think-a-lot-about"
    "5099857-the-main-goal-of-th"
    "5075781-hot-take-spellcheck"
    "4997698-if-daylight-saving-t"
    "4962364-buying-digital-music"
    "4944983-name-ideas"
    "4738263-the-oldest-house"
    "4687878-learned-about-the-ta"
    "4253502-i-want-a-tattoo-that"
    "3905105-super-hexagon-by-t"
    "3895180-encanto-isn-t-good"
    "3746439-i-ve-used-my-switch"
    "3656388-i-ended-up-buying"
    "3611722-the-funny-thing-abou"
    "3296132-i-don-t-know-how-to"
    "3271389-george-lucas-selling"
    "3261127-ynab-privacy"
    "3213204-on-the-one-hand-i-d"
    "3070906-bought-some-more-mus"
    "2901583-if-you-re-gonna-crit"
    "2848499-got-a-new-shirt"
    "2837714-how-do-you-come-up-w"
    "2812517-y-all-ever-think-abo"
    "2801315-jumbled-sexuality-th"
    "2646635-looking-for-a-live-a"
    "2598417-old-memories"
    "2546608-cw-gamergate"
    "2195959-bogos-binted-was"
    "1950335-i-understand-that-co"
    "1215841-whomst-can-i-talk-to"
    "1645249-went-to-a-concert-an"
    "1360941-cw-suicide"
    "1249274-company-fired-some-o"
    ;; friends at the table
    "5249141-palisade-41-is-reall"
    "5050236-i-hope-i-never-forge"
    ;; ttrpgs
    "5032286-reign-2e"
    "5094613-more-reign-2e-though"
    "5058467-i-forgot-about-the"
    "4378205-just-popped-into-my"
    ;; memes/jokes
    "407951-eggbug-eggbug"
    "5656627-the-bene-gesserit-l"
    "4174530-trans-jerry-i-have"
    "7509572-hole-w-t-we-gardena"
    "7272516-fanfic-of-william-gi"
    "5136426-may-thy-knife-chip-a"
    "4904173-eggs-bene-gesserit"
    ;; taylor swift
    "4030418-everyone-i-know-post"
    "4047732-i-spent-multiple-hou"
    "4032503-here-s-the-chart"
    "1887941-album-is-easy-lover"
    "2436561-this-album-is-so-fuc"
    "1798662-saw-taylor-swift-las"
    ;; programming
    "7369102-ocaml-wishlist"
    "7108863-they-merged-it-my-c"
    "7020673-learning-a-new-progr"
    "6286441-clojure-enterprise"
    "5980051-i-think-i-severely"
    "5793881-open-source-maintena"
    "5258703-ruminations-on-techn"
    "5018957-yuki-s-vacation-an"
    "4896592-what-is-this-data-st"
    "4445263-viscerally-annoyed"
    "4086578-tyranny-of-the-blank"
    "2711345-if-your-programming"
    "2400662-mild-success"
    "1789527-in-increasing-order"
    "161270-i-commented-on-someo"
    })

(def skip-posts
  #{"7300465-i-should-probably-st"
    "5145221-especially-when-your"
    "7148525-how-i-feel-when-my-w"
    })

(def good-tags
  #{"adhdchosting"
    "adhd posting"
    "jnet"
    "jinteki.net"
    "kidposting"
    "noahtheduke asks"
    "noahtheduke reviews"
    "parentposting"
    "splint"
    "wifeposting"})

(defn post-filter [post]
  (and (not (skip-posts (:filename post)))
       (or (good-posts (:filename post))
           (some good-tags (:tags post)))))

(def posts
  (->> (json/parse-string (slurp "/Users/noah/personal/cohost-dl/out/noahtheduke/posts.json") true)
       (map #(assoc % :share (boolean (seq (:shareTree %)))))
       (map #(assoc % :authors (concat (map (fn [p] (:handle p)) (:relatedProjects %))
                                       [(:handle (:postingProject %))])))
       (map #(update % :tags (fn [tags]
                               (->> tags
                                    (map (fn [t]
                                           (if (str/starts-with? t "#")
                                             (subs t 1)
                                             t)))
                                    (map (fn [t] (str/escape t {\" "\\\""})))
                                    (cons "cohost mirror")
                                    (distinct)
                                    vec))))
       (map #(update % :headline str/escape {\" "\\\""}))))

(comment
  (->> posts
       (remove :pinned)
       (map #(dissoc % :astMap))
       (take 1)
       (map #(dissoc % :postingProject))
       (map #(assoc % :shareTree (filter (fn [st] (seq (:blocks st))) (:shareTree %))))
       ; (m/find-first #(str/includes? (:headline %) "blog"))
       ; (m/find-first #(= "4039109-1-death-stranding-2" (:filename %)))
       ))

(defmulti render-block "convert block to html" :type)

(defmethod render-block "markdown"
  render-block--markdown
  [block]
  (let [block-str (-> block
                      :markdown
                      :content
                      str/trim
                      not-empty)]
    (if (:share? block)
      (format "<div style=\"white-space: pre-line;\">%s</div>"
              block-str)
      block-str)))

(defmethod render-block "attachment"
  render-block--attachment
  [block]
  (let [post-filename (::filename block)
        {alt :altText
         id :attachmentId
         kind :kind
         file-url :fileURL} (:attachment block)
        [_ filename] (str/split file-url (re-pattern id))
        filename (-> (subs filename 1)
                     (str/replace "%20" " ")
                     (str/replace "%40" "@"))
        img-filename (-> filename
                         (str/replace "%C2%A0" " ")
                         (str/replace "%E2%80%AF" " ")
                         (str/replace "%E2%80%87" " "))
        img-path (io/file "content" "img" "cohost-mirror" post-filename img-filename)
        src-filename (-> filename
                         (str/replace "%C2%A0" (str \u2024))
                         (str/replace "%E2%80%AF" (str \u202F))
                         (str/replace "%E2%80%87" (str \u2007)))
        src-path (fs/expand-home (fs/path "~" "personal" "cohost-dl" "out" "rc" "attachment" id src-filename))]
    (when-not (fs/exists? src-path)
      (println src-path (fs/exists? src-path))
      (prn img-path (fs/exists? img-path))
      (prn :id id
           :combined [(not (fs/exists? img-path))
               (fs/exists? src-path)])
      )
    (when (and (not (fs/exists? img-path))
               (fs/exists? src-path))
      (println "copying" (str img-path))
      (io/make-parents img-path)
      (fs/copy src-path img-path {:replace-existing true}))
    (case kind
      "image" (format "![%s](/img/cohost-mirror/%s/%s)"
                      (or alt "image")
                      post-filename
                      (c/url-encode img-filename))
      "audio" (format "[%s](/img/cohost-mirror/%s/%s)"
                      (or alt "audio")
                      post-filename
                      (c/url-encode img-filename)))))

(defmethod render-block "attachment-row"
  render-block--attachment-row
  [block]
  (->> (:attachments block)
       (map #(assoc % ::filename (::filename block)))
       (map render-block)
       (str/join "\n\n")
       (str/trim)
       (not-empty)))

(defmethod render-block "ask"
  render-block--ast
  [block]
  (format "**@%s** asked:\n> %s"
          (or (-> block :ask :askingProject :handle)
              "Anonymous")
          (-> block :ask :content str/trim)))

(defn combine-blocks [blocks]
  (->> blocks
       (keep render-block)
       (str/join "\n\n")
       (str/trim)
       (not-empty)))

(defn render-body [post]
  (let [shares (not-empty (:shares post))
        body (->> (:blocks post)
                  (map #(assoc % ::filename (:filename post)))
                  combine-blocks)]
    (if shares
      (str "**@noahtheduke** posted:\n\n" body)
      body)))

(defn render-share [share]
  (let [raw-blocks (->> (:blocks share)
                        (map #(assoc % :share? true)))]
    (when-let [blocks (->> raw-blocks
                           (map #(assoc % ::filename (::filename share)))
                           combine-blocks)]
      {:author (-> share :postingProject :handle)
       :plain-body (when (->> (:blocks share)
                              (map :type)
                              (filter #{"attachment" "attachment-row"})
                              empty?)
                     (not-empty (:plainTextBody share)))
       :body blocks})))

(defn build-shares [post]
  (->> (:shareTree post)
       (map #(assoc % ::filename (:filename post)))
       (keep render-share)
       seq))

(def template (slurp (io/file "resources" "cohost_template.md")))

(defn render-post [post]
  (let [rendered-post (str (str/trim (selmer/render template post)) "\n")
        post-filename (str (:filename post) ".md")
        post-path (io/file "content" "md" "cohost-archive" post-filename)]
    (io/make-parents post-path)
    (spit post-path rendered-post)))

(defn convert []
  (fs/delete-tree (io/file "content" "md" "cohost-archive"))
  (->> posts
       (remove :pinned)
       (filter post-filter)
       (map #(assoc % :shares (build-shares %)))
       (map #(assoc % :body (render-body %)))
       (run! render-post)))

(comment
  (convert))
