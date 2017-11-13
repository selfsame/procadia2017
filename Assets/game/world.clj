(ns game.world
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics)
  (import 
    [OverlapWFC]
    [UnityEngine GameObject]))

(defn spawn-points []
  (remove null-obj? (shuffle (objects-tagged "spawn"))))

(defn make-world [path w h]
  (let [tiled (clone! :tiled)
        border (clone! :world-border)
        wfc (.GetComponent tiled "SimpleTiledWFC")]
    (set! (.xmlpath wfc) (str "Resources/" path))
    (set! (.width wfc) (int w))
    (set! (.depth wfc) (int h))
    (.Generate wfc)
    (.Run wfc)
    (destroy wfc)
    (parent! border tiled)
    (local-position! border (v3+ (>v3 tiled) (v3 w h 0) (v3 -1 -1 0)))
    (local-scale! border (v3 w h 2))
    tiled))

'(do 
  (clear-cloned!)
  (game.world/make-world "worlds/world.xml" 10 10))


(defn make-overlap-world [k w h]
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
    (set-mask! overlap "level")
    overlap))