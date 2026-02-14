(ns insurance-challenge.middleware.security
  (:require [clojure.string :as str]))

(defonce ^:private buckets (atom {}))

(defn- now-ms [] (System/currentTimeMillis))

(defn- client-ip [req]
  ;; se tiver proxy, você pode usar X-Forwarded-For
  (or (get-in req [:headers "x-forwarded-for"])
      (:remote-addr req)
      "unknown"))

(defn wrap-rate-limit
   ([handler] (wrap-rate-limit handler {:limit 60 :window-ms 60000}))
   ([handler {:keys [limit window-ms]}]
    (fn [req]
      (let [uri (:uri req)]
        (if (or (.startsWith uri "/swagger")
                (= uri "/swagger.json")
                (= uri "/health"))
          (handler req)
          (let [ip (client-ip req)
                t  (now-ms)
                {:keys [start count]} (get @buckets ip {:start t :count 0})
                reset? (>= (- t start) window-ms)
                start' (if reset? t start)
                count' (if reset? 1 (inc count))]
            (swap! buckets assoc ip {:start start' :count count'})
            (if (> count' limit)
              {:status 429
               :body {:error "Too many requests"}}
              (handler req))))))))

(defn wrap-security-headers [handler]
  (fn [req]
    (let [resp (handler req)]
      (-> resp
          (update :headers #(merge
                             {"X-Content-Type-Options" "nosniff"
                              "X-Frame-Options" "DENY"
                              "Referrer-Policy" "no-referrer"
                              "Cache-Control" "no-store"}
                             (or % {})))))))
