(ns game.fx
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    tween.core))




'(timeline*
  (fn []
    (let [o (the smoke)
          smr (cmpt (the smoke) UnityEngine.SkinnedMeshRenderer)]
      (.SetBlendShapeWeight smr 0 
        (+ (.GetBlendShapeWeight smr 0)
          2))) true))

(defn smoke [pos]
  (let [smoke (clone! :empty)]
    (position! smoke pos)
    (dotimes [i 15]
      (let [o (clone! :fx/smoke)]

        (parent! o smoke)
        (local-position! o (v3 (?f -2 2)(?f 0 2)(?f -2 2)))
        
        (local-scale! o (v3 0))
        (timeline*
          (tween {:local {:scale (v3 1)}} o (?f 0.1 0.3))
          (tween {:local {:scale (v3 0)}} o (?f 0.5 1.5)))
        (timeline*
          (tween {:local {:position (v3+ (local-position o) (v3 (?f -0.2 0.2)(?f 1 2)(?f -0.2 0.2)))}}
            o 1.8)
          (destroy o))))
    (destroy smoke 2)
    smoke))

