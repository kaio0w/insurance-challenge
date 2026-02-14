(ns insurance-challenge.partner-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [cheshire.core :as json]
   [insurance-challenge.routes :refer [app]]))

(defn resp->json [resp]
  (-> resp :body slurp (json/parse-string true)))

(deftest create-partner-success
  (let [response (app (-> (mock/request :post "/partners")
                          (mock/json-body {:name "XPTO"
                                           :cnpj "12345678901234"})))
        body (resp->json response)]
    (is (= 201 (:status response)))
    (is (contains? body :id))))

(deftest create-partner-invalid
  (let [response (app (-> (mock/request :post "/partners")
                          (mock/json-body {:name ""
                                           :cnpj ""})))
        body (resp->json response)]
    (is (= 400 (:status response)))
    (is (contains? body :humanized))
    (is (= {:name ["should be at least 1 characters"]
            :cnpj ["should match regex"]}
           (:humanized body)))))

