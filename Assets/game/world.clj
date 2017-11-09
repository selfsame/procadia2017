(ns game.world
  (use
    arcadia.core
    arcadia.linear
    hard.core)
  (import 
    [OverlapWFC]
    [UnityEngine GameObject]))

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