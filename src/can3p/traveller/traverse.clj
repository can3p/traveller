(ns can3p.traveller.traverse
  (:use [can3p.traveller.protocol])
  (:require [clojure.core.async :refer [thread]]
            [taoensso.timbre :as timbre
             :refer (log  trace  debug  debugf info  warn  error  fatal  report)]))

(def initial-state { :terminate nil })
(def state (atom nil))

(def sleep-delay 3000)
(def empty-thershold 10000)

(defn all-empty? [state]
  (let [val @state]
    (and
     (:dl-empty val)
     (:pc-empty val)
     (:ks-empty val))))

(defn terminate? []
  (:terminate @state))

(defn terminate! []
  (swap! state assoc :terminate true))

(defn empty! [key]
  (swap! state #(assoc % key true)))

(defn not-empty! [key]
  (swap! state #(assoc % key nil)))

(defn sleep [delay]
  (Thread/sleep delay))

(defmacro wrap-thread [name body & [ catch ] ]
  (let [e (gensym 'message)]
    `(thread
       (debugf "Starting %s" ~name)
       (try
         ~body
         (catch Exception ~e
           (debugf "Exception occured while running %s: %s " ~name ~e)
           ~catch
           ))
       (debugf "Finishing %s" ~name))))

(defmacro wrap [body]
  (let [e (gensym 'message)]
    `(try
      ~body
      (catch Exception ~e
        (debugf "Exception occured while running %s: %s " (quote ~body) ~e)
        (throw ~e)
        ))))

(defn downloader
  "Poll new nodes feed in a loop and download them"
  [conf]
  (loop [ node-id (fetch-node-to-dl conf)]
    (debug "downloader new item in line" node-id)
    (when-not (terminate?)
      (if (nil? node-id)
        (do
          (debug "downloader empty")
          (empty! :dl-empty)
          (sleep sleep-delay)
          (recur (fetch-node-to-dl conf)))
        (let [ edges (fetch-edges conf node-id) ]
          (debug "node fetched" edges)
          (not-empty! :dl-empty)
          (store-edges conf node-id edges)
          (schedule-processing conf
                               (store-edges conf node-id edges))
          (recur (fetch-node-to-dl conf)))))))

(defn processor
  "Poll new edges feed in a loop and poulate nodes/edges data structure"
  [conf]
  (loop [ store-id (fetch-store-id-to-process conf)]
    (debug "processor new item in in line" store-id)
    (when-not (terminate?)
      (if (nil? store-id)
        (do
          (debug "processor empty")
          (empty! :pc-empty)
          (sleep sleep-delay)
          (recur (fetch-store-id-to-process conf)))
        (let [ edges (wrap (retrieve-edges conf store-id)) ]
          (debug "processor new item in store" store-id)
          (not-empty! :pc-empty)
          (wrap (schedule-download conf
                             (wrap (register-edges conf edges))))
          (recur (wrap (fetch-store-id-to-process conf))))))))

(defn kickstarter
  "Poll new kickstart nodes feed in a loop and poulate nodes/edges data structure"
  [conf]
  (debug "Start kickstarter")
  (loop [ node (fetch-node-to-register conf)]
    (when-not (terminate?)
      (if (nil? node)
        (do
          (debug "kickstarter empty")
          (empty! :ks-empty)
          (sleep sleep-delay)
          (recur (fetch-node-to-register conf)))
        (let [ node-id (register-node conf node)]
          (debug "kickstarter new node in queue" node)
          (not-empty! :ks-empty)
          (when node-id
            (schedule-download conf [node-id]))
          (recur (fetch-node-to-register conf)))))))

(defn terminator
  "When run in a thread function watches if all others signal end of processing
   and raises a flag so the everybody terminates"
  [conf]
  (loop [ th 0 ]
    (when-not (terminate?)
      (if (all-empty? state)
        (when (< th empty-thershold)
          (debug "Terminator will sleep, current delay is " th)
          (sleep sleep-delay)
          (recur (+ th sleep-delay)))
        (do
          (debug "Terminator still has work to do")
          (sleep sleep-delay)
          (recur 0))))))

(defn traverse
  "Traverse a graph using can3p.traveller.protocol"
  [conf]
  (reset! state initial-state)
  (init-store conf)
  (wrap-thread "downloader"
               (downloader conf)
               (terminate!)
               )
  (wrap-thread "processor"
               (processor conf)
               (terminate!)
               )
  (wrap-thread "kickstarter"
               (kickstarter conf)
               (terminate!)
               )
  (when (:can-terminate conf)
    (wrap-thread "terminator"
                 (do
                   (terminator conf)
                   (terminate!))
                 (terminate!)
                 )))
