(ns nextjournal.clerk.view
  (:require [nextjournal.viewer :as v]
            [hiccup.page :as hiccup]
            [clojure.walk :as w]))

(defn doc->viewer
  ([doc] (doc->viewer {} doc))
  ([{:keys [inline-results?] :or {inline-results? false}} doc]
   (into (v/view-as :clerk/notebook [])
         (mapcat (fn [{:as x :keys [type text result]}]
                   (case type
                     :markdown [(v/view-as :markdown text)]
                     :code (cond-> [(v/view-as :code text)]
                             (contains? x :result)
                             (conj (if (and (not inline-results?)
                                            (instance? clojure.lang.IMeta result)
                                            (contains? (meta result) :blob/id)
                                            (not (v/registration? result)))
                                     (v/view-as :clerk/blob (v/describe result))
                                     result))))))
         doc)))


#_(doc->viewer (nextjournal.clerk/eval-file "notebooks/elements.clj"))

(defn ex->viewer [e]
  (into ^{:nextjournal/viewer :notebook}
        [(v/view-as :code (pr-str (Throwable->map e)))]))


#_(doc->viewer (nextjournal.clerk/eval-file "notebooks/elements.clj"))

(defn var->data [v]
  (v/view-as :clerk/var (symbol v)))

#_(var->data #'var->data)

(defn fn->data [_]
  {:nextjournal/value 'fn :nextjournal/type-key :fn})

#_(fn->data (fn []))

(defn make-printable [x]
  (cond-> x
    (var? x) var->data
    (meta x) (vary-meta #(w/prewalk make-printable %))
    (fn? x) fn->data))

#_(meta (make-printable ^{:f (fn [])} []))

(defn ->edn [x]
  (binding [*print-meta* true
            *print-namespace-maps* false]
    (pr-str (w/prewalk make-printable x))))

#_(->edn [:vec (with-meta [] {'clojure.core.protocols/datafy (fn [x] x)}) :var #'->edn])

(defonce ^{:doc "Load dynamic js from shadow or static bundle from cdn."}
  live-js?
  false)


(defn ->html [{:keys [conn-ws?] :or {conn-ws? true}} viewer]
  (hiccup/html5
   [:head
    [:meta {:charset "UTF-8"}]
    (hiccup/include-css
     (if live-js?
       "https://cdn.dev.nextjournal.com:8888/build/stylesheets/viewer.css"
       "https://cdn.nextjournal.com/data/QmZc2Rhp7GqGAuZFp8SH6yVLLh3sz3cyjDwZtR5Q8PGcye?filename=viewer-a098a51e8ec9999fae7673b325889dbccafad583.css&content-type=text/css"))

    (hiccup/include-js
     (if live-js?
       "/js/out/viewer.js"
       "https://cdn.nextjournal.com/data/Qmc5rjhjB6irjrJnCgsB4JU3Vvict3DEHeV4Zvq7GJQv4F?filename=viewer.js&content-type=application/x-javascript"))]
   [:body
    [:div#app]
    [:script "nextjournal.viewer.notebook.mount(document.getElementById('app'))
nextjournal.viewer.notebook.reset_state(nextjournal.viewer.notebook.read_string(" (-> viewer ->edn pr-str) "))"]
    (when conn-ws?
      [:script "const ws = new WebSocket(document.location.origin.replace(/^http/, 'ws') + '/_ws')
ws.onmessage = msg => nextjournal.viewer.notebook.reset_state(nextjournal.viewer.notebook.read_string(msg.data))"])]))


(defn doc->html [doc]
  (->html {} (doc->viewer {} doc)))

(defn doc->static-html [doc]
  (->html {:conn-ws? false} (doc->viewer {:inline-results? true} doc)))

#_(let [out "test.html"]
    (spit out (doc->static-html (nextjournal.clerk/eval-file "notebooks/pagination.clj")))
    (clojure.java.browse/browse-url out))
