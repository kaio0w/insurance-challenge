(ns insurance-challenge.policy-test
  (:require
   [clojure.test :refer :all]
   [insurance-challenge.services.policy-service :as ps]
   [insurance-challenge.clients.insurer :as insurer]
   [insurance-challenge.store :as store]))

(deftest policy-service-create-success
  (with-redefs [store/partners (atom {"p1" {:id "p1"}})
                store/policies (atom {})
                insurer/auth-token! (fn [] "t")
                insurer/create-policy! (fn [_ body]
                                         {:status 200
                                          :body {"id" "pol1"
                                                 "quotation_id" (str (:quotation_id body))
                                                 "name" (:name body)
                                                 "sex" (:sex body)
                                                 "date_of_birth" (:date_of_birth body)}})]
    (let [res (ps/create-policy! "p1" {:quotation_id "q1"
                                       :name "Alice"
                                       :sex "f"
                                       :date_of_birth "1995-01-01"})]
      (is (= :created (:status res)))
      (is (= "pol1" (get-in res [:policy :id]))))))
