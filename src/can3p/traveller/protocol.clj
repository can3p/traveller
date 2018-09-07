(ns can3p.traveller.protocol)

;; (init-store)        ;; initial code to reset
;; (fetch-node-to-dl)  ;; get another item to process
;; (fetch-edges node-id) ;; get connections from/to the node
;;                     ;; returns a map with keys :from and :to
;; (store-edges edges) ;; stores connections somewhere and return store-id
;; (schedule-processing store-id)


;; (retrieve-edges store-id) ;; retrieve connections from the store
;; (register-edges edges) ;; register nodes as known if not already
;;                        ;; and return a list of new node-ids
;;                        ;; structure is whatever was returned by
;;                        ;; fetch edges
;; (schedule-download node-ids)

;; (fetch-node-to-register) ;; check if we got any new node to register, nil if not
;; (register-node node) ;; register node and return node-id if node
;;                      ;; didn't exist before

;; This is a protocol that can be used by crawler workers
;; to concurrently download the graph.
;; multimethods depend on a map like:
;; { :store :dummy
;;   :queue :dummy
;;   :transport :dummy
;;   :finish-on-exhaustion t } ;; this is not apart of multimethods, but
;; a control options for implementation code not to hang forever in hope to get new nodes

(defmulti init-store
  "Prepare store and setup connections if necessary"
  (fn [conf] (:store conf)))

(defmulti fetch-node-to-dl
  "Get unchecked node to download"
  (fn [conf] (:queue conf)))

(defmulti fetch-edges
  "Retrieve information about connections for a given node-id"
  (fn [conf node-id] (:transport conf)))

(defmulti store-edges
  "Store downloaded information about edges"
  (fn [conf node-id edges] (:store conf)))

(defmulti schedule-processing
  "Add pointer to the saved information to the queue"
  (fn [conf store-id] (:queue conf)))

(defmulti schedule-register
  "Add a new node to the processing queue"
  (fn [conf node] (:queue conf)))

(defmulti fetch-store-id-to-process
  "Retrieve  a pointer on unprocessed connections"
  (fn [conf] (:queue conf)))

(defmulti retrieve-edges
  "Get information about retrieved connections from the store"
  (fn [conf store-id] (:store conf)))

(defmulti register-edges
  "Register edges and corresponding nodes and mark them as visited"
  (fn [conf edges] (:store conf)))

(defmulti schedule-download
  "Add node-ids into a queue to download"
  (fn [conf node-ids] (:queue conf)))

(defmulti fetch-node-to-register
  "Get a new node from the kickstart queue"
  (fn [conf] (:queue conf)))

(defmulti register-node
  "Register node and return node-id if node didn't exist before"
  (fn [conf node] (:store conf)))

(defmulti register-nodes
  "Register nodes and return a list of node-id from newly created ones"
  (fn [conf nodes] (:store conf)))
