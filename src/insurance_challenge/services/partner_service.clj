(ns insurance-challenge.services.partner-service
  (:require [insurance-challenge.store :as store]))

(defn create-partner! [data]
  (let [id (str (java.util.UUID/randomUUID))
        partner (assoc data :id id)
        _ (println :SERVICE_BEFORE (count @store/partners) (System/identityHashCode store/partners))
        _ (swap! store/partners assoc id partner)
        _ (println :SERVICE_AFTER (count @store/partners) (System/identityHashCode store/partners))]
    (swap! store/partners assoc id partner)
    partner))
