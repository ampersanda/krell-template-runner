; #!/usr/bin/env bb

; NodeJS is required by react-native in this project

(ns runner
  (:require [clojure.java.shell :as shell]
            [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  [["-h" "--help" "Show help"]])

;(defn >>>-)

(defn gen-rn-project [project-name]
  (let [{:keys [:exit :err :out]} (shell/sh "npx" "react-native" "init" project-name)]
    (if (zero? exit)
      out
      (do (println "ERROR:" err)
        (System/exit 1)))))

(defn gen-project [{:keys [options arguments]}]
  )

(let [{:keys [options arguments summary errors] :as args} (parse-opts *command-line-args* cli-options)]
  ;; guard errors
  (cond
    errors (println errors "\n" summary)
    (:help options) (println summary)
    arguments (gen-project args)))
