(ns leiningen.shell
  (:require [clojure.java.io :as io]
            [leiningen.core.eval :as eval]))

(def ^:dynamic *dir*
  "Directory in which to start subprocesses."
  (System/getProperty "user.dir"))

(def ^:dynamic *env*
  "Environment map given to subprocesses."
  nil)

(defn normalize-env
  "Normalizes a map such that it can be properly read by a Process created by
  Runtime/getRuntime."
  [env]
  (->> (merge {} (System/getenv) env)
       (filter val)
       (map #(str (name (key %)) "=" (val %)))
       (into-array String)))

(defn- out-pump [reader out]
  (let [buffer (make-array Character/TYPE 1024)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (recur (.read reader buffer))))))

(defn- in-pump [reader out done?]
  "Redirects input from this process to the input stream to the other process,
  one byte at a time. Instead of blocking when reading, busy waits in order to
  gracefully exit and not read other subprocesses' input."
  (loop []
    (if (.ready reader)
      (do
        (.write out (.read reader))
        (.flush out))
      (Thread/sleep 10))
    (when (not @done?)
      (recur))))

(defn sh
  "A version of clojure.java.shell/sh that streams out/err/in to the
  subprocess."
  [cmd]
  (let [proc (.exec (Runtime/getRuntime) (into-array cmd)
                    *env* (io/file *dir*))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn [] (.destroy proc))))
    (with-open [out (io/reader (.getInputStream proc))
                err (io/reader (.getErrorStream proc))
                in (io/writer (.getOutputStream proc))]
      (let [done (atom false)
            pump-out (doto (Thread. (bound-fn [] (out-pump out *out*))) .start)
            pump-err (doto (Thread. (bound-fn [] (out-pump err *err*))) .start)
            pump-in (doto (Thread. (bound-fn [] (in-pump *in* in done)))
                      .start)]
        (.join pump-err)
        (.join pump-err)
        (let [exit-value (.waitFor proc)]
          (reset! done true)
          (.join pump-in)
          exit-value)))))

(defn- lookup-command [project cmd]
  (get-in project [:shell :commands cmd :os (eval/get-os)]
          cmd))

(defn- shell-with-project [project cmd]
  (binding [*dir* (or (get-in project [:shell :dir])
                      *dir*)
            *env* (normalize-env (get-in project [:shell :env]))]
    (let [cmd (lookup-command project cmd)]
      (sh cmd))))

(defn ^:no-project-needed shell
  "For shelling out from Leiningen. Useful for adding stuff to prep-tasks like
`make` or similar, which currently has no Leiningen plugin.

Call through `lein shell cmd arg1 arg2 ... arg_n`."
  [& args]
  (let [[project cmd] (if ((some-fn map? nil?) (first args))
                        [(first args) (rest args)]
                        [nil args])]
    (shell-with-project project cmd)))
