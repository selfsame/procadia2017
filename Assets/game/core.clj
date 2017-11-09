(ns game.core
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    game.data
    tween.core)
  (require
    input.core
    game.world
    game.entity
    game.ai
    selfsame))

(defn update-camera [o _]
  (position! o 
    (lerp (>v3 o)
          (v3+ (>v3 @PLAYER) (v3 -50 70 -50)) 0.1)))

(defn make-level [depth]
  (let [world (game.world/make-world :worlds/cubeworld 40 40)
        _ (local-scale! world (v3 2))
        spawn-points (game.world/spawn-points)
        player-input (clone! :player-input)
        player (game.entity/make-entity (* depth 10))
        monsters 
        (dorun 
          (map 
            (fn [sp] 
              (let [monster (game.entity/make-entity (* depth 10))]
                (position! monster (.position (.transform sp)))
                (hook+ monster :start :ai #'game.ai/ai-start)
                (hook+ monster :update :ai #'game.ai/ai-update)
                monster))
            (take 20 (rest spawn-points))))
        camera (clone! :iso-camera)]
    (reset! PLAYER player)
    (reset! INPUT player-input)
    (reset! CAMERA camera)
    (reset! CAMERA-AXIS (first (children camera)))
    (reset! AIM (clone! :cube))
    (hook+ player-input :update #'input.core/push-input!)
    (state+ player-input :output-obj player)
    (position! player (.position (.transform (first spawn-points))))
    (hook+ camera :update #'update-camera)))

(defn start [_ _]
  (clear-cloned!)
  (destroy-immediate (the overlap))
  (make-level 1))

'(start nil nil)
