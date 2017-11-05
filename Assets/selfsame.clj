(ns selfsame
  (use
    arcadia.core
    arcadia.linear
    hard.core)
  (import 
    Bone
    [UnityEngine Mathf Time]))



(defn update-tentacle [o _] 
  (let [bones (map #(.gameObject %) (.GetComponentsInChildren o Bone))
        offset (or (data o :offset)
                   (do (data! o :offset (rand-int 100)) (data o :offset)))]
    (dorun
      (map-indexed
        #(rotate! %2 (v3 0 0  (* (Mathf/Cos (+ (* (+ Time/time offset) 2 ) %1)) 1.9) ))
        bones))))


'(hook+ (clone! :parts/tentacle) :update #'update-tentacle)