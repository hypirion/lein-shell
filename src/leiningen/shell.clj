(ns leiningen.shell
  (:require [clojure.java.io :as io])
  (:import [java.io BufferedReader]))

(defn- out-pump [reader out]
  (let [buffer (make-array Character/TYPE 1024)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (recur (.read reader buffer))))))

(defn- in-pump [reader out done?]
  (loop []
    (if (.ready reader)
      (do
        (.write out (.read reader))
        (.flush out))
      (Thread/sleep 25))
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
      (let [in-wrap (BufferedReader. *in* 1)
            done (atom false)
            pump-out (doto (Thread. (bound-fn [] (out-pump out *out*))) .start)
            pump-err (doto (Thread. (bound-fn [] (out-pump err *err*))) .start)
            pump-in (doto (Thread. (bound-fn [] (in-pump in-wrap in done)))
                      .start)]
        (.join pump-err)
        (.join pump-err)
        (.waitFor proc)
        (reset! done true)
        (.join pump-in)))))

(defn ^:no-project-needed shell
  "I don't do a lot."
  [& args]
  (let [args (if ((some-fn map? nil?) (first args))
               (rest args)
               args)]
    (sh args)))
