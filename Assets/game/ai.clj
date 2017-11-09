(ns game.ai
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    tween.core
    game.data)
  (require
    game.entity
    game.fx))

(defn ai-start [o _]
  (state+ o ::target @PLAYER))

(defn ai-update [o _]
  (let [target (>v3 @PLAYER)
        o->t (v3- target (>v3 o))
        movement (.normalized o->t)
        to-target (v2 (.x movement) (.z movement))
        dist (.magnitude o->t)]
    (if (> 40 dist)
      (state+ o :input {
        :movement 
        (if (> dist 15.0) to-target 
            (if (> 10.0 dist) (v2* to-target -1)
                (v2 0)))
        :mouse-intersection target
        :aim to-target
        :buttons-pressed #{:fire}}))))