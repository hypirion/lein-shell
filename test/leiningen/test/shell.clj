(ns leiningen.test.shell
  (:use [clojure.test])
  (:require [leiningen.shell :as shell]))

(def ^:private replace-values @#'leiningen.shell/replace-values)

(deftest test-replacement
  (let [p {:a {:a 'a :b 1 :c "2"}
           :foo "banana"
           :bar {:baz "zap"}}]
    (is (= (replace-values p "foo bar baz") "foo bar baz"))
    (is (= (replace-values p "${:foo}") "banana"))
    (is (= (replace-values p "${:baz:-not-found}") "not-found"))
    (is (= (replace-values p "${:baz:-foo: ${:foo}}") "foo: banana"))
    (is (= (replace-values p "${:recursive:-10 ${:thing:-delicious ${:foo}}s}")
           "10 delicious bananas"))

    (is (= (replace-values p "${[:a :b]}") "1"))
    (is (= (replace-values p "${[:a :c]}") "2"))
    (is (= (replace-values p "${[:a :a]}") "a"))

    (is (thrown-with-msg? Exception #"Unexpected end of argument"
                          (replace-values p "${:a")))
    (is (thrown-with-msg? Exception #"Unexpected end of argument"
                          (replace-values p "${:a:-Only partially closed: ${:b}")))

    (is (thrown-with-msg? Exception #"Expected \{ after \$"
                          (replace-values p "$foo")))))
