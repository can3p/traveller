(ns can3p.traveller.test
  (:use [can3p.traveller.protocol]
        [can3p.traveller.protocol-dummy]
        [taoensso.timbre :as timbre
         :refer (debug  debugf)]
        [can3p.traveller.traverse]))

(def conf {
           :store :dummy
           :queue :dummy
           :transport :dummy
           :can-terminate true
           })

(defn test-traverse []
  (traverse conf)
  (do
    (schedule-register conf "A")
    (schedule-register conf "F"))
  nil)
