(ns insurance-challenge.services.policy-service
  (:require
   [insurance-challenge.store :as store]
   [insurance-challenge.clients.insurer :as insurer]
   [clojure.string :as str]))

(defn partner-exists? [partner-id]
  (contains? @store/partners (str partner-id)))

(defn create-policy! [partner-id {:keys [quotation_id name sex date_of_birth] :as body}]
  (let [partner-id (str partner-id)]
    (cond
      (not (partner-exists? partner-id))
      {:status :partner-not-found}

      (or (nil? quotation_id) (str/blank? (str quotation_id)))
      {:status :invalid-request :message "quotation_id required"}

      (or (nil? name) (str/blank? name))
      {:status :invalid-request :message "name required"}

      (not (contains? #{"m" "M" "f" "F" "n" "N"} sex))
      {:status :invalid-request :message "invalid sex"}

      (or (nil? date_of_birth) (str/blank? date_of_birth))
      {:status :invalid-request :message "date_of_birth required"}

      :else
      (try
        (let [token (insurer/auth-token!)
              resp  (insurer/create-policy! token body)]
          (if (<= 200 (:status resp) 299)
            (let [b (:body resp)
                  policy {:id (or (get b "id") (:id b))
                          :partner-id partner-id
                          :quotation-id (or (get b "quotation_id") (:quotation_id b) quotation_id)
                          :name (or (get b "name") (:name b) name)
                          :sex (or (get b "sex") (:sex b) sex)
                          :date-of-birth (or (get b "date_of_birth") (:date_of_birth b) date_of_birth)}]
              (swap! store/policies assoc (:id policy) policy)
              {:status :created :policy policy})
            (insurer/insurer-error->map :policy-create (:status resp) (:body resp))))
        (catch Exception e
          (println :INSURER_EXCEPTION :policy-create (.getMessage e))
          {:status :insurer-error
           :http-status 500
           :code :exception
           :message "Insurer exception"})))))

(defn get-policy! [partner-id policy-id]
  (let [partner-id (str partner-id)
        policy-id  (str policy-id)]
    (cond
      (not (partner-exists? partner-id))
      {:status :partner-not-found}

      :else
      (try
        (let [token (insurer/auth-token!)
              resp  (insurer/get-policy! token policy-id)]
          (if (<= 200 (:status resp) 299)
            (let [b (:body resp)
                  policy {:id (or (get b "id") (:id b))
                          :partner-id partner-id
                          :quotation-id (or (get b "quotation_id") (:quotation_id b))
                          :name (or (get b "name") (:name b))
                          :sex (or (get b "sex") (:sex b))
                          :date-of-birth (or (get b "date_of_birth") (:date_of_birth b))}]
              {:status :ok :policy policy})
            (insurer/insurer-error->map :policy-get (:status resp) (:body resp))))
        (catch Exception e
          (println :INSURER_EXCEPTION :policy-get (.getMessage e))
          {:status :insurer-error
           :http-status 500
           :code :exception
           :message "Insurer exception"})))))
