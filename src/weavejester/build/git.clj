(ns weavejester.build.git
  (:require [clojure.string :as str]
            [clojure.java.shell :as sh]))

(defn- git [& args]
  (some-> (apply sh/sh "git" args) :out str/trim))

(defn default-version []
  (git "describe" "--exact-match" "--abbrev=0"))

(defn git-head []
  (git "rev-parse" "HEAD"))

(defn git-origin []
  (git "config" "--get" "remote.origin.url"))

(defn- parse-github-url [url]
  (or (re-matches #"(?:[A-Za-z-]{2,}@)?github.com:([^/]+)/([^/]+).git" url)
      (re-matches #"[^:]+://(?:[A-Za-z-]{2,}@)?github.com/([^/]+)/([^/]+?)(?:.git)?" url)))

(defn- github-urls [url]
  (when-let [[_ user repo] (parse-github-url url)]
    {:public-clone (str "git://github.com/"     user "/" repo ".git")
     :dev-clone    (str "ssh://git@github.com/" user "/" repo ".git")
     :browse       (str "https://github.com/"   user "/" repo)}))

(defn github-scm-map []
  (try
    (let [origin (git-origin)
          head   (git-head)
          urls   (github-urls origin)]
      (cond-> {:url (:browse urls)}
        (:public-clone urls) (assoc :connection (str "scm:git:" (:public-clone urls)))
        (:dev-clone urls)    (assoc :developerConnection (str "scm:git:" (:dev-clone urls)))
        head                 (assoc :tag head)))
    (catch java.io.FileNotFoundException _)))
