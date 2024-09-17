(ns cryogen.cohost
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [ring.util.codec :as c]
   [selmer.parser :as selmer]
   [medley.core :as m]))

(def good-posts
  #{;; original list
    "https://cohost.org/noahtheduke/post/3252059-money-handling-when"
    "https://cohost.org/noahtheduke/post/3566741-thoughts-on-being-po"
    "https://cohost.org/noahtheduke/post/2842001-pixar-movie-rankings"
    "https://cohost.org/noahtheduke/post/4225466-gender-feelings"
    "https://cohost.org/noahtheduke/post/3201474-at-this-point-i-don"
    ;; new good posts
    "https://cohost.org/noahtheduke/post/4962364-buying-digital-music"
    "https://cohost.org/noahtheduke/post/4738263-the-oldest-house"
    ;; new additions
    "https://cohost.org/noahtheduke/post/4867618-to-be-a-pedant"
    "https://cohost.org/noahtheduke/post/7392136-cw-suicidal-ideatio"
    "https://cohost.org/noahtheduke/post/6365357-i-dropped-my-iphone"
    "https://cohost.org/noahtheduke/post/6120035-just-look-at-this-fu"
    "https://cohost.org/noahtheduke/post/6002566-only-through-repetit"
    "https://cohost.org/noahtheduke/post/5500275-mental-math-pseudo-r"
    "https://cohost.org/noahtheduke/post/5434180-in-the-comment-of-th"
    "https://cohost.org/noahtheduke/post/5174669-looks-bad-tastes-gr"
    "https://cohost.org/noahtheduke/post/5168099-i-think-a-lot-about"
    "https://cohost.org/noahtheduke/post/5099857-the-main-goal-of-th"
    "https://cohost.org/noahtheduke/post/5075781-hot-take-spellcheck"
    "https://cohost.org/noahtheduke/post/4997698-if-daylight-saving-t"
    "https://cohost.org/noahtheduke/post/4944983-name-ideas"
    "https://cohost.org/noahtheduke/post/4687878-learned-about-the-ta"
    "https://cohost.org/noahtheduke/post/4253502-i-want-a-tattoo-that"
    "https://cohost.org/noahtheduke/post/3905105-super-hexagon-by-t"
    "https://cohost.org/noahtheduke/post/3895180-encanto-isn-t-good"
    "https://cohost.org/noahtheduke/post/3746439-i-ve-used-my-switch"
    "https://cohost.org/noahtheduke/post/3656388-i-ended-up-buying"
    "https://cohost.org/noahtheduke/post/3611722-the-funny-thing-abou"
    "https://cohost.org/noahtheduke/post/3296132-i-don-t-know-how-to"
    "https://cohost.org/noahtheduke/post/3271389-george-lucas-selling"
    "https://cohost.org/noahtheduke/post/3261127-ynab-privacy"
    "https://cohost.org/noahtheduke/post/3213204-on-the-one-hand-i-d"
    "https://cohost.org/noahtheduke/post/3070906-bought-some-more-mus"
    "https://cohost.org/noahtheduke/post/2901583-if-you-re-gonna-crit"
    "https://cohost.org/noahtheduke/post/2848499-got-a-new-shirt"
    "https://cohost.org/noahtheduke/post/2837714-how-do-you-come-up-w"
    "https://cohost.org/noahtheduke/post/2812517-y-all-ever-think-abo"
    "https://cohost.org/noahtheduke/post/2801315-jumbled-sexuality-th"
    "https://cohost.org/noahtheduke/post/2646635-looking-for-a-live-a"
    "https://cohost.org/noahtheduke/post/2598417-old-memories"
    "https://cohost.org/noahtheduke/post/2546608-cw-gamergate"
    "https://cohost.org/noahtheduke/post/2195959-bogos-binted-was"
    "https://cohost.org/noahtheduke/post/1950335-i-understand-that-co"
    "https://cohost.org/noahtheduke/post/1215841-whomst-can-i-talk-to"
    "https://cohost.org/noahtheduke/post/1645249-went-to-a-concert-an"
    "https://cohost.org/noahtheduke/post/1360941-cw-suicide"
    "https://cohost.org/noahtheduke/post/1249274-company-fired-some-o"
    ;; friends at the table
    "https://cohost.org/noahtheduke/post/5249141-palisade-41-is-reall"
    "https://cohost.org/noahtheduke/post/5050236-i-hope-i-never-forge"
    ;; ttrpgs
    "https://cohost.org/noahtheduke/post/5032286-reign-2e"
    "https://cohost.org/noahtheduke/post/5094613-more-reign-2e-though"
    "https://cohost.org/noahtheduke/post/5058467-i-forgot-about-the"
    "https://cohost.org/noahtheduke/post/4378205-just-popped-into-my"
    ;; memes/jokes
    "https://cohost.org/noahtheduke/post/407951-eggbug-eggbug"
    "https://cohost.org/noahtheduke/post/5656627-the-bene-gesserit-l"
    "https://cohost.org/noahtheduke/post/4174530-trans-jerry-i-have"
    "https://cohost.org/noahtheduke/post/7509572-hole-w-t-we-gardena"
    "https://cohost.org/noahtheduke/post/7272516-fanfic-of-william-gi"
    "https://cohost.org/noahtheduke/post/5136426-may-thy-knife-chip-a"
    "https://cohost.org/noahtheduke/post/4904173-eggs-bene-gesserit"
    ;; taylor swift
    "https://cohost.org/noahtheduke/post/4030418-everyone-i-know-post"
    "https://cohost.org/noahtheduke/post/4047732-i-spent-multiple-hou"
    "https://cohost.org/noahtheduke/post/4032503-here-s-the-chart"
    "https://cohost.org/noahtheduke/post/1887941-album-is-easy-lover"
    "https://cohost.org/noahtheduke/post/2436561-this-album-is-so-fuc"
    "https://cohost.org/noahtheduke/post/1798662-saw-taylor-swift-las"
    ;https://cohost.org/noahtheduke/post/; programming
    "https://cohost.org/noahtheduke/post/7369102-ocaml-wishlist"
    "https://cohost.org/noahtheduke/post/7108863-they-merged-it-my-c"
    "https://cohost.org/noahtheduke/post/7020673-learning-a-new-progr"
    "https://cohost.org/noahtheduke/post/6286441-clojure-enterprise"
    "https://cohost.org/noahtheduke/post/5980051-i-think-i-severely"
    "https://cohost.org/noahtheduke/post/5793881-open-source-maintena"
    "https://cohost.org/noahtheduke/post/5258703-ruminations-on-techn"
    "https://cohost.org/noahtheduke/post/5018957-yuki-s-vacation-an"
    "https://cohost.org/noahtheduke/post/4896592-what-is-this-data-st"
    "https://cohost.org/noahtheduke/post/4445263-viscerally-annoyed"
    "https://cohost.org/noahtheduke/post/4086578-tyranny-of-the-blank"
    "https://cohost.org/noahtheduke/post/2711345-if-your-programming"
    "https://cohost.org/noahtheduke/post/2400662-mild-success"
    "https://cohost.org/noahtheduke/post/1789527-in-increasing-order"
    "https://cohost.org/noahtheduke/post/161270-i-commented-on-someo"
    })

(def skip-posts
  #{"https://cohost.org/noahtheduke/post/7300465-i-should-probably-st"
    "https://cohost.org/noahtheduke/post/5145221-especially-when-your"
    "https://cohost.org/noahtheduke/post/7148525-how-i-feel-when-my-w"
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
  (and (not (skip-posts (:singlePostPageUrl post)))
       (or (good-posts (:singlePostPageUrl post))
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
       (map #(dissoc % :postingProject))
       (map #(assoc % :shareTree (filter (fn [st] (seq (:blocks st))) (:shareTree %))))
       ; (m/find-first #(str/includes? (:headline %) "blog"))
       (m/find-first #(= "4027959-may-hands-me-a-fancy" (:filename %)))
       ))

(defmulti render-block "convert block to html" :type)

(defn combine-blocks [blocks]
  (->> blocks
       (keep render-block)
       (str/join "\n\n")
       (str/trim)
       (not-empty)))

(defmethod render-block "markdown"
  render-block--markdown
  [block]
  (let [block-str (-> block
                      :markdown
                      (:content "")
                      not-empty)]
    (when block-str
      (if (str/includes? block-str " | ")
        block-str
        (str/join "  \n" (str/split block-str #"(?<!\p{Space})\n(?![\p{Space}\W\p{Digit}])"))))))

(comment
  (render-block
   {:type "markdown"
    :markdown
    {:content
     "* many such things\n* many more such things\nthis is some bullshit"}}))

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
       (combine-blocks)))

(defmethod render-block "ask"
  render-block--ast
  [block]
  (format "**@%s** asked:\n> %s"
          (or (-> block :ask :askingProject :handle)
              "Anonymous")
          (-> block :ask :content str/trim)))

(defn render-body [post]
  (let [shares (not-empty (:shares post))
        body (->> (:blocks post)
                  (map #(assoc % ::filename (:filename post)))
                  (combine-blocks))]
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
