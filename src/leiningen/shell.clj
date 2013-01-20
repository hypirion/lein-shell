(ns leiningen.shell
  (:require [clojure.java.io :as io]))

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
  (let [proc (.exec (Runtime/getRuntime) (into-array cmd))]
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

(defn ^:no-project-needed shell
  "For shelling out from Leiningen. Useful for adding stuff to prep-tasks like
`make` or similar, which currently has no leiningen plugin.

Call through `lein shell cmd arg1 arg2 ... arg_n`."
  [& args]
  (let [args (if ((some-fn map? nil?) (first args))
               (rest args)
               args)]
    (sh args)))
