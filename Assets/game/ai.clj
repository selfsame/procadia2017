(ns game.ai
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    tween.core
    game.data)
  (require
    game.fx
    [magic.api :as m])
  (import 
    [UnityEngine Debug Vector3 GameObject]))

(defmacro behaviour [bind & args]
  (let [wrapped (map #(list 'fn '[] %) args)]
    `(fn ~bind [~@wrapped])))


(defn player-in-range? [^GameObject o n]
  (fn []
    (let [^Vector3 target (>v3 @PLAYER)
          o->t (v3- target (>v3 o))
          dist (.magnitude o->t)]
      (if (< dist n) true false))))

(defn charge [o]
  (fn [] 
    (let [input (state o :input)
          target (v3+ (>v3 @PLAYER) (v3 0 1.2 0))
          o->t (v3- target (>v3 o))
          movement (.normalized o->t)]
      (Debug/DrawLine (>v3 o) target (color 1 0 0))    
      (set! (.movement input) movement)
      (set! (.aim input) movement)
      (set! (.target input) target)) true))

(defn strafe [o n]
  (fn [] 
    (let [input (state o :input)
          target (>v3 @PLAYER)
          o->t (.normalized (v3- target (>v3 o)))
          perp (v3* (Vector3/Cross o->t (v3 0 1 0)) n)]
      (Debug/DrawLine (>v3 o) (v3+ (>v3 o) perp) (color 0 1 0))
      (set! (.movement input) perp))))

(defn aim [o]
  (fn [] 
    (let [input (state o :input)
          target (v3+ (>v3 @PLAYER) (v3 0 1.2 0))
          o->t (v3- target (>v3 o))
          movement (.normalized o->t)]
      (set! (.aim input) movement)
      (set! (.target input) target))))

(defn wander [o]
  (fn [] 
    (let [input (state o :input)
          m (.normalized (?circle 2.0))
          movement (v3 (.x m) 0 (.y m))]
      (set! (.movement input) movement)
      (set! (.aim input) movement)
      (set! (.target input) (v3+ (>v3 o) movement)) 
      nil)))

(defn stop [o]
  (fn [] (set! (.movement (state o :input)) (v3 0)) nil))

(defn fire [o]
  (fn [] (set! (.pressed (state o :input)) #{:fire}) nil))

(defn end-fire [o]
  (fn [] (set! (.pressed (state o :input)) #{}) nil))

(def default-ai 
  (behaviour [o]
    (wait (?f 1))
    (NOT (player-in-range? o 40))
    (AND (charge o)
         (NOT (player-in-range? o 6))
         (wait (?f 0.3 1.5)))
    (stop o)
    (fire o)
    (AND (aim o)
         (wait (?f 0.5 2.0)))
    (end-fire o)
    (wander o)
    (wait (?f 0.5 1.0))
    (AND (strafe o (rand-nth [-1 1]))
         (aim o)
         (wait (?f 0.5 2.0)))
    (stop o)))

(defn ai-start [o _]
  (timeline-1 
    (cycle (or (state o :game.entity/ai) 
               (default-ai o)))))