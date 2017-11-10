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
    game.fx)
  (import [UnityEngine Debug]))

(defn ai-start [o _]
  (state+ o ::target @PLAYER))

(defn ai-update [o _]
  (let [target (>v3 @PLAYER)
        o->t (v3- target (>v3 o))
        movement (.normalized o->t)
        to-target (v2 (.x movement) (.z movement))
        dist (.magnitude o->t)
        h (hit (v3+ (>v3 o) (v3 0 0.5 0)) o->t dist (mask "level"))]
    ;(if h (Debug/DrawLine (>v3 o) (.point h) (color 1 0 0)))
    (if (> 60 dist)
      (state+ o :input {
        :movement 
        (if (> dist 15.0) to-target 
            (if (> 10.0 dist) (v2* to-target -1)
                (v2 0)))
        :mouse-intersection target
        :aim to-target
        :buttons-pressed 
        (if (and (not h) (< (rand) 0.2)) #{:fire} #{})})
      (state+ o :input {}))))