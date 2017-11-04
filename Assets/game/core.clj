(ns game.core
  (use
    arcadia.core
    arcadia.linear
    hard.core)
  (require
    [game.world]))

(def spawn (atom nil))

(defn start [_ _]
  (clear-cloned!)
  (let [world (game.world/make-world :worlds/cubeworld 30 30)
        spawn-points (shuffle (objects-tagged "spawn"))
        player (clone! :player)]
    (reset! spawn (first spawn-points))
    (position! player (.position (.transform @spawn)))))

'(start nil nil)

'(hook+ (the start) :start :start #'start)