;#!/usr/bin/env bb

; NodeJS is required by react-native in this project
; clj is required
; pod is optional

(ns runner
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :refer [make-parents]]
            [clojure.string :refer [replace split join lower-case]]
            [clojure.tools.cli :refer [parse-opts]]))

;; -- ANSI colors --------------------------------------------------------------

(def ^:private blue   "\033[1;34m")
(def ^:private green  "\033[1;32m")
(def ^:private yellow "\033[1;33m")
(def ^:private red    "\033[1;31m")
(def ^:private dim    "\033[2m")
(def ^:private reset  "\033[0m")

(defn- info [msg]  (println (str blue "=>" reset " " msg)))
(defn- ok   [msg]  (println (str green "=>" reset " " msg)))
(defn- warn [msg]  (println (str yellow "=>" reset " " msg)))
(defn- fail [msg]
  (println (str red "=>" reset " " msg))
  (System/exit 1))
(defn- verbose [msg] (println (str dim "   " msg reset)))

;; -- Helpers ------------------------------------------------------------------

(defn camelcase-to-delimitered
  "Transform CamelCase project name to delimitered.
   e.g (camelcase-to-delimitered AwesomeProject _) will produce awesome_project"
  [k delimeter]
  (->> (split k #"(?=[A-Z])")
       (map lower-case)
       (join delimeter)))

(def documentation
  "https://github.com/vouch-opensource/krell/wiki/Reagent-Tutorial#using-the-repl")

(def cli-options
  [["-p" "--package PACKAGE_NAME" "Define organization package name e.g. --package com.example.krell"]
   ["-v" "--version VERSION" "Define specific React Native version e.g. --version 1.0.0"]
   ["-h" "--help" "Show help"]])

(defn- run-shell
  ([shell-command]
   (let [{:keys [exit err out]} shell-command]
     (if (zero? exit)
       out
       (fail (str "Command failed: " err)))))
  ([shell-command fn-error]
   (let [{:keys [exit out]} shell-command]
     (if (zero? exit)
       out
       (fn-error shell-command))))
  ([shell-command fn-success fn-error]
   (let [{:keys [exit]} shell-command]
     (if (zero? exit)
       (fn-success shell-command)
       (fn-error shell-command)))))

(defn- opts->cli-args
  "Convert parsed options map to CLI argument list.
   Maps :package to --package-name for @react-native-community/cli."
  [options]
  (reduce-kv
   (fn [acc k v]
     (let [flag (case k
                  :package "--package-name"
                  (str "--" (name k)))]
       (conj acc flag (str v))))
   []
   options))

;; -- Steps --------------------------------------------------------------------

(defn- gen-rn-project
  "Generate react-native project using @react-native-community/cli"
  [{:keys [arguments options]}]
  (let [project-name (first arguments)
        sh-args      (opts->cli-args options)]
    (info (str "Initializing React Native project: " project-name))
    (verbose "npx @react-native-community/cli init ...")
    (run-shell (apply shell/sh "npx" "@react-native-community/cli" "init" project-name sh-args))
    (ok "React Native project created")))

(defn- install-rn-deps
  "Run npm install"
  [project-name]
  (info "Installing Node dependencies...")
  (verbose "npm install")
  (run-shell (shell/sh "npm" "install" :dir project-name))
  (ok "Node dependencies installed"))

(defn- make-edns [project-name]
  (info "Creating dependency files...")
  (verbose "Writing deps.edn")
  (spit (str project-name "/deps.edn") (slurp "templates/deps-template"))
  (verbose "Writing build.edn")
  (spit (str project-name "/build.edn")
        (replace (slurp "templates/build-template") #"\$TEMPLATE\$" (str (camelcase-to-delimitered project-name "-") ".core")))
  (ok "Dependency files created"))

(defn- install-deps [project-name]
  (info "Installing Clojure dependencies...")
  (verbose "clj -M -m cljs.main --install-deps")
  (run-shell (shell/sh "clj" "-M" "-m" "cljs.main" "--install-deps" :dir project-name))
  (ok "Clojure dependencies installed"))

(defn- run-pod-install
  "Run pod install"
  [project-name]
  (run-shell (shell/sh "which" "pod")
             (fn [_]
               (info "Running pod install...")
               (verbose "pod install")
               (run-shell (shell/sh "pod" "install" :dir (str project-name "/ios")))
               (ok "Pod install complete"))
             (fn [{:keys [exit]}]
               (when (= exit 1)
                 (warn "pod not found, skipping iOS pod install")))))

(defn- write-clojure-file [project-name]
  (let [file-name        (str project-name "/src/" (camelcase-to-delimitered project-name "_") "/core.cljs")
        content          (slurp "templates/core-template")
        adjusted-content (replace content #"\$TEMPLATE\$" (str (camelcase-to-delimitered project-name "-") ".core"))]
    (info "Preparing ClojureScript files...")
    (verbose (str "Writing " file-name))
    (make-parents file-name)
    (spit file-name adjusted-content)
    (ok "ClojureScript files ready")))

(defn- setup-clojure-env [project-name]
  (make-edns project-name)
  (install-deps project-name)
  (run-pod-install project-name)
  (write-clojure-file project-name))

;; -- Main ---------------------------------------------------------------------

(defn- gen-project [{:keys [arguments summary] :as args}]
  (let [project-name (first arguments)]
    (if (re-find #"^\w+$" project-name)
      (do
        (println)
        (gen-rn-project args)
        (install-rn-deps project-name)
        (setup-clojure-env project-name)
        (println)
        (ok "Done!\n")
        (println (str "  $ cd " project-name))
        (println "  $ clj -M -m krell.main -co build.edn -c -r")
        (println)
        (println "  Open new terminal tabs and run:")
        (println "  $ npx react-native start")
        (println)
        (println "  For iOS:     $ npx react-native run-ios")
        (println "  For Android: $ npx react-native run-android")
        (println)
        (println (str "  Production:  $ clj -M -m krell.main -v -co build.edn -O advanced -c"))
        (println)
        (println (str "  Read more: " documentation)))
      (fail (str "Invalid project name \"" project-name "\". Use CamelCase, e.g. AwesomeProject\n\n" summary)))))

(let [{:keys [options arguments summary errors] :as args} (parse-opts *command-line-args* cli-options)
      is-args-empty?                                      (empty?
                                                           ;; to avoid NPE in babashka, bash will pass empty string to arguments
                                                           (filter #(not= % "") arguments))]
  (cond
    errors          (fail (str (first errors) "\n\n" summary))
    (:help options) (println summary)
    is-args-empty?  (fail (str "Missing project name.\n\n" summary))
    arguments       (gen-project args)))
