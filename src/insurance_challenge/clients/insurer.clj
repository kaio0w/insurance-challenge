(ns insurance-challenge.clients.insurer
  (:require
   [clj-http.client :as http]
   [cheshire.core :as json]
   [clojure.string :as str]))

(defn env [k default]
  (or (System/getenv k) default))

(def base-url
  (env "INSURER_BASE_URL" "http://localhost:8080"))

(def api-key
  (env "INSURER_API_KEY" nil))

(defn maybe-parse-json [x]
  (cond
    (map? x) x
    (string? x)
    (try (json/parse-string x true)
         (catch Exception _ {:raw x}))
    :else {:raw x}))

(defn insurer-error->map
  "Padroniza erro vindo da seguradora + loga no terminal."
  [context http-status body]
  (let [b (maybe-parse-json body)
         msg (or (get-in b [:error :message])
                (get-in b ["error" "message"])
                (:message b)
                (get b "message")
                (str b))
        msg-lc (str/lower-case (str msg))
        code (cond
               (and (= 404 http-status) (re-find #"quotation.*not found" msg-lc))
               :quotation-not-found

               (and (= 400 http-status) (re-find #"expired" msg-lc))
               :quotation-expired

               (and (= 400 http-status) (re-find #"sex" msg-lc))
               :sex-mismatch

               (and (= 400 http-status) (re-find #"date_of_birth|date of birth|birth" msg-lc))
               :dob-mismatch

               (= 401 http-status) :unauthorized
               (= 403 http-status) :forbidden
               (>= http-status 500) :insurer-down
               :else :insurer-error)]
    (println :INSURER_ERROR context http-status code msg)
    {:status :insurer-error
     :http-status http-status
     :code code
     :message msg}))

(defn auth-token! []
  (when (or (nil? api-key) (str/blank? api-key))
    (throw (ex-info "Missing INSURER_API_KEY env var" {})))

  (let [resp (http/post (str base-url "/api/auth")
                        {:headers {"x-api-key" api-key}
                         :throw-exceptions false
                         :as :json
                         :conn-timeout 2000
                         :socket-timeout 5000})]
    (if (= 200 (:status resp))
      (or (get-in resp [:body "access_token"])
          (get-in resp [:body :access_token]))
      (throw (ex-info "Insurer auth failed"
                      {:status (:status resp) :body (:body resp)})))))

(defn create-quotation! [token {:keys [age sex]}]
  (http/post (str base-url "/api/quotations")
             {:headers {"Authorization" (str "Bearer " token)}
              :content-type :json
              :accept :json
              :body (json/generate-string {:age age :sex sex})
              :throw-exceptions false
              :as :json
              :conn-timeout 2000
              :socket-timeout 5000}))

(defn create-policy! [token {:keys [quotation_id name sex date_of_birth]}]
  (http/post (str base-url "/api/policies")
             {:headers {"Authorization" (str "Bearer " token)}
              :content-type :json
              :accept :json
              :body (json/generate-string {:quotation_id quotation_id
                                           :name name
                                           :sex sex
                                           :date_of_birth date_of_birth})
              :throw-exceptions false
              :as :json
              :conn-timeout 2000
              :socket-timeout 5000}))

(defn get-policy! [token policy-id]
  (http/get (str base-url "/api/policies/" policy-id)
            {:headers {"Authorization" (str "Bearer " token)}
             :throw-exceptions false
             :as :json
             :conn-timeout 2000
             :socket-timeout 5000}))
