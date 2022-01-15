(ns weavejester.build.tool
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as deploy]
            [weavejester.build.project :as p]
            [weavejester.build.write-pom :as pom]))

(defn- find-self-version []
  (let [basis (-> (System/getProperty "clojure.basis") slurp edn/read-string)
        lib   (-> basis :libs (get 'com.github.weavejester/build))]
    (select-keys lib [:git/sha :git/tag])))

(defn- replace-template-vars [text vars]
  (reduce-kv (fn [s k v] (str/replace s k v)) text vars))

(defn- write-file
  ([src dest]
   (write-file src dest identity))
  ([src dest xform]
   (if (.exists (io/file b/*project-root* dest))
     (println "Skipping" dest "as it already exists")
     (let [text (xform (slurp (io/resource src)))]
       (b/write-file {:path dest, :string text})
       (println "Written " dest)))))

(defn- write-template [src dest vars]
  (write-file src dest #(replace-template-vars % vars)))

(defn- write-bb-edn []
  (write-template "weavejester/build/bb.edn.tmpl" "bb.edn"
                  {"VERSION" (pr-str (find-self-version))}))

(defn- write-project-edn []
  (write-template "weavejester/build/project.edn.tmpl" "project.edn"
                  {"LIBRARY_NAME" "test"}))

(defn- write-tests-edn []
  (write-file "weavejester/build/tests.edn" "tests.edn" ))

(defn init [_]
  (write-bb-edn)
  (write-project-edn)
  (write-tests-edn))

(defn- copy-to-target [{:keys [src-dirs resource-dirs class-dir]}]
  (b/copy-dir {:src-dirs   (into src-dirs resource-dirs)
               :target-dir class-dir}))

(defn jar [_]
  (doto (assoc @p/project :basis (b/create-basis))
    (pom/write-pom)
    (copy-to-target)
    (b/jar)))

(defn uberjar [_]
  (doto (assoc @p/project :basis (b/create-basis))
    (pom/write-pom)
    (copy-to-target)
    (b/compile-clj)
    (b/uber)))

(defn deploy [m]
  (jar m)
  (deploy/deploy
   {:artifact       (:jar-file @p/project)
    :pom-file       (io/file (pom/pom-dir @p/project) "pom.xml")
    :installer      :remote
    :sign-releases? true}))

(defn evalstr [{:keys [sexp]}]
  (eval sexp))
