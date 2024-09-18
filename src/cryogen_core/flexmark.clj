(ns cryogen-core.flexmark
  (:require
   [clojure.string :as str]
   [cryogen-core.markup :refer [markup-registry rewrite-hrefs]])
  (:import
   (com.vladsch.flexmark.ext.aside AsideExtension)
   (com.vladsch.flexmark.ext.attributes AttributesExtension)
   (com.vladsch.flexmark.ext.emoji EmojiExtension)
   (com.vladsch.flexmark.ext.footnotes FootnoteExtension)
   (com.vladsch.flexmark.ext.gfm.strikethrough StrikethroughExtension)
   (com.vladsch.flexmark.ext.superscript SuperscriptExtension)
   (com.vladsch.flexmark.ext.tables TablesExtension)
   (com.vladsch.flexmark.html HtmlRenderer)
   (com.vladsch.flexmark.parser Parser)
   (com.vladsch.flexmark.util.data MutableDataSet)
   (cryogen_core.markup Markup)
   (java.util ArrayList)))

(defn markdown
  "Returns a Markdown (CommonMark) implementation of the Markup protocol."
  []
  (let [extensions [(FootnoteExtension/create)
                    (StrikethroughExtension/create)
                    (SuperscriptExtension/create)
                    (TablesExtension/create)
                    (AttributesExtension/create)
                    (EmojiExtension/create)
                    (AsideExtension/create)]
        options (-> (MutableDataSet.)
                    (.set Parser/EXTENSIONS (ArrayList. extensions))
                    (.set HtmlRenderer/FENCED_CODE_LANGUAGE_CLASS_PREFIX "")
                    (.set HtmlRenderer/GENERATE_HEADER_ID true)
                    (.set HtmlRenderer/RENDER_HEADER_ID true))
        parser (.build (Parser/builder options))
        renderer (.build (HtmlRenderer/builder options))]
   (reify Markup
    (dir [_this] "md")
    (exts [_this] #{".md"})
    (render-fn [_this]
      (fn [rdr config]
        (->> (java.io.BufferedReader. rdr)
             (line-seq)
             (str/join "\n")
             (.parse parser)
             (.render renderer)
             (rewrite-hrefs (:blog-prefix config))))))))

(defn init []
  (swap! markup-registry conj (markdown)))
