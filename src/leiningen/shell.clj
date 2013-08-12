(ns leiningen.shell
  (:require [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]))

(defn- lookup-command
  "Looks up the first part of command, and replaces it with an os-specific
  version if there is one."
  [project cmd]
  (let [command (first cmd)]
    (if-let [os-cmd (get-in project [:shell :commands command (eval/get-os)])]
      (cons os-cmd (rest cmd))
      cmd)))

(defn- shell-with-project [project cmd]
  (binding [eval/*dir* (or (get-in project [:shell :dir])
                           eval/*dir*)
            eval/*env* (get-in project [:shell :env])]
    (let [cmd (lookup-command project cmd)]
      (apply eval/sh cmd))))

(defn ^:no-project-needed shell
  "For shelling out from Leiningen. Useful for adding stuff to prep-tasks like
`make` or similar, which currently has no Leiningen plugin. If the process
returns a nonzero exit code, this command will force Leiningen to exit with the
same exit code.

Call through `lein shell cmd arg1 arg2 ... arg_n`."
  [& args]
  (let [[project cmd] (if ((some-fn map? nil?) (first args))
                        [(first args) (rest args)]
                        [nil args])]
    (let [exit-code (shell-with-project project cmd)]
      (if-not (zero? exit-code)
        (main/exit exit-code)))))
