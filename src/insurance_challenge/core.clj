(ns insurance-challenge.core
  (:require
   [ring.adapter.jetty :as jetty]
   [insurance-challenge.routes :refer [app]])
  (:gen-class))

(defonce server (atom nil))

(defn start-server []
  (reset! server
          (jetty/run-jetty #'app
                           {:port 3000
                            :join? false}))
  (println "Server running on port 3000"))

(defn stop-server []
  (when @server
    (.stop @server)
    (reset! server nil)
    (println "Server stopped")))

(defn -main [& _]
  (start-server))
