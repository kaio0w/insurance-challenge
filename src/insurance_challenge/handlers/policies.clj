(ns insurance-challenge.handlers.policies
  (:require
   [insurance-challenge.services.policy-service :as service]))

(defn- map-insurer-status->http [s]
  (cond
    (= 400 s) 400
    (= 404 s) 404
    (>= s 500) 502
    :else 502))

(defn create-policy [req]
  (let [partner-id (or (get-in req [:parameters :path :partner-id])
                       (get-in req [:path-params :partner-id])
                       (some-> req :path-params vals first))
        body       (or (get-in req [:parameters :body])
                       (get-in req [:body-params]))
        result     (service/create-policy! partner-id body)]
    (case (:status result)
      :created {:status 201 :body (:policy result)}
      :partner-not-found {:status 404 :body {:error "Partner not found"}}
      :invalid-request {:status 400 :body {:error (:message result)}}
      :insurer-error {:status (map-insurer-status->http (:http-status result))
                      :body {:error (:message result)
                             :code (name (:code result))}}
      {:status 500 :body {:error "Unexpected error"}})))

(defn get-policy [req]
  (let [partner-id (or (get-in req [:parameters :path :partner-id])
                       (get-in req [:path-params :partner-id])
                       (some-> req :path-params vals first))
        policy-id  (or (get-in req [:parameters :path :policy-id])
                       (get-in req [:path-params :policy-id])
                       (some-> req :path-params vals second))
        result     (service/get-policy! partner-id policy-id)]
    (case (:status result)
      :ok {:status 200 :body (:policy result)}
      :partner-not-found {:status 404 :body {:error "Partner not found"}}
      :insurer-error {:status (map-insurer-status->http (:http-status result))
                      :body {:error (:message result)
                             :code (name (:code result))}}
      {:status 500 :body {:error "Unexpected error"}})))
