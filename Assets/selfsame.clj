(ns selfsame
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    hard.animation
    game.entity
    game.data)
  (import 
    Bone
    [UnityEngine Mathf Time]))


(defn feet-move [o this movement] 
  (when movement 
    (let [movement 
          (.TransformDirection 
            (.transform @CAMERA-AXIS) 
            (v3 (.x movement) 0 (.y movement)))
          rb (->rigidbody o)
          vel (v3* (v3 (.x movement) -0.5 (.z movement)) 15)
          speed (.magnitude movement)]
      (param-float this "walkspeed" (* speed 1.5))
      (set! (.velocity rb) vel)
      (when (> (.magnitude movement) 0.001)
        (lerp-look! this (v3+ (>v3 this) movement) 0.2)))))

(defn body-update [o this]
  (let [{:keys [movement aim mouse-intersection]} (state o :input)]
    (when aim 
      (position! @AIM mouse-intersection)
      (lerp-look! this (v3+ (>v3 this) 
                         (v3 (.x aim) 0 (.y aim))) 0.4))))

(part {
  :type :feet
  :id :business
  :prefab :parts/boots
  :mount-points {
    :body {:body 1}} 
  :hooks {
    :move #'feet-move}})

(part {
  :type :body
  :id :business
  :prefab :parts/business-body
  :mount-points {
    :neck {:head 1}
    :left-arm {:arm 1 :head 1}
    :right-arm {:arm 1 :head 1}} 
  :hooks {:update #'body-update}})

(part {
  :type :head
  :id :business
  :prefab :parts/business-head
  :hooks {
    :update 
    (fn [root this] )
    }})

(part {
  :type :arm
  :id :business
  :prefab :parts/business-arm
  :hooks {:aim (fn [root this aim])}})

(part {
  :type :head
  :id :eyeball
  :prefab :parts/eyeball})


(defn update-tentacle [o _] 
  (let [bones (map #(.gameObject %) (.GetComponentsInChildren o Bone))
        offset (or (data o :offset)
                   (do (data! o :offset (rand-int 100)) (data o :offset)))]
    (dorun
      (map-indexed
        #(rotate! %2 (v3 0 0  (* (Mathf/Cos (+ (* (+ Time/time offset) 2 ) %1)) 1.9) ))
        bones))))


'(hook+ (clone! :parts/tentacle) :update #'update-tentacle)