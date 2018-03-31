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
  (import [UnityEngine GameObject Vector3 GL RenderTexture Graphics Rect ]))

(defn e->ui-v2 [e]
  (let [p (>v3 e)
        p (v2 (.x p) (.z p))
        ld (v2 (* @SIZE 40))
        r (/ 122 (* @SIZE 40))
        p (v2* p r)
        p (v2+ p (v2* (v2 30 -30) r))] p))

(defn color-texture [c]
  (let [t (UnityEngine.Texture2D. 1 1)]
    (.SetPixel t 0 0 c)
    (.Apply t) t))

(defn blank-render-texture [w h c]
  (let [rt (RenderTexture. w h 32)]
    (UnityEngine.Graphics/Blit (color-texture c) rt)
    rt))

(defn draw-texture [rt t x y]
  (let [w (.width rt)
        h (.height rt)]
   (set! RenderTexture/active rt)
   (GL/PushMatrix)
   (GL/LoadPixelMatrix 0 w h 0)
   (Graphics/DrawTexture (Rect. x y (.width t) (.height t)) t)

   (GL/PopMatrix)
   (set! RenderTexture/active nil)

   ;(.SetPixel rt 1 1 (color 0 0 0 0))
   ))

(.SetPixel (UnityEngine.Texture2D. 10 10) 1 1 (color 0 0 0 0))


(defn clear-texture [rt c]
  (let [white (blank-render-texture 512 512 (color 1 1 1 1))]
    (draw-texture rt white 0 0)))


(defn reveal-map [rt]
  (let [dim 20
        half-dim (* dim 0.5)
        view (blank-render-texture dim dim (color 1 1 1 1))
        p (e->ui-v2 @PLAYER)
        x (- (.x p) half-dim)
        y (- (- (.y p)) half-dim)]
    (draw-texture rt view x y)
    ;(clear-texture rt nil)
    ))




(m/defn update-camera [^GameObject o _]
  (let [^GameObject player @PLAYER]
    (set! (>v3 o) 
          (Vector3/Lerp
            (>v3 o)
            (v3+ (>v3 player) (v3 -20 28 -20))
            0.1)))
  nil)





;TODO magic
(defn draw-blips [minimap]
  (let [entities (objects-tagged "entity")
        blips (objects-tagged "blip")]
  (dotimes [i (count blips)]
    (if (< i (count entities))
      (let [e (aget entities i)
            b (aget blips i)
            p (e->ui-v2 e)]
        (if (= e @PLAYER)
            (set! (.color (cmpt b UnityEngine.UI.Image)) (color 0 1 0))
            (set! (.color (cmpt b UnityEngine.UI.Image)) (color 1 0 0)))
          (set! (.anchoredPosition (.transform b)) p))
      (set! (.anchoredPosition (.transform (aget blips i))) (v2 300 300))))))

(defn update-canvas [^GameObject o _]
  (let [health (cmpt (child-named o "health") UnityEngine.UI.Text)
        swapped (cmpt (child-named o "swapped") UnityEngine.UI.Text)
        minimap (child-named o "map")
        player (state @PLAYER)]
    (set! (.text health) (str (int (:hp player)) "/" (:max-hp player)))
    (set! (.text swapped) (str (first @SWAPPED) "/" (last @SWAPPED)))
    (draw-blips minimap)
    (reveal-map (state o :rt))))



(defn make-level 
  ([depth] (make-level depth 10 30))
  ([depth dim entity-cnt]
    (let [world (game.world/make-world "worlds/world" dim dim)
          _ (local-scale! world (v3 20))
          sun (clone! :sun)
          mapcam (-clone! :mapcam (v3 (* 40 (- (* dim 0.5) 0.5)) 100 (* -40 (- (* dim 0.5) 0.5))))
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
            (take entity-cnt (rest spawn-points)))
          balls 
          (run! 
            (fn [spawn] 
              (let [ball (clone! :prisoner-ball)]
                (position! ball (v3+ (>v3 spawn) (v3 0 2 0)))))
            (take 20 (drop 31 spawn-points)))
          blips 
          (let [minimap (child-named canvas "map")]
            (dotimes [i (inc entity-cnt)]
              (.SetParent (.transform (clone! :blip)) (.transform minimap))))
          camera (clone! :iso-camera)
          rt (blank-render-texture 128 128 (color 0 0 0 1))]
      (state+ canvas :rt rt)
      (.SetTexture (.material (cmpt (child-named canvas "reveal") UnityEngine.UI.Image)) "_MainTex" rt)
      (set! (.orthographicSize (cmpt mapcam UnityEngine.Camera)) (float (* dim 21.5)))
      (reset! SIZE dim)
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
      (set! (>v3 camera) (v3+ (>v3 player) (v3 -50 70 -50))))))

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

'(do
  (clear-cloned!)
  (destroy-immediate (the tiled))
  (make-level 1 5 0))

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