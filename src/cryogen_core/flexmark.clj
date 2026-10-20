(ns cryogen-core.flexmark
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [cryogen-core.markup :refer [markup-registry rewrite-hrefs]]
   [malli.core :as m]
   [malli.transform :as mt]
   [medley.core :refer [update-existing]])
  (:import
   (com.vladsch.flexmark.ext.aside AsideExtension)
   (com.vladsch.flexmark.ext.attributes AttributesExtension)
   (com.vladsch.flexmark.ext.emoji EmojiExtension EmojiShortcutType)
   (com.vladsch.flexmark.ext.footnotes FootnoteExtension)
   (com.vladsch.flexmark.ext.gfm.strikethrough StrikethroughSubscriptExtension)
   (com.vladsch.flexmark.ext.gfm.tasklist TaskListExtension)
   (com.vladsch.flexmark.ext.superscript SuperscriptExtension)
   (com.vladsch.flexmark.ext.tables TablesExtension)
   (com.vladsch.flexmark.ext.yaml.front.matter AbstractYamlFrontMatterVisitor YamlFrontMatterExtension)
   (com.vladsch.flexmark.html HtmlRenderer)
   (com.vladsch.flexmark.parser Parser)
   (com.vladsch.flexmark.util.ast Node)
   (com.vladsch.flexmark.util.data MutableDataSet)
   (cryogen_core.markup Markup RenderSteps)
   (java.util ArrayList Collection)))

(set! *warn-on-reflection* true)

(defn convert-metadata [metadata]
  (walk/postwalk (fn [x]
                   (if (map? x)
                     (persistent!
                       (reduce-kv
                         (fn [m k v]
                           (let [k' (if (string? k) (keyword k) k)]
                             (if (= :tags k')
                               (assoc! m k' v)
                               (assoc! m k' (first v)))))
                         (transient {})
                         x))
                     x))
    metadata))

(def quoted-string-transformer
  (mt/transformer
    {:name :quoted-string-transformer
     :encoders {}
     :decoders {:string (fn [s]
                          (if (and (= \' (String/.charAt s 0))
                                (= \' (String/.charAt s (dec (count s)))))
                            (subs s 1 (dec (count s)))
                            s))}}))

(def Metadata
  (m/schema
   [:map
    [:title :string]
    [:layout {:optional true} :keyword]
    [:date {:optional true} :string]
    [:author {:optional true} :string]
    [:tags {:optional true} [:sequential :string]]
    [:toc {:optional true} [:or [:enum :ol :ul] :boolean]]
    [:draft {:optional true} :boolean]
    [:home {:optional true} :boolean]
    [:page-index {:optional true} :int]
    [:navbar {:optional true} :boolean]]))

(def string-strip-quotes
  (mt/transformer mt/string-transformer quoted-string-transformer))

(def coerce-metadata
  (m/coercer Metadata string-strip-quotes))

(defrecord MD [dir exts parser renderer]
  Markup
  (dir [this] dir)
  (exts [this] exts)
  (render-fn [this] (throw (ex-info "don't use this" {})))
  RenderSteps
  (parse [this file _config]
    (->> (java.io.BufferedReader. (io/reader (fs/file file)))
      (line-seq)
      (str/join "\n")
      (Parser/.parse parser)))
  (metadata [this document _config]
    (let [yaml (let [visitor (new AbstractYamlFrontMatterVisitor)]
                 (.visit visitor ^Node document)
                 (not-empty (convert-metadata (into {} (.getData visitor)))))
          clj (when-not yaml
                (let [child (Node/.getFirstChild document)
                      m (-> (Node/.getChars child)
                          (str)
                          (java.io.StringReader.)
                          (java.io.PushbackReader.)
                          (read))]
                  (when m
                    (Node/.unlink child)
                    m)))
          metadata (update-existing (or yaml clj) :tags vec)]
      {:document document
       :metadata (coerce-metadata metadata)}))
  (render [this document _metadata config]
    (->> document
      (HtmlRenderer/.render renderer)
      (rewrite-hrefs (:blog-prefix config)))))

(defn markdown []
  (let [extensions [(AsideExtension/create)
                    (AttributesExtension/create)
                    (EmojiExtension/create)
                    (FootnoteExtension/create)
                    (StrikethroughSubscriptExtension/create)
                    (SuperscriptExtension/create)
                    (TablesExtension/create)
                    (TaskListExtension/create)
                    (YamlFrontMatterExtension/create)]
        options (-> (MutableDataSet.)
                  (.set EmojiExtension/ATTR_IMAGE_CLASS "emoji")
                  (.set EmojiExtension/USE_SHORTCUT_TYPE EmojiShortcutType/ANY_GITHUB_PREFERRED)
                  (.set HtmlRenderer/FENCED_CODE_LANGUAGE_CLASS_PREFIX "")
                  (.set HtmlRenderer/GENERATE_HEADER_ID true)
                  (.set HtmlRenderer/RENDER_HEADER_ID true)
                  (.set Parser/EXTENSIONS (ArrayList. ^Collection extensions)))
        parser (.build (Parser/builder options))
        renderer (.build (HtmlRenderer/builder options))]
    (->MD "md" #{".md"} parser renderer)))

(comment
  (type (markdown)))

(defn init []
  (reset! markup-registry [(markdown)]))
