(ns cryogen.core
  (:require
   [cryogen-core.compiler :refer [compile-assets-timed]]
   [cryogen-core.plugins :refer [load-plugins]]
   [cryogen-core.util :as util]
   [cryogen.server :refer [transform-html]]))

(defn -main []
  (load-plugins)
  (with-redefs [util/trimmed-html-snippet transform-html]
    (compile-assets-timed))
  (System/exit 0))
