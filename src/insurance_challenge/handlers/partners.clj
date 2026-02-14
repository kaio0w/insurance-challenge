(ns insurance-challenge.handlers.partners
  (:require
   [clojure.string :as str]
   [insurance-challenge.services.partner-service :as services]
   [insurance-challenge.util :as u]))

(defn validate-partner [{:keys [name cnpj]}]
  (let [errors (cond-> {}
                 (or (nil? name) (str/blank? name))
                 (assoc :name ["should not be blank"])

                 (or (nil? cnpj) (str/blank? cnpj))
                 (assoc :cnpj ["should not be blank"])

                 (and (string? cnpj)
                      (not (str/blank? cnpj))
                      (not (u/valid-cnpj? cnpj)))
                 (assoc :cnpj ["should match 14 digits"]))]
    (when (seq errors) errors)))

(defn create-partner [{:keys [parameters]}]
  (let [data (:body parameters)
        errors (validate-partner data)]
    (if errors
      {:status 400
       :body {:error errors}}
      {:status 201
       :body (services/create-partner! data)})))
