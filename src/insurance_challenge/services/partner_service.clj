(ns insurance-challenge.services.partner-service
  (:require [insurance-challenge.store :as store]))

(defn create-partner! [data]
  (let [id (str (java.util.UUID/randomUUID))
        partner (assoc data :id id)]
    (swap! store/partners assoc id partner)
    partner))
