(ns leiningen.test.cljsbuild.jar
  (:use
    leiningen.cljsbuild.jar
    midje.sweet)
  (:require
    [me.raynes.fs :as fs]))

(defn- make-bytes [s]
  byte-array (map (comp byte int) s))

(fact
  (let [root "/a/b"
        basename-1 "file1"
        basename-2 "file2"
        path-1 (join-paths root basename-1)
        path-2 (join-paths root basename-2)
        bytes-1 (make-bytes "some-bytes")
        bytes-2 (make-bytes "other-bytes")]
    (path-filespecs root) => [{:type :bytes
                               :path basename-1
                               :bytes bytes-1}
                              {:type :bytes
                               :path basename-2
                               :bytes bytes-2}]
    (provided
      (fs/iterate-dir root) => [[root [] [basename-1 basename-2]]] :times 1
      (file-bytes path-1) => bytes-1 :times 1
      (file-bytes path-2) => bytes-2 :times 1)))

(defn make-jar-directory [root basename contents]
  {:root (join-paths root basename)
   :filespec
     {:type :bytes
      :path basename
      :bytes (make-bytes contents)}})

(defn make-jar-directories [names]
  (let [directories (for [n (map name names)]
                      (make-jar-directory
                        (join-paths "/root" n)
                        (str "file-" n)
                        (str "contents-" n)))]
    (into {} (map vector names directories))))

(fact
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
    (get-filespecs project) => (just (set (map :filespec (vals entries))))
    (provided
      (path-filespecs root-a) => [(get-in entries [:a :filespec])] :times 1
      (path-filespecs root-b) => [(get-in entries [:b :filespec])] :times 1)
    (get-filespecs project) => (just (set (map :filespec (vals entries))))
    (provided
     (path-filespecs root-a) => [(get-in entries [:a :filespec])] :times 1
     (path-filespecs root-b) => [(get-in entries [:b :filespec])] :times 1)))
