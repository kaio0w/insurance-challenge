(ns insurance-challenge.handlers.quotes
  (:require
   [insurance-challenge.services.quote-service :as service]))

(defn- map-insurer-status->http [s]
  (cond
    (= 400 s) 400
    (= 404 s) 404
    (>= s 500) 502
    :else 502))

(defn create-quote [req]
  (let [partner-id (or (get-in req [:parameters :path :partner-id])
                       (get-in req [:path-params :partner-id]))
        body       (or (get-in req [:parameters :body])
                       (get-in req [:body-params])
                       (get-in req [:json-body]))
        result     (service/create-quote! (some-> partner-id str) body)]
    (case (:status result)
      :created {:status 200 :body (:quote result)}
      :partner-not-found {:status 404 :body {:error "Partner not found"}}
      :invalid-age {:status 400 :body {:error "Invalid age"}}
      :invalid-sex {:status 400 :body {:error "Invalid sex"}}
      :insurer-error {:status (map-insurer-status->http (:http-status result))
                      :body {:error (:message result)
                             :code (name (:code result))}}
      {:status 500 :body {:error "Unexpected error"}})))
