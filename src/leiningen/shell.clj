(ns leiningen.shell
  (:require [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]))

(defn- get-environment [project [command & args]]
  (or (get-in project [:shell :commands command :env])
      (get-in project [:shell :env])
      eval/*env*))

(defn- get-directory [project [command & args]]
  (or (get-in project [:shell :commands command :dir])
      (get-in project [:shell :dir])
      eval/*dir*))

(defn- lookup-command
  "Looks up the first part of command, and replaces it with an os-specific
  version if there is one."
  [project cmd]
  (let [command (first cmd)
        os (eval/get-os)]
    (if-let [os-cmd (get-in project [:shell :commands command os])]
      (do
        (main/debug (format "[shell] Replacing command %s with %s. (os is %s)"
                            command os-cmd os))
        (cons os-cmd (rest cmd)))
      cmd)))

(defn- shell-with-project [project cmd]
  (binding [eval/*dir* (get-directory project cmd)
            eval/*env* (get-environment project cmd)]
    (let [cmd (lookup-command project cmd)]
      (main/debug "[shell] Calling the shell with" cmd)
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
