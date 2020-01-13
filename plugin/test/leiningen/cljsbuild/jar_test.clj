(ns leiningen.cljsbuild.jar-test
  (:require [clojure.test :refer [deftest is testing]]
            [leiningen.cljsbuild.jar :as jar]
            [me.raynes.fs :as fs]))

(defn- make-bytes [s]
  byte-array (map (comp byte int) s))

(deftest path-filespecs
  (let [root "/a/b"
        basename-1 "file1"
        basename-2 "file2"
        path-1 (jar/join-paths root basename-1)
        path-2 (jar/join-paths root basename-2)]
    (with-redefs [fs/iterate-dir (fn [path]
                                   [[root [] [basename-1 basename-2]]])
                  jar/file-bytes (fn [path]
                                   (make-bytes path))]
      (is (= (jar/path-filespecs root) [{:type :bytes
                                         :path basename-1
                                         :bytes (make-bytes path-1)}
                                        {:type :bytes
                                         :path basename-2
                                         :bytes (make-bytes path-2)}])))))

(defn make-jar-directory [root basename contents]
  {:root (jar/join-paths root basename)
   :filespec
     {:type :bytes
      :path basename
      :bytes (make-bytes contents)}})

(defn make-jar-directories [names]
  (let [directories (for [n (map name names)]
                      (make-jar-directory
                        (jar/join-paths "/root" n)
                        (str "file-" n)
                        (str "contents-" n)))]
    (into {} (map vector names directories))))

(deftest get-filespecs
  (let [entries (make-jar-directories [:a :b])
        project {:cljsbuild
                  {:builds
                     [{:source-paths [(get-in entries [:a :root])]
                       :jar true}
                      {:source-paths [(get-in entries [:b :root])]
                       :jar true}
                      {:source-paths ["not-in-jar"]}]}}
        root-a (get-in entries [:a :root])
        root-b (get-in entries [:b :root])]
    (with-redefs [jar/path-filespecs (fn [path]
                                       (condp = path
                                         root-a [(get-in entries [:a :filespec])]
                                         root-b [(get-in entries [:b :filespec])]))]
      (is (= (jar/get-filespecs project) (map :filespec (vals entries)))))))
