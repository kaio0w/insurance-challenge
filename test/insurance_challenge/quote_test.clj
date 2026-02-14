(ns insurance-challenge.quote-test
  (:require
   [clojure.test :refer :all]
   [insurance-challenge.handlers.quotes :as handler]
   [insurance-challenge.services.quote-service :as service]
   [insurance-challenge.store :as store]
   [insurance-challenge.clients.insurer :as insurer]))

(defn resp->json [resp] 
  (:body resp))


(deftest valid-age-test
  (when (resolve 'insurance-challenge.handlers.quotes/valid-age?)
    (let [valid-age? (var-get (resolve 'insurance-challenge.handlers.quotes/valid-age?))]
      (is (true? (valid-age? 0)))
      (is (true? (valid-age? 99)))
      (is (false? (valid-age? -1)))
      (is (false? (valid-age? 100)))
      (is (false? (valid-age? "20"))))))

(deftest valid-sex-test
  (when (resolve 'insurance-challenge.handlers.quotes/valid-sex?)
    (let [valid-sex? (var-get (resolve 'insurance-challenge.handlers.quotes/valid-sex?))]
      (doseq [s ["m" "M" "f" "F" "n" "N"]]
        (is (true? (valid-sex? s))))
      (doseq [s ["x" "" nil "male" "1"]]
        (is (false? (valid-sex? s)))))))

(deftest create-quote-returns-201-when-created
  (with-redefs [service/create-quote! (fn [partner-id body]
                                        (is (= "p1" partner-id))
                                        (is (= {:age 20 :sex "m"} body))
                                        {:status :created
                                         :quote {:id "q1"
                                                 :partner-id "p1"
                                                 :age 20
                                                 :sex "m"
                                                 :price 123.45
                                                 :expire-at "2026-03-15"}})]
    (let [req {:parameters {:path {:partner-id "p1"}
                            :body {:age 20 :sex "m"}}}
          resp (handler/create-quote req)
          body (resp->json resp)]
      (is (= 200 (:status resp)))
      (is (= "q1" (:id body)))
      (is (= "p1" (:partner-id body))))))

(deftest create-quote-returns-404-when-partner-not-found
  (with-redefs [service/create-quote! (fn [_partner-id _body]
                                        {:status :partner-not-found})]
    (let [req {:parameters {:path {:partner-id "missing"}
                            :body {:age 20 :sex "m"}}}
          resp (handler/create-quote req)
          body (resp->json resp)]
      (is (= 404 (:status resp)))
      (is (= {:error "Partner not found"} body)))))

(deftest create-quote-returns-400-when-invalid-age
  (with-redefs [service/create-quote! (fn [_ _] {:status :invalid-age})]
    (let [resp (handler/create-quote {:parameters {:path {:partner-id "p1"}
                                                   :body {:age -1 :sex "m"}}})]
      (is (= 400 (:status resp)))
      (is (= {:error "Invalid age"} (:body resp))))))

(deftest create-quote-returns-500-on-unexpected-status
  (with-redefs [service/create-quote! (fn [_ _] {:status :wat})]
    (let [resp (handler/create-quote {:parameters {:path {:partner-id "p1"}
                                                   :body {:age 10 :sex "m"}}})]
      (is (= 500 (:status resp)))
      (is (= {:error "Unexpected error"} (:body resp))))))

(deftest store-sanity-check
  ;; Só pra garantir que a store existe e é um atom (opcional)
  (is (instance? clojure.lang.IAtom store/partners)))

(deftest service-create-quote-partner-not-found
  (with-redefs [store/partners (atom {})         ;; ninguém cadastrado
                store/quotes   (atom {})]
    (let [result (service/create-quote! "p-missing" {:age 20 :sex "m"})]
      (is (= :partner-not-found (:status result)))
      (is (empty? @store/quotes)))))

(deftest service-create-quote-invalid-age
  (with-redefs [store/partners (atom {"p1" {:id "p1"}})
                store/quotes   (atom {})]
    (let [result (service/create-quote! "p1" {:age -1 :sex "m"})]
      (is (= :invalid-age (:status result)))
      (is (empty? @store/quotes)))))

(deftest service-create-quote-invalid-sex
  (with-redefs [store/partners (atom {"p1" {:id "p1"}})
                store/quotes   (atom {})]
    (let [result (service/create-quote! "p1" {:age 20 :sex "x"})]
      (is (= :invalid-sex (:status result)))
      (is (empty? @store/quotes)))))

(deftest service-create-quote-success-persists
  (with-redefs [store/partners (atom {"p1" {:id "p1"}})
                store/quotes   (atom {})
                insurer/auth-token! (fn [] "t")
                insurer/create-quotation! (fn [_ _]
                                            {:status 200
                                             :body {:id "q-local"
                                                    :price 143.82
                                                    :expire_at "2026-02-17"}})]
    (let [result (service/create-quote! "p1" {:age 20 :sex "m"})]
      (is (= :created (:status result)))

      (let [q (:quote result)]
        (is (= "q-local" (:id q)))
        (is (= "p1" (:partner-id q)))
        (is (= 20 (:age q)))
        (is (= "m" (:sex q)))

        ;; preço agora vem da seguradora (número)
        (is (number? (:price q)))
        (is (= 143.82 (:price q)))

        ;; expire-at mapeado do :expire_at
        (is (= "2026-02-17" (:expire-at q)))

        (is (= q (get @store/quotes "q-local")))))))

(deftest service-create-quote-success-from-insurer
  (with-redefs [store/partners (atom {"p1" {:id "p1"}})
                store/quotes   (atom {})
                insurer/auth-token! (fn [] "t0k3n")
                insurer/create-quotation! (fn [token payload]
                                            (is (= "t0k3n" token))
                                            (is (= {:age 20 :sex "m"} payload))
                                            {:status 200
                                             :body {"id" "q1"
                                                    "price" 123.45
                                                    "expire_at" "2026-03-15"}})]
    (let [result (service/create-quote! "p1" {:age 20 :sex "m"})
          q (:quote result)]
      (is (= :created (:status result)))
      (is (= "q1" (:id q)))
      (is (= "p1" (:partner-id q)))
      (is (= 123.45 (:price q)))
      (is (= "2026-03-15" (:expire-at q)))
      (is (= q (get @store/quotes "q1"))))))

(deftest service-create-quote-insurer-error
  (with-redefs [store/partners (atom {"p1" {:id "p1"}})
                store/quotes   (atom {})
                insurer/auth-token! (fn [] "t0k3n")
                insurer/create-quotation! (fn [_ _] {:status 500 :body {"message" "internal"}})

                ;; 👇 mock com 3 args (context, status, body)
                insurer/insurer-error->map (fn [_ctx status _body]
                                             {:status :insurer-error
                                              :http-status status
                                              :code :insurer-down
                                              :message "internal"})]
    (let [result (service/create-quote! "p1" {:age 20 :sex "m"})]
      (is (= :insurer-error (:status result)))
      (is (= 500 (:http-status result)))
      (is (= :insurer-down (:code result)))
      (is (empty? @store/quotes)))))


