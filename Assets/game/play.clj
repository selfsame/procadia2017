(ns game.play
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    tween.core
    game.data)
  (require
    game.entity
    game.fx)
  (import [UnityEngine]))

(defn kino-settle [^UnityEngine.GameObject o _]
  (let [root (.. o transform root gameObject)
        rb (cmpt root UnityEngine.Rigidbody)]
    (set! (.isKinematic rb) true)
    (timeline*
      (wait 0.2)
      #(do (set! (.isKinematic rb) false) nil))))

(defn kill [o]
  (let [seed (:game.entity/seed (state o))
        start-type (:game.entity/start-type (state o))
        ppos (>v3 @PLAYER)
        prot (rotation @PLAYER)
        new-player (game.entity/make-entity start-type 20 seed)]
    (run! 
      (fn [o] 
        (game.fx/smoke (>v3 o))
        (timeline*
          (tween {:local {:scale (v3 0)}} o 0.5)
          (destroy o))) 
      [o @PLAYER])
    (position! new-player ppos)
    (rotation! new-player prot)
    (kino-settle new-player nil)
    (reset! PLAYER new-player)
    (state+ @INPUT :output-obj @PLAYER)))

(defn damage [o n]
  (let [root (.gameObject (.root (.transform o)))]
    (when (:entity? (state root))
      (update-state root :hp #(- % n))
      (if (< (state root :hp) 0)
          (kill root)))))

(defn arm-update [o this aim]
  (let [{:keys [movement aim mouse-intersection]} (state o :input)]
    (when aim 
      (look-at! this (v3+ mouse-intersection (v3 0 (.y (>v3 this)) 0)) (v3 0 1 0)))))

