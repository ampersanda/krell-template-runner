#!/usr/bin/env bb

; NodeJS is required by react-native in this project
; clj is required
; pod is optional

(ns runner
  (:require [clojure.java.shell :as shell]
            [clojure.tools.cli :refer [parse-opts]]))

(defn camelcase-to-delimitered [k delimeter]
  (->> (clojure.string/split k #"(?=[A-Z])")
       (map clojure.string/lower-case)
       (clojure.string/join delimeter)))

(def documentation
  "https://github.com/vouch-opensource/krell/wiki/Reagent-Tutorial#using-the-repl")

(def cli-options
  [;["-v" "--version" "Define specific React Native version"]
    ["-h" "--help" "Show help"]])

(defn run-shell
  ([shell-command]
   (let [{:keys [exit err out]} shell-command]
     (if (zero? exit)
       out
       (do (println "ERROR:" err)
         (System/exit 1)))))
  ([shell-command fn-error]
   (let [{:keys [exit err out]} shell-command]
     (println exit)
     (if (zero? exit)
       out
       (fn-error shell-command))))
  ([shell-command fn-success fn-error]
   (let [{:keys [exit]} shell-command]
     (if (zero? exit)
       (fn-success shell-command)
       (fn-error shell-command)))))

(defn gen-rn-project [{:keys [arguments]}]
  "Generate react-native project using [npx react-native init ProjectName]"
  (let [project-name (first arguments)]
    (println (str "🛠 Building React Native " project-name " project"))
    (println (str "ℹ️ (This may take a while. You know npm better than me)\n"))
    (run-shell (shell/sh "npx" "react-native" "init" project-name))))

(defn install-rn-deps [project-name]
  "npm install"
  (println (str "🛠 Installing Node dependencies"))
  (run-shell (shell/sh "npm" "install" :dir project-name)))

(defn make-edns [project-name]
  (println "🛠 Initializing dependency files")
  (spit (str project-name "/deps.edn") (slurp "deps-template.edn"))
  (spit (str project-name "/build.edn")
        (clojure.string/replace
         (slurp "build-template.edn") #"\$TEMPLATE\$" (str (camelcase-to-delimitered project-name "_") ".core"))))

(defn install-deps [project-name]
  (println "🛠 Installing Clojure dependencies")
  (run-shell (shell/sh "clj" "-m" "cljs.main" "--install-deps" :dir project-name)))

(defn run-pod-install [project-name]
  "pod install"
  (run-shell (shell/sh "which" "pod")
             (fn [_]
               (println "🛠 Running pod install")
               (run-shell (shell/sh "pod" "install" :dir (str project-name "/ios"))))
             (fn [{:keys [exit]}]
               (when (= exit 1)
                 (println "⚠️ Pod is not installed. Skipping ...")))))

(defn write-clojure-file [project-name]
  (let [file-name        (str project-name "/src/" (camelcase-to-delimitered project-name "_") "/core.cljs")
        content          (slurp "core-template.cljs")
        adjusted-content (clojure.string/replace content #"\$TEMPLATE\$" (str (camelcase-to-delimitered project-name "-") ".core"))]
    (println "🛠 Preparing ClojureScript files")
    (clojure.java.io/make-parents file-name)
    (spit file-name adjusted-content)))

(defn setup-clojure-env [project-name]
  (make-edns project-name)
  (install-deps project-name)
  (run-pod-install project-name)
  (write-clojure-file project-name))

(defn gen-project [{:keys [options arguments summary] :as args}]
  (let [project-name           (first arguments)]
    (if-let [is-project-name-valid? (re-find #"^\w+$" project-name)]
      (do
        (gen-rn-project args)
        (install-rn-deps project-name)
        (setup-clojure-env project-name)
        (println
         (str "\n"
              "👉 You're good to go!!\n\n"
              "  $ cd " project-name "\n"
              "  $ clj -m krell.main -co build.edn -c -r\n"
              "\n"
              "  Open New Terminal Tab and the run\n"
              "  $ npx react-native start"
              "\n"
              "  Open New Terminal Tab again and the run \n"
              "  For iOS :"
              "  $ npx react-native run-ios\n"
              "  For Android :"
              "  $ npx react-native run-android\n"
              "\n"
              "👉 Production Build\n"
              "  $ cd " project-name "\n"
              "  $ clj -m krell.main -v -co build.edn -O advanced -c\n\n"
              "Read more: " documentation)))
      (do
        (println
         (str "ERROR: Error creating \"" project-name "\" project. Consider using CamelCase name. e.g. AwesomeProject"
              "\n"
              "\n"
              summary))
        (System/exit 1)))))

(let [{:keys [options arguments summary errors] :as args} (parse-opts *command-line-args* cli-options)]
    (cond
      errors          (println errors "\n" summary)
      (:help options) (println summary)
      arguments       (gen-project args)))
