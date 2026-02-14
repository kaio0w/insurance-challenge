(ns insurance-challenge.util
  (:require [clojure.string :as str]))

(defn digits-only? [s]
  (every? #(Character/isDigit %) s))

(defn valid-cnpj? [cnpj]
  (and (string? cnpj)
       (= 14 (count cnpj))
       (digits-only? cnpj)))
