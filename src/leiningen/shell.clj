(ns leiningen.shell
  (:require [clojure.java.io :as io]))

(defn- pump [reader out]
  (let [buffer (make-array Character/TYPE 1024)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (recur (.read reader buffer))))
    (.close reader)))

(defn sh
  "A version of clojure.java.shell/sh that streams out/err, but not in."
  [cmd]
  (let [proc (.exec (Runtime/getRuntime) (into-array cmd))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn [] (.destroy proc))))
    (with-open [out (io/reader (.getInputStream proc))
                err (io/reader (.getErrorStream proc))]
      (let [pump-out (doto (Thread. (bound-fn [] (pump out *out*))) .start)
            pump-err (doto (Thread. (bound-fn [] (pump err *err*))) .start)]
        (.join pump-out)
        (.join pump-err))
      (.waitFor proc))))

(defn ^:no-project-needed shell
  "I don't do a lot."
  [& args]
  (let [args (if ((some-fn map? nil?) (first args))
               (rest args)
               args)]
    (sh args)))
