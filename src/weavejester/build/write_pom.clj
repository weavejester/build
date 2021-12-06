;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns weavejester.build.write-pom
  (:require [clojure.java.io :as jio]
            [clojure.string :as str]
            [clojure.data.xml :as xml]
            [clojure.data.xml.tree :as tree]
            [clojure.data.xml.event :as event]
            [clojure.zip :as zip]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [clojure.tools.deps.alpha.util.io :refer [printerrln]]
            [clojure.tools.build.api :as api]
            [clojure.tools.build.util.file :as file])
  (:import [java.io Reader]
           [clojure.data.xml.node Element]))

(xml/alias-uri 'pom "http://maven.apache.org/POM/4.0.0")

(defn- to-dep
  [[lib {:keys [mvn/version exclusions optional] :as coord}]]
  (let [[group-id artifact-id classifier] (maven/lib->names lib)]
    (if version
      (cond->
        [::pom/dependency
         [::pom/groupId group-id]
         [::pom/artifactId artifact-id]
         [::pom/version version]]

        classifier
        (conj [::pom/classifier classifier])

        (seq exclusions)
        (conj [::pom/exclusions
               (map (fn [excl]
                      [::pom/exclusion
                       [::pom/groupId (or (namespace excl) (name excl))]
                       [::pom/artifactId (name excl)]])
                 exclusions)])

        optional
        (conj [::pom/optional "true"]))
      (printerrln "Skipping coordinate:" coord))))

(defn- gen-deps [deps]
  [::pom/dependencies (map to-dep deps)])

(defn- gen-source-dir [path]
  [::pom/sourceDirectory path])

(defn- to-resource [resource]
  [::pom/resource [::pom/directory resource]])

(defn- gen-resources [rpaths]
  [::pom/resources (map to-resource rpaths)])

(defn- to-repo [[name repo]]
  [::pom/repository
   [::pom/id name]
   [::pom/url (:url repo)]])

(defn- gen-repos [repos]
  [::pom/repositories (map to-repo repos)])

(defn- gen-pom
  [{:keys [deps src-paths resource-paths repos group artifact version scm
           description url licenses]
    :or {version "0.1.0"}}]
  (let [[path & paths] src-paths]
    (xml/sexp-as-element
      [::pom/project
       {:xmlns "http://maven.apache.org/POM/4.0.0"
        (keyword "xmlns:xsi") "http://www.w3.org/2001/XMLSchema-instance"
        (keyword "xsi:schemaLocation") "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"}
       [::pom/modelVersion "4.0.0"]
       [::pom/packaging "jar"]
       [::pom/groupId group]
       [::pom/artifactId artifact]
       [::pom/version version]
       [::pom/name artifact]
       (when description
         [::pom/description description])
       (when url
         [::pom/url url])
       (when licenses
         [::pom/licenses
          (for [{:keys [name url]} licenses]
            [::pom/license
             [::pom/name name]
             [::pom/url url]])])
       (gen-deps deps)
       (when (or path (seq resource-paths))
         (when (seq paths) (apply printerrln "Skipping paths:" paths))
         [::pom/build
          (when path (gen-source-dir path))
          (when (seq resource-paths) (gen-resources resource-paths))])
       (gen-repos repos)
       (when scm
         (let [{:keys [connection developerConnection tag url]} scm]
           [::pom/scm
            (when connection [::pom/connection connection])
            (when developerConnection [::pom/developerConnection developerConnection])
            (when tag [::pom/tag tag])
            (when url [::pom/url url])]))])))

(defn- make-xml-element
  [{:keys [tag attrs] :as node} children]
  (with-meta
    (apply xml/element tag attrs children)
    (meta node)))

(defn- libs->deps [libs]
  (reduce-kv
    (fn [ret lib {:keys [dependents] :as coord}]
      (if (seq dependents)
        ret
        (assoc ret lib coord)))
    {} libs))

(defn- basis-repos [{:mvn/keys [repos]}]
  (remove #(= "https://repo1.maven.org/maven2/" (-> % val :url)) repos))

(defn meta-maven-path
  [params]
  (let [{:keys [lib]} params
        pom-file (jio/file "META-INF" "maven" (namespace lib) (name lib))]
    (.toString pom-file)))

(defn write-pom
  [{:keys [basis class-dir src-pom lib version scm src-dirs resource-dirs
           repos description url licenses]}]
  (let [{:keys [libs]} basis
        root-deps      (libs->deps libs)
        src-pom-file   (api/resolve-path (or src-pom "pom.xml"))
        repos          (or repos (basis-repos basis))
        pom            (gen-pom
                        (cond-> {:deps           root-deps
                                 :src-paths      src-dirs
                                 :resource-paths resource-dirs
                                 :repos          repos
                                 :group          (namespace lib)
                                 :artifact       (name lib)}
                          version     (assoc :version version)
                          scm         (assoc :scm scm)
                          description (assoc :description description)
                          url         (assoc :url url)
                          licenses    (assoc :licenses licenses)))
        class-dir-file (api/resolve-path class-dir)
        pom-dir        (meta-maven-path {:lib lib})
        pom-dir-file   (file/ensure-dir (jio/file class-dir-file pom-dir))]
    (spit (jio/file pom-dir-file "pom.xml") (xml/indent-str pom))
    (spit (jio/file pom-dir-file "pom.properties")
      (str/join (System/lineSeparator)
        ["# Generated by dev.weavejester/build"
         (format "# %tc"         (java.util.Date.))
         (format "version=%s"    version)
         (format "groupId=%s"    (namespace lib))
         (format "artifactId=%s" (name lib))]))))
