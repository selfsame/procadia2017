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
    [magic.api :as m]
    game.fx
    game.play)
  (import 
    Bone
    Timer
    [UnityEngine Mathf Time GameObject]))


(defn feet-move [^UnityEngine.GameObject o ^UnityEngine.GameObject this movement] 
  (when movement 
    (let [^UnityEngine.GameObject axis @CAMERA-AXIS
          movement (v3 (.x movement) 0 (.y movement))
          rb (->rigidbody o)
          vel (v3* (v3 (.x movement) -0.5 (.z movement)) 15)
          speed (.magnitude movement)]
      (param-float this "walkspeed" (* speed 1.5))
      (set! (.velocity rb) vel)
      (when (> (.magnitude movement) 0.001)
        (lerp-look! this (v3+ (>v3 this) movement) 0.2)))))

(defn body-update [^UnityEngine.GameObject o ^UnityEngine.GameObject this]
  (let [{:keys [aim mouse-intersection]} (state o :input)
        ^UnityEngine.Vector2 aim aim]
    (when aim 
      (lerp-look! this (v3+ (>v3 this) 
                         (v3 (.x aim) 0 (.y aim))) 0.4))))

(defn gun-update [^UnityEngine.GameObject o ^UnityEngine.GameObject this]
  (let [buttons-pressed (:buttons-pressed (state o :input))
        ^Timer timer (cmpt this Timer)] 
    (set! (.value timer) (int (+ (.value timer) 1)))
    (when (and (:fire buttons-pressed)
               (> (.value timer) 10))   
        (set! (.value timer) (int 0))
        (let [bullet (clone! :bullets/pellet)
              layer (int (state o :mask))
              ^UnityEngine.Transform btrf (.transform bullet)]
          (position! bullet (>v3 this))
          (set! (.rotation btrf) (.rotation (.transform this)))
          (timeline*
            (fn [] 
              (when-let [hit (hit (>v3 bullet) (.forward btrf) (∆ 20) layer)]
                (game.play/damage (.gameObject (.collider hit)) 2)
                (let [spark (clone! :fx/spark)] 
                  (position! spark (.point hit))
                  (destroy spark 0.5))
                (destroy bullet 0.01))
              (position! bullet 
                (v3+ (>v3 bullet) (local-direction bullet (v3 0 0 (∆ 20)))))))
          (destroy bullet 3.0)))))

(part {
  :type :feet
  :id :business
  :prefab :parts/boots
  :mount-points {
    :body {:body 5
           :blob 1}} 
  :hooks {
    :move #'feet-move}})

(part {
  :type :body
  :id :business
  :prefab :parts/business-body
  :mount-points {
    :neck {:head 3 :tentacle 1}
    :left-arm {:arm 1 }
    :right-arm {:arm 1 }} 
  :hooks {:update #'body-update}})

(part {
  :type :blob
  :id :blob
  :prefab :parts/blob
  :hp 3
  :mount-points {
    :arm1 {:tentacle 1}
    :arm2 {:tentacle 1}
    :arm3 {:tentacle 1}
    :arm4 {:tentacle 1}
    :arm5 {:tentacle 1}
    :arm6 {:tentacle 1}} 
  :hooks {:update #'body-update}})

(part {
  :type :head
  :id :business
  :prefab :parts/business-head
  :hooks {
    :update 
    (fn [root this])}})

(part {
  :type :arm
  :id :business
  :prefab :parts/business-arm
  :mount-points {
    :item {:item 1}}
  :hooks {:aim #'game.play/arm-update}})

(part {
  :type :head
  :id :eyeball
  :prefab :parts/eyeball
  :hp 2})

(part {
  :type [:arm :tentacle]
  :id :tentacle
  :prefab :parts/rag-tentacle-k
  :mount-points {
    :item {:item 1
           :nil 1}}})

(part {
  :type :item
  :id :raygun
  :prefab :parts/raygun
  :power 1
  :hooks {
    :start (fn [root this] (cmpt+ this Timer))
    :update #'gun-update}})





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
          (set! (.max (.limits hj)) 10))
        bone)
      o bones)))

'(hook+ (the blob) :start #'kino-settle)
'(hook+ (the rag-tentacle-k) :update #'kino-match)

