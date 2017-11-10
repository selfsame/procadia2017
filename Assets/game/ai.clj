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

(defn input! [o k v] (update-state o :input #(assoc % k v)))

(defn player-in-range? [o n]
  (fn []
    (let [target (>v3 @PLAYER)
          o->t (v3- target (>v3 o))
          dist (.magnitude o->t)]
      (if (< dist n) false true))))

(defn charge [o]
  (fn [] 
    (let [target (>v3 @PLAYER)
          o->t (v3- target (>v3 o))
          movement (.normalized o->t)
          to-target (v2 (.x movement) (.z movement))]
      (input! o :movement to-target)
      (input! o :aim to-target)
      (input! o :mouse-intersection target))))

(defn wander [o]
  (fn [] 
    (let [movement (.normalized (?circle 2.0))]
      (input! o :movement movement)
      (input! o :aim movement)
      (input! o :mouse-intersection (v3+ (>v3 o) (v3 (.x movement) 0 (.y movement)))) 
      nil)))

(defn stop [o]
  (fn [] (input! o :movement (v2 0)) nil))

(defn fire [o]
  (fn [] (input! o :buttons-pressed #{:fire})))

(defn end-fire [o]
  (fn [] (input! o :buttons-pressed #{}) nil))

(defn ai-start [o _]
  (state+ o ::target @PLAYER)
  (timeline* :loop
    (wait (?f 1))
    (player-in-range? o 40)
    (AND (charge o)
         (wait 1.5))
    (stop o)
    (AND (fire o)
         (wait (?f 0.5 2.0)))
    (end-fire o)
    (wander o)
    (wait (?f 0.5 1.0))
    (stop o)))




(defn ai-update [o _]
  #_(let [target (>v3 @PLAYER)
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
      (state+ o :input {}))) )