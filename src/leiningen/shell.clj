(ns leiningen.shell
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]))

(defn- failf [fmt-string & args]
  (throw (Exception. ^String (apply format fmt-string args))))

(declare read-expansion)

(defn- read-default
  "read-default works like replace-values, except that it will stop
  reading when it finds a }. It returns a tuple [val end-pos], where
  val is a string of what was read."
  [project ^String s orig-start start]
  (let [sb (StringBuilder.)]
    (loop [i start
           quoted false]
      (if (>= i (.length s))
        (failf "Unexpected end of argument '%s'. Opening ${ starts at %d, but is not closed"
               s (dec orig-start))
        (let [c (.charAt s i)]
          (if quoted
            (do (.append sb c)
                (recur (inc i) false))
            (case c
              \$ (let [[val end-pos] (read-expansion project s (inc i))]
                   (.append sb (str val))
                   (recur (inc end-pos) false))
              \} [(.toString sb) i]
              \\ (recur (inc i) true)
              (do (.append sb c)
                  (recur (inc i) false)))))))))

(defn- lookup-vector
  "Given a string and a start/stop position, returns the Clojure value it
  represents. If the value is not a vector, it is wrapped in one."
  [^String s start end]
  (let [lookup-str (subs s start end)
        lookup-val (read-string lookup-str)]
    (if-not (vector? lookup-val)
      [lookup-val]
      lookup-val)))

(defn- read-expansion
  "read-expansion reads a parameter expansion. It does not perform any kind of
  quoting, and Clojure values that contain :- or } will error out the expansion
  mechanism. For information on how the parameter expansion works, look at
  replace-values."
  [project ^String s start]
  (if (not= (.charAt s start) \{)
    (failf "Expected { after $ at position %d in argument '%s', but was %c"
           start s (.charAt s start))
    (loop [i (inc start)
           colon false]
      (if (>= i (.length s))
        (failf "Unexpected end of argument '%s'. Opening ${ starts at %d, but is not closed"
               s (dec start))
        (let [c (.charAt s i)]
          (cond (and colon (= c \-))
                ;; ":-" -> we're done then. No smart syntactic trick here, so if you
                ;; have a Clojure value that contains :- then you have a problem.
                (let [lookup-vec (lookup-vector s (inc start) (dec i))
                      [default end-pos] (read-default project s start (inc i))]
                  [(get-in project lookup-vec default) end-pos])
                (= c \}) ;; no support for values containing } either
                (let [lookup-vec (lookup-vector s (inc start) i)]
                  [(get-in project lookup-vec) i])
                :otherwise
                (recur (inc i) (= c \:))))))))

(defn- replace-values
  "replace-values takes a project and a string s. Parameter expansion is applied
  to forms in the shape ${xxx}, where xxx is a clojure value or a vector of
  clojure values. If the shape looks like ${xxx:-yyy}, then the string yyy will
  be the default value if xxx does not exist in the project map. yyy will be
  expanded recursively, i.e. the form ${xxx:-${xxx2:-yyy}} is legal, and will
  look for xxx, then xxx2 if not found, then yyy if not found, in that order.

  replace-values will also unquote backslashed values. \"\\${:foo}\"
  will be translated to the string \"${:foo}\", and \"\\\\${:foo}\"
  will be translated to \"\\[expansion]\".

  The form ${} is illegal."
  [project ^String s]
  (let [sb (StringBuilder. (.length s))]
    (loop [i (int 0)
           quoted false]
      (if (>= i (.length s))
        (.toString sb)
        (let [c (.charAt s i)]
          (if quoted
            (do (.append sb c)
                (recur (inc i) false))
            (case c
                \$ (let [[val end-pos] (read-expansion project s (inc i))]
                       (.append sb (str val))
                       (recur (int (inc end-pos)) false))
                \\ (recur (inc i) true)
                (do (.append sb c)
                    (recur (inc i) false)))))))))

(defn- param-expand
  [project s]
  (if-not (string? s)
    (str s)
    (replace-values project s)))

(defmacro ^:private get-setting-fn
  "Returns a function which returns the highest priority setting when called
  with a project and a command. It is a macro because dynamic variables will get
  caught and dereferenced if this was a function. Will return falsey values."
  ([kw] `(get-setting-fn ~kw nil))
  ([kw default]
     `(let [gsym# (gensym "not-found")]
        (fn [project# [command# & args#]]
          (first
           (remove #(= gsym# %)
                   [(get-in project# [:shell :commands command# ~kw] gsym#)
                    (get-in project# [:shell ~kw] gsym#)
                    ~default]))))))

(def ^:private get-environment
  (get-setting-fn :env eval/*env*))

(def ^:private get-directory
  (get-setting-fn :dir eval/*dir*))

(def ^:private get-exit-code
  (get-setting-fn :exit-code :default))

(def ^:private get-pipe-stdin?
  (get-setting-fn :pipe-stdin? true))

(defn- lookup-command
  "Looks up the first part of command, and replaces it with an os-specific
  version if there is one."
  [project cmd]
  (let [command (first cmd)
        os (eval/get-os)]
    (if-let [os-cmd (or (get-in project [:shell :commands command os])
                        (get-in project [:shell :commands command :default-command]))]
      (let [normalized-cmd (if (string? os-cmd) [os-cmd] os-cmd)]
        (main/debug (format "[shell] Replacing command %s with %s. (os is %s)"
                            command normalized-cmd os))
        (concat normalized-cmd (rest cmd)))
      cmd)))

(defn- shell-with-project [project cmd]
  (binding [eval/*dir* (get-directory project cmd)
            eval/*env* (get-environment project cmd)
            eval/*pump-in* (get-pipe-stdin? project cmd)]
    (let [cmd (lookup-command project cmd)]
      (main/debug "[shell] Calling the shell with" cmd)
      (apply eval/sh cmd))))

(defn ^:no-project-needed shell
  "For shelling out from Leiningen. Useful for adding stuff to prep-tasks like
`make` or similar commands, which currently has no Leiningen plugin. If the
process returns a nonzero exit code, this command will force Leiningen to exit
with the same exit code.

Call through `lein shell cmd arg1 arg2 ... arg_n`."
  [project & cmd]
  (let [cmd (mapv #(param-expand project %) cmd)
        exit-code (shell-with-project project cmd)
        exit-code-action (get-exit-code project cmd)]
    (case exit-code-action
      :ignore (main/debug (format "[shell] Ignoring exit code (is %d)"
                                  exit-code))
      :default (if-not (zero? exit-code)
                 (main/exit exit-code)))))
