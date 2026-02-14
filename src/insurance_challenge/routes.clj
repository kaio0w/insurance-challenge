(ns insurance-challenge.routes
  (:require
   [reitit.ring :as ring]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.ring.middleware.exception :as exception]
   [reitit.coercion.malli :as coercion]
   [reitit.ring.coercion :as coercion-middleware]
   [muuntaja.core :as m]
   [insurance-challenge.handlers.policies :as policies]
   [insurance-challenge.schemas :as schemas]
   [insurance-challenge.handlers.partners :as partners]
   [insurance-challenge.handlers.quotes :as quotes]
   [insurance-challenge.middleware.security :as sec]))

(def router
  (ring/router
   [["/swagger.json"
     {:get (swagger/create-swagger-handler)}]
    
    ["/swagger"
     {:get (fn [_]
             {:status 302
              :headers {"Location" "/swagger/"}})}]
    
    ["/swagger/*"
     (swagger-ui/create-swagger-ui-handler
      {:path "/swagger"
       :url "/swagger.json"})]

    ["/health"
     {:get {:summary "Health check"
            :responses {200 {:body schemas/health-response}}
            :handler (fn [_]
                       {:status 200
                        :body {:status "ok"}})}}]

    ["/partners"
     {:post {:summary "Create partner"
             :parameters {:body schemas/partner-schema}
             :responses {200 {:body map?}
                         400 {:body map?}}
             :handler partners/create-partner}}]

    ["/partners/:partner-id/quotes"
     {:post {:summary "Create quote"
             :parameters {:path [:map
                                 [:partner-id string?]]
                          :body schemas/quote-schema}
             :responses {200 {:body map?}
                         400 {:body map?}
                         404 {:body map?}}
             :handler quotes/create-quote}}]
    
    ["/partners/:partner-id/policies"
     {:post {:summary "Create policy"
             :parameters {:path [:map [:partner-id string?]]
                          :body schemas/policy-schema}
             :responses {200 {:body map?}
                         400 {:body map?}
                         404 {:body map?}
                         502 {:body map?}}
             :handler policies/create-policy}}]
    
    ["/partners/:partner-id/policies/:policy-id"
     {:get {:summary "Get policy"
            :parameters {:path [:map
                                [:partner-id string?]
                                [:policy-id string?]]}
            :responses {200 {:body map?}
                        404 {:body map?}
                        502 {:body map?}}
            :handler policies/get-policy}}]
]

   {:data {:swagger {:info {:title "Insurance Challenge API"
                            :description "API Principal"
                            :version "1.0.0"}}
           :coercion coercion/coercion
           :muuntaja m/instance
           :middleware [sec/wrap-rate-limit
                        sec/wrap-security-headers
                        parameters/parameters-middleware
                        muuntaja/format-middleware
                        exception/exception-middleware
                        coercion-middleware/coerce-request-middleware]}}))

(def app
  (ring/ring-handler
   router
   (ring/create-default-handler)))

