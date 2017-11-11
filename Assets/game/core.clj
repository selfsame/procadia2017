(ns game.core
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    game.data
    tween.core)
  (require
    input.core
    game.world
    [game.entity :refer :all]
    game.play
    game.ai
    selfsame
    clojure.core.server
    [magic.api :as m])
  (import [UnityEngine GameObject Vector3]))

(m/defn update-camera [^GameObject o _]
  (let [^GameObject player @PLAYER]
    (set! (>v3 o) 
          (Vector3/Lerp
            (>v3 o)
            (v3+ (>v3 player) (v3 -50 70 -50))
            0.1)))
  nil)

(defn make-level [depth]
  (let [world (game.world/make-world :worlds/cubeworld 25 25)
        _ (local-scale! world (v3 4))
        spawn-points (game.world/spawn-points)
        player-input (clone! :player-input)
        player (game.entity/make-entity :player-feet (* depth 10))
        monsters 
        (dorun 
          (map 
            (fn [sp] 
              (let [monster (game.entity/make-entity (* depth 10))]
                (position! monster (.position (.transform sp)))
                (hook+ monster :start :ai #'game.ai/ai-start)
                (state+ monster :input (input.core/new-control))
                (set-mask! monster "monster")
                (state+ monster :mask (int (+ (mask "level") (mask "player"))))
                monster))
            (take 20 (rest spawn-points))))
        camera (clone! :iso-camera)]
    (reset! INPUT player-input)
    (reset! CAMERA camera)
    (reset! CAMERA-AXIS (first (children camera)))
    (reset! AIM (clone! :cube))
    (hook+ player-input :update #'input.core/push-input!)
    (game.play/set-player! player)
    (state+ player-input :output-obj player)
    (position! player (.position (.transform (first spawn-points))))
    (hook+ camera :update #'update-camera)))

(defn start [_ _]
  (clear-cloned!)
  (destroy-immediate (the overlap))
  (make-level 1))

'(start nil nil)


;;Special starting player parts

(part {
  :type :player-feet
  :id :player
  :prefab :parts/boots
  :mount-points {
    :body {:player-body 1}} 
  :hooks {
    :move #'selfsame/feet-move}})

(part {
  :type :player-body
  :id :player
  :prefab :parts/business-body
  :mount-points {
    :neck {:player-head 1}
    :left-arm {:player-arm 1 }
    :right-arm {:player-arm 1 }} 
  :hooks {:update #'selfsame/body-update}})

(part {
  :type :player-head
  :id :player
  :prefab :parts/business-head})

(part {
  :type :player-arm
  :id :player
  :prefab :parts/business-arm
  :mount-points {
    :item {:item 1}}
  :hooks {:aim #'game.play/arm-update}})