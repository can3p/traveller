(ns can3p.traveller.protocol-dummy
  (:require [can3p.traveller.protocol]
            [clojure.set :as set]))

;; (init-store)        ;; initial code to reset
;; (fetch-node-to-dl)  ;; get another item to process
;; (fetch-edges node-id) ;; get connections from/to the node
;;                     ;; returns a map with keys :points-to and :pointed-by
;; (store-edges node-id edges) ;; stores connections somewhere and return store-id
;; (schedule-processing store-id)
;; (fetch-store-id-to-process)


;; (retrieve-edges store-id) ;; retrieve connections from the store
;; (register-edges edges) ;; register nodes as known if not already
;;                        ;; and return a list of new node-ids
;;                        ;; structure is whatever was returned by
;;                        ;; fetch edges
;; (schedule-download node-ids)

;; (fetch-node-to-register) ;; check if we got any new node to register, nil if not
;; (register-node node) ;; register node and return node-id if node
;;                      ;; didn't exist before

(def dummy-connections {
                        "A" { :points-to #{ "B" "C" }
                             :pointed-by #{ "C" "E" } }
                        "B" { :points-to #{ "C" }
                             :pointed-by #{ "A" "E" } }
                        "C" { :points-to #{ "E" }
                             :pointed-by #{ "A" "B" } }
                        "E" { :points-to #{ "A" "B" }
                             :pointed-by #{ "C" } }
                        "F" { :points-to #{ "G" }
                             :pointed-by #{ } }
                        "G" { :points-to #{}
                             :pointed-by #{ "F" } }
                        })

(def initial-edges {
                    :edges nil
                    :last-result nil
                    :inc 0 })

(def initial-nodes {
                    :nodes nil
                    :reverse nil
                    :last-result nil
                    :inc 0 })

(defonce store (atom nil))
(defonce to-dl (atom []))
(defonce to-process (atom []))
(defonce to-register (atom []))
(defonce nodes (atom initial-nodes))
(defonce edges (atom nil))
(defonce raw-edges (atom initial-edges))

(defn get-node-id [node]
  (get-in @nodes [:reverse node]))

(defmethod can3p.traveller.protocol/init-store :dummy [conf]
  (reset! store nil)
  (reset! to-dl [])
  (reset! to-process [])
  (reset! to-register [])
  (reset! nodes initial-nodes)
  (reset! edges nil)
  (reset! raw-edges initial-edges)
  nil)

(defmethod can3p.traveller.protocol/fetch-node-to-dl :dummy [conf]
  (when (> (count @to-dl) 0)
    (let [[old new] (swap-vals! to-dl pop)] ;; pop removes the first
      (peek old))))

(defmethod can3p.traveller.protocol/schedule-processing :dummy [conf store-id]
  (swap! to-process conj store-id))

(defmethod can3p.traveller.protocol/schedule-register :dummy [conf node]
  (swap! to-register conj node))

(defmethod can3p.traveller.protocol/fetch-store-id-to-process :dummy [conf]
  (when (> (count @to-process) 0)
    (let [[old new] (swap-vals! to-process pop)] ;; pop removes the first
      (peek old))))

(defmethod can3p.traveller.protocol/schedule-download :dummy [conf node-ids]
  (swap! to-dl #(apply conj % node-ids)))

(defmethod can3p.traveller.protocol/fetch-node-to-register :dummy [conf]
  (when (> (count @to-register) 0)
    (let [[old new] (swap-vals! to-register pop)] ;; pop removes the first
      (peek old))))

(defmethod can3p.traveller.protocol/fetch-edges :dummy [conf node-id]
  (if-let [node (get-in @nodes [:nodes node-id])]
    (get dummy-connections node)))

(defmethod can3p.traveller.protocol/store-edges :dummy [conf node-id edges]
  (let [new (swap! raw-edges (fn [store]
                               (let [new-inc (inc (:inc store))
                                     store-map {
                                                :edges edges
                                                :node-id node-id
                                                }]
                                 {
                                  :edges (assoc (:edges store) new-inc store-map)
                                  :last-result new-inc
                                  :inc new-inc
                                  })))]
    (:last-result new)))

(defmethod can3p.traveller.protocol/retrieve-edges :dummy [conf store-id]
  (get-in @raw-edges [:edges store-id]))

(defmethod can3p.traveller.protocol/register-edges :dummy [conf edges-map]
  (let [node-id (:node-id edges-map)
        new-edges (:edges edges-map)
        new-node-ids (can3p.traveller.protocol/register-nodes conf
                                                      (set/union
                                                       (:points-to new-edges)
                                                       (:pointed-by new-edges)))
        new-connections (concat
                         (map vector (repeat node-id) (map get-node-id (:points-to new-edges)))
                         (map vector (map get-node-id (:pointed-by new-edges)) (repeat node-id)))
        new-connections-hash (reduce (fn [acc [k v]] (assoc acc k (conj (get acc k #{}) v)))
                                     {} new-connections)]
    (swap! edges #(merge-with set/union % new-connections-hash))
    new-node-ids))

(defmethod can3p.traveller.protocol/register-node :dummy [conf node]
  (let [new (swap! nodes (fn [store]
                           (if (get-in store [:reverse node])
                             (assoc store :last-result nil)
                             (let [new-inc (inc (:inc store))]
                               {
                                :nodes (assoc (:nodes store) new-inc node)
                                :reverse (assoc (:reverse store) node new-inc)
                                :last-result new-inc
                                :inc new-inc }))))]
    (:last-result new)))

(defmethod can3p.traveller.protocol/register-nodes :dummy [conf nodes]
  (into []
        (filter some? (map #(can3p.traveller.protocol/register-node conf %1) nodes))))
