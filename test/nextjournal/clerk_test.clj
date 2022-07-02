(ns nextjournal.clerk-test
  (:require [babashka.fs :as fs]
            [clojure.java.browse :as browse]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [matcher-combinators.test :refer [match?]]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.hashing :as hashing]
            [nextjournal.clerk.parser :as parser]
            [nextjournal.clerk.view :as view]
            [nextjournal.clerk.viewer :as viewer])
  (:import (java.io File)))

(deftest url-canonicalize
  (testing "canonicalization of file components into url components"
    (let [dice (str/join (File/separator) ["notebooks" "dice.clj"])]
      (is (= (#'clerk/path-to-url-canonicalize dice) (str/replace dice  (File/separator) "/"))))))

(deftest static-app
  (let [url* (volatile! nil)]
    (with-redefs [clojure.java.browse/browse-url (fn [path]
                                                   (vreset! url* path))]
      (testing "browser receives canonical url in this system arch"
        (fs/with-temp-dir [temp {}]
          (let [expected (-> (str/join (java.io.File/separator) [(.toString temp) "index.html"])
                             (str/replace (java.io.File/separator) "/"))]
            (clerk/build-static-app! {:paths ["notebooks/hello.clj"]
                                      :out-path temp})
            (is (= expected @url*))))))))

(deftest expand-paths-test
  (let [paths (clerk/expand-paths ["notebooks/*clj"])]
    (is (> (count paths) 25))
    (is (every? #(str/ends-with? % ".clj") paths))))

