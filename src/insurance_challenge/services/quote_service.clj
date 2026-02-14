(ns insurance-challenge.services.quote-service
  (:require
   [insurance-challenge.store :as store]
   [insurance-challenge.clients.insurer :as insurer]))

(defn valid-age? [age]
  (and (integer? age) (<= 0 age 99)))

(defn valid-sex? [sex]
  (contains? #{"m" "M" "f" "F" "n" "N"} sex))

(defn partner-exists? [partner-id]
  (contains? @store/partners (str partner-id)))

(defn create-quote! [partner-id {:keys [age sex]}]
  (let [partner-id (str partner-id)]
    (cond
      (not (partner-exists? partner-id))
      {:status :partner-not-found}

      (not (valid-age? age))
      {:status :invalid-age}

      (not (valid-sex? sex))
      {:status :invalid-sex}

      :else
      (try
        (let [token (insurer/auth-token!)
              resp  (insurer/create-quotation! token {:age age :sex sex})]
          (if (<= 200 (:status resp) 299)
            (let [b (:body resp)
                  quote {:id        (or (get b "id") (:id b))
                         :partner-id partner-id
                         :age       age
                         :sex       sex
                         :price     (or (get b "price") (:price b))
                         :expire-at (or (get b "expire_at") (:expire_at b)
                                        (get b "expireAt") (:expireAt b))}]
              (swap! store/quotes assoc (:id quote) quote)
              {:status :created :quote quote})
            (insurer/insurer-error->map :quote-create (:status resp) (:body resp))))
        (catch Exception e
          (println :INSURER_EXCEPTION :quote-create (.getMessage e))
          {:status :insurer-error
           :http-status 500
           :code :exception
           :message "Insurer exception"})))))
