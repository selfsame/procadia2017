(ns game.world
  (use
    arcadia.core
    arcadia.linear
    hard.core)
  (import 
    [OverlapWFC]
    [UnityEngine GameObject]))




(defn arm [entity]
  {:object (GameObject. "arm")
   :mounts [{:point (v3 1 0 0) :type :weapon}]
   :update (fn [this input])
   :hp 10})


(defn spawn-points []
  (remove null-obj? (shuffle (objects-tagged "spawn"))))

(defn make-world [k w h]
  (let [input (clone! k)
        overlap (clone! :overlap)
        training (.GetComponent input "Training")
        wfc (.GetComponent overlap "OverlapWFC")]
    (set! (.training wfc) training)
    (set! (.width wfc) (int w))
    (set! (.depth wfc) (int h))
    (.Generate wfc)
    (.Run wfc)
    (destroy wfc)
    (destroy-immediate input)
    overlap))