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
            (v3+ (>v3 player) (v3 -20 28 -20))
            0.1)))
  nil)

(defn update-canvas [^GameObject o _]
  (let [health (cmpt (child-named o "health") UnityEngine.UI.Text)
        swapped (cmpt (child-named o "swapped") UnityEngine.UI.Text)
        player (state @PLAYER)]
    (set! (.text health) (str (int (:hp player)) "/" (:max-hp player)))
    (set! (.text swapped) (str (first @SWAPPED) "/" (last @SWAPPED)))))

(defn make-level [depth]
  (let [world (game.world/make-world "worlds/world" 9 9)
        _ (local-scale! world (v3 20))
        sun (clone! :sun)
        event-system (clone! :EventSystem)
        canvas (clone! :Canvas)
        spawn-points (game.world/spawn-points)
        player-input (clone! :player-input)
        player (game.entity/make-entity :player-feet (* depth 10))
        monsters 
        (run! 
          (fn [sp] 
            (let [monster (game.entity/make-entity (* depth 10))]
              (if (< (state monster :power) 1)
                (destroy monster)
                (do 
                  (position! monster (.position (.transform sp)))
                  (hook+ monster :start :ai #'game.ai/ai-start)
                  (set-mask! monster "monster")
                  (state+ monster :mask (int (+ (mask "level") (mask "player"))))
                  monster))))
          (take 30 (rest spawn-points)))
        balls 
        (run! 
          (fn [spawn] 
            (let [ball (clone! :prisoner-ball)]
              (position! ball (v3+ (>v3 spawn) (v3 0 2 0)))))
          (take 20 (drop 31 spawn-points)))
        camera (clone! :iso-camera)]
    (reset! INPUT player-input)
    (reset! CAMERA camera)
    (reset! CAMERA-AXIS (first (children camera)))
    (reset! AIM (clone! :cube))
    (hook+ player-input :update #'input.core/push-input!)
    (game.play/set-player! player)
    (state+ player-input :output-obj player)
    (position! player (.position (.transform (first spawn-points))))
    (hook+ camera :update #'update-camera)
    (hook+ canvas :update #'update-canvas)
    (log monsters)
    (reset! SWAPPED ((juxt identity identity) (dec (count (objects-tagged "entity")))))
    (set! (>v3 camera) (v3+ (>v3 player) (v3 -50 70 -50)))))

(defn start [_ _]
  (clear-cloned!)
  (destroy-immediate (the tiled))
  (make-level 1))


(deftween [:camera :size] [this]
  {:base (.GetComponent this UnityEngine.Camera)
   :base-tag UnityEngine.Camera
   :get (.orthographicSize this)
   :tag System.Single})

(defn menu [_ _]
  (clear-cloned!)
  (clone! :menu/sun)
  (let [world (game.world/make-world "worlds/world" 10 10)
        camera (clone! :menu/menu-camera)
        title (clone! :menu/title)
        newgame (clone! :menu/button)
        tpos (>v3 title)
        bpos (>v3 newgame)]
    (local-scale! title (v3 8))
    (position! newgame (v3+ tpos (v3 0 -3 0)))
    (timeline* 
      (AND 
        (tween {:camera {:size 2.03}} camera 6.0 :pow4)
        (tween {:local {:scale (v3 1)}} title 6.0 :pow2)
        (OR 
          (wait 5.5)
          (tween {:position bpos} newgame 0.5 :pow4))))
    (hook+ newgame :on-mouse-enter (fn [_ _] 
      (log "enter")
      (timeline* (tween {:local {:scale (v3 0.29)}} newgame 0.3 :pow2))))
    (hook+ newgame :on-mouse-exit (fn [_ _] 
      (log "exit")
      (timeline* (tween {:local {:scale (v3 0.26)}} newgame 0.3 :pow2))))
    (hook+ newgame :on-mouse-down (fn [_ _] 
      (timeline* (wait 0.1) #(do (start nil nil) nil))))))
(reset! MENU menu)


'(start nil nil)

'(menu nil nil)



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