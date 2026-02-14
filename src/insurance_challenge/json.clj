(ns insurance-challenge.json
  (:require [cheshire.core :as json]
            [ring.util.response :as response]))

(defn parse-json-body [handler]
  (fn [request]
    (let [body (slurp (:body request))
          parsed (when (seq body)
                   (json/parse-string body true))]
      (handler (assoc request :json-body parsed)))))

(defn json-response [data & [status]]
  (-> (response/response (json/generate-string data))
      (response/status (or status 200))
      (response/content-type "application/json")))
