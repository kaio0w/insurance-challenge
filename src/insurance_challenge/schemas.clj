(ns insurance-challenge.schemas)

(def partner-schema
  [:map
   [:name [:string {:min 1}]]
   [:cnpj [:re #"^\d{14}$"]]])

(def quote-schema
  [:map
   [:age [:int {:min 0 :max 99}]]
   [:sex [:enum "m" "M" "f" "F" "n" "N"]]])

(def health-response
  [:map
   [:status string?]])

(def policy-schema
  [:map
   [:quotation_id :uuid]
   [:name [:string {:min 1}]]
   [:sex [:enum "m" "M" "f" "F" "n" "N"]]
   [:date_of_birth [:re #"^\d{4}-\d{2}-\d{2}$"]]])
