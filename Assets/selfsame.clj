(ns selfsame
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    hard.animation
    hard.sound
    game.entity
    game.data
    tween.core
    game.ai)
  (require
    [magic.api :as m]
    game.fx
    game.play)
  (import 
    Bone
    Timer
    [UnityEngine Mathf Time GameObject Vector3]))

(defn color! [o c]
  (set! (.color (aget (.materials (cmpt o UnityEngine.MeshRenderer)) 0)) c))

(defn trail-color! [o c]
  (set! (.color (aget (.materials (cmpt o UnityEngine.TrailRenderer)) 0)) c))

(defn feet-move [^UnityEngine.GameObject o ^UnityEngine.GameObject this movement] 
  (when movement 
    (let [last-vel (or (state this ::move) (v3 0))
          movement (v3* (v3+ movement last-vel) 0.5)
          rb (->rigidbody o)
          vel (v3+ (v3* movement 12) (v3 0 (.y (.velocity rb)) 0))
          speed (.magnitude movement)]
      (state+ this ::move movement)
      (param-float this "walkspeed" (* speed 1.5))
      (set! (.velocity rb) vel)
      (when (> (.magnitude movement) 0.001)
        (lerp-look! this (v3+ (>v3 this) movement) 0.2)))))

(defn body-update [^UnityEngine.GameObject o ^UnityEngine.GameObject this]
  (let [input (state o :input)
        ^Vector3 aim (.aim input)]
    (when (> (.magnitude aim) 0.01) 
      (lerp-look! this (v3+ (>v3 this) aim) 0.4))))

(defn y-look [^UnityEngine.GameObject o ^UnityEngine.GameObject this]
  (let [input (state o :input)]
    (when input
      (look-at! this (v3+ (.target input) (v3 0 5 0)) (v3 0 1 0))
      (rotate! this (v3 90 0 0)))))

(defn gun-update [^UnityEngine.GameObject o ^UnityEngine.GameObject this]
  (let [pressed (.pressed (state o :input))
        ^Timer timer (cmpt this Timer)] 
    (set! (.value timer) (int (+ (.value timer) 1)))
    (when (and (:fire pressed)
               (> (.value timer) 10))   
        (set! (.value timer) (int 0))
        (let [bullet (clone! :bullets/pellet)
              layer (int (state o :mask))
              ^UnityEngine.Transform btrf (.transform bullet)]
          (play-clip! (rand-nth ["hurt1" "hurt2"]) {:volume 0.2})
          (position! bullet (>v3 this))
          (color! bullet (:hue (state o)))
          (trail-color! bullet (:hue (state o)))
          (set! (.rotation btrf) (.rotation (.transform this)))
          (timeline*
            (fn [] 
              (when-let [hit (hit (>v3 bullet) (.forward btrf) (∆ 30) layer)]
                (game.play/damage (.gameObject (.collider hit)) 1)
                (let [spark (clone! :fx/spark)] 
                  (position! spark (.point hit))
                  (destroy spark 0.5))
                (destroy bullet 0.01))
              (position! bullet 
                (v3+ (>v3 bullet) (local-direction bullet (v3 0 0 (∆ 30)))))))
          (destroy bullet 3.0)))))


(defn cannon-update [^UnityEngine.GameObject o ^UnityEngine.GameObject this]
  (let [pressed (.pressed (state o :input))
        ^Timer timer (cmpt this Timer)
        b (state this :cannon)]
    (set! (.value timer) (int (+ (.value timer) 1)))
    (look-at! b (v3+ (.target (state o :input)) (v3 0 1 0)) (v3 0 1 0))
    (when (and (:fire pressed)
               (> (.value timer) 40))   
        (set! (.value timer) (int 0))
        (let [bullet (clone! :bullets/cannonball)
              layer (int (state o :mask))
              ^UnityEngine.Transform btrf (.transform bullet)]
          (play-clip! "fire" {:volume 0.4})
          (position! bullet (>v3 this))
          (set! (.rotation btrf) (.rotation (.transform b)))
          (timeline*
            (fn [] 
              (when-let [hit (hit (>v3 bullet) (.forward btrf) (∆ 20) layer)]
                (let [found 
                      (distinct (filter 
                        #(state % :entity?)
                        (map 
                        #(.gameObject (.root (.transform (.gameObject %))))
                        (overlap-sphere (.point hit) 2.0 layer))))]
                    (run! #(game.play/damage % 3) found))
                (let [smoke (game.fx/smoke (.point hit))] 
                  (local-scale! smoke (v3 0.7)))
                (play-clip! "explosion.wav" {:volume 0.4})
                (destroy bullet 0.01))
              (position! bullet 
                (v3+ (>v3 bullet) (local-direction bullet (v3 0 0 (∆ 20)))))))
          (let [point (transform-point b (v3 0 -0.2 2))
                smoke (game.fx/smoke point)]
            (rotation! smoke (rotation b))
            (local-scale! smoke (v3 0.4))
            (timeline*
              (tween {:local {:scale (v3 1 1 0.5)}} b 0.2 {:out :pow2})
              (tween {:local {:scale (v3 1 1 1)}} b 0.6 {:in :pow2})))
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
  :hp 2
  :hooks {:update #'y-look}
  :ai 
  (behaviour [o]
    (NOT (player-in-range? o 40))
    (AND (charge o)
         (NOT (player-in-range? o 15))
         (wait (?f 0.3 1)))
    (stop o)
    (fire o)
    (AND (strafe o (rand-nth [-1 1]))
         (aim o)
         (wait (?f 0.2 1.0)))
    (end-fire o)
    (stop o))})

(part {
  :type [:arm :tentacle]
  :id :tentacle
  :prefab :parts/rag-tentacle-k
  :mount-points {
    :item {:nil 1}}})

(part {
  :type :item
  :id :raygun
  :prefab :parts/raygun
  :power 1
  :hooks {
    :start (fn [root this] (cmpt+ this Timer))
    :update #'gun-update}})


(part {
  :type :feet
  :id :snake-feet
  :prefab :parts/snake
  :mount-points {
    :body {:body 1}} 
  :hooks {
    :move #'feet-move}})

(part {
  :type :body
  :id :snake
  :prefab :parts/snake-body
  :mount-points {
    :neck {:head 4
           :arm 1}
    :left-arm {:arm 1 }
    :right-arm {:arm 1 }} 
  :hooks {:update #'body-update}})


(part {
  :type :body
  :id :tank
  :prefab :parts/tank-body
  :hp 3
  :mount-points {
    :head {:head 1}
    :leftarm {:arm 2 :cannon 1}
    :rightarm {:arm 2 :cannon 1}} 
  :hooks {:update #'body-update}})


(part {
  :type :arm
  :id :claw
  :prefab :parts/claw
  :mount-points {
    :item {:item 1}}
  :hooks {:aim #'game.play/arm-update}})

(part {
  :type :arm
  :id :double
  :prefab :parts/double-arm
  :mount-points {
    :arm1 {
      :arm 4
      :head 1
      :nil 1}
    :arm2 {
      :arm 4
      :head 1
      :nil 1}}})

(part {
  :type [:cannon :head]
  :id :tank
  :prefab :parts/cannon
  :power 3
  :hooks {
    :start (fn [root this] (cmpt+ this Timer)
      (state+ this :cannon (child-named this "barrell")))
    :update #'cannon-update}})


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

