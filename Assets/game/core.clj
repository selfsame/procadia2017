(ns game.core
  (use
    arcadia.core
    arcadia.linear
    hard.core)
  (require
    game.world
    game.entity))

(def PLAYER (atom nil))

(defn update-camera [o _]
  (position! o 
    (lerp (>v3 o)
          (v3+ (>v3 @PLAYER) (v3 -50 70 -50)) 0.1)))

(defn make-level [depth]
  (let [world (game.world/make-world :worlds/cubeworld 40 40)
        _ (local-scale! world (v3 2))
        spawn-points (game.world/spawn-points)
        player (game.entity/make-entity (* depth 10))
        monsters 
        (dorun 
          (map 
            (fn [sp] 
              (let [monster (game.entity/make-entity (* depth 10))]
                (position! monster (.position (.transform sp)))
                monster))
            (take 15 (rest spawn-points))))
        camera (clone! :iso-camera)]
    (reset! PLAYER player)
    (position! player (.position (.transform (first spawn-points))))
    (hook+ camera :update :update #'update-camera)))

(defn start [_ _]
  (clear-cloned!)
  (destroy-immediate (the overlap))
  (make-level 1))

'(start nil nil)

'(hook+ (the start) :start :start #'start)





