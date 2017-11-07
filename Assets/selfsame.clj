(ns selfsame
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    hard.animation
    game.entity
    game.data
    tween.core)
  (require
    [magic.api :as m])
  (import 
    Bone
    [UnityEngine Mathf Time GameObject]))


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
    :left-arm {:arm 1 }
    :right-arm {:arm 1 }} 
  :hooks {:update #'body-update}})

(part {
  :type :body
  :id :blob
  :prefab :parts/blob
  :mount-points {
    :arm1 {:arm 1}
    :arm2 {:arm 1}
    :arm3 {:arm 1}
    :arm4 {:arm 1}
    :arm5 {:arm 1}
    :arm6 {:arm 1}} 
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


(part {
  :type :arm
  :id :tentacle
  :prefab :parts/rag-tentacle-k})


(defn kino-settle [^UnityEngine.GameObject o _]
  (let [root (.. o transform root gameObject)
        rb (cmpt root UnityEngine.Rigidbody)]
    (set! (.isKinematic rb) true)
    (timeline*
      (wait 0.2)
      #(do (set! (.isKinematic rb) false) nil))))


(defn attach-rb [o _]
  (let [root (.. o transform root gameObject)
        rb (cmpt root UnityEngine.Rigidbody)
        hj (cmpt+ o UnityEngine.HingeJoint)]
    (log root rb)
    (set! (.isKinematic (->rigidbody o)) false)
    (set! (.connectedBody hj) rb)))

(m/defn kino-match [^UnityEngine.GameObject o _]
  ;(set! (.position (.transform o)) (.position (.transform (.parent (.transform o)))))
  ;(set! (.rotation (.transform o)) (.rotation (.transform (.parent (.transform o)))))
  )

'(do
  (clear-cloned!)
  (let [o (clone! :parts/tentacle)
        bones (map #(.gameObject %) (.GetComponentsInChildren o Bone))]
    (reduce
      (fn [prev bone]
        (let [rb (cmpt+ bone UnityEngine.Rigidbody)
              hj (cmpt+ bone UnityEngine.HingeJoint)
              cc (cmpt+ bone UnityEngine.CapsuleCollider)]
          (set! (.height cc) (float 0.009))
          (set! (.radius cc) (float 0.002))
          (set! (.center cc) (v3 0 0.004 0))
          (set! (.connectedBody hj) (cmpt prev UnityEngine.Rigidbody))
          (set! (.useLimits hj) true)
          (set! (.min (.limits hj)) -10)
          (set! (.max (.limits hj)) 10)
          )
        bone
        )
      o
      bones)

    ))

'(hook+ (the blob) :start #'kino-settle)

'(hook+ (the rag-tentacle-k) :update #'kino-match)