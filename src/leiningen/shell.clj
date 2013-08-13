(ns leiningen.shell
  (:require [clojure.java.io :as io]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]))

(defmacro ^:private get-setting-fn
  "Returns a function which returns the highest priority setting when called
  with a project and a command. It is a macro because dynamic variables will get
  caught and dereferenced if this was a function. :("
  ([kw] `(get-setting-fn ~kw nil))
  ([kw default]
     `(fn [project# [command# & args#]]
        (or (get-in project# [:shell :commands command# ~kw])
            (get-in project# [:shell ~kw])
            ~default))))

(def ^:private get-environment
  (get-setting-fn :env eval/*env*))

(def ^:private get-directory
  (get-setting-fn :dir eval/*dir*))

(def ^:private get-exit-code
  (get-setting-fn :exit-code :default))

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
`make` or similar commands, which currently has no Leiningen plugin. If the
process returns a nonzero exit code, this command will force Leiningen to exit
with the same exit code.

Call through `lein shell cmd arg1 arg2 ... arg_n`."
  [& args]
  (let [[project cmd] (if ((some-fn map? nil?) (first args))
                        [(first args) (rest args)]
                        [nil args])]
    (let [exit-code (shell-with-project project cmd)
          exit-code-action (get-exit-code project cmd)]
      (case exit-code-action
        :ignore (main/debug (format "[shell] Ignoring exit code (is %d)"
                                    exit-code))
        :default (if-not (zero? exit-code)
                   (main/exit exit-code))))))
