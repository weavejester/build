(ns weavejester.build.git
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(defn- git [& args]
  (b/process {:command-args (into ["git"] args)
              :out :capture
              :err :ignore}))

(defn default-version []
  (some-> (git "describe" "--exact-match" "--abbrev=0") :out str/trim))

(defn git-head []
  (some-> (git "rev-parse" "HEAD") :out str/trim))

(defn git-origin []
  (some-> (git "config" "--get" "remote.origin.url") :out str/trim))

(defn- parse-github-url [url]
  (or (re-matches #"(?:[A-Za-z-]{2,}@)?github.com:([^/]+)/([^/]+).git" url)
      (re-matches #"[^:]+://(?:[A-Za-z-]{2,}@)?github.com/([^/]+)/([^/]+?)(?:.git)?" url)))

(defn- github-urls [url]
  (if-let [[_ user repo] (parse-github-url url)]
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
