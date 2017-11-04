(ns game.core
  (use
    arcadia.core
    arcadia.linear
    hard.core)
  (require
    [game.world]))

(defn start [_ _]
  (clear-cloned!)
  (clone! :iso-camera)
  (let [world (game.world/make-world :worlds/cubeworld 40 40)
        spawn-points (game.world/spawn-points)
        player (clone! :player)]
    (position! player (.position (.transform (first spawn-points))))))

'(start nil nil)

'(hook+ (the start) :start :start #'start)
