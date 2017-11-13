(ns game.play
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    tween.core
    game.data)
  (require
    input.core
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

(defn set-player! [o]
  (reset! PLAYER o)
  (state+ @INPUT :output-obj @PLAYER)
  (set-mask! o "player")
  (state+ o :mask (int (+ (mask "level") (mask "monster"))))
  ;(state+ o :hp 30)
  ;(state+ o :max-hp 30)
  (hook+ o :update :target
    (fn [o _] 
      (when-let [input (state o :input)]
        (position! @AIM (.target input))))))

(defn kill [o]
  (if (= o @PLAYER)
    (@MENU nil nil)
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
      (set-player! new-player)
      (swap! SWAPPED assoc 0 (dec (count (objects-tagged "entity")))))))

(defn damage [o n]
  (let [root (.gameObject (.root (.transform o)))]
    (when (:entity? (state root))
      (update-state root :hp #(- % n))
      (if (< (state root :hp) 0)
          (kill root)))))

(defn arm-update [o this aim]
  (let [input (state o :input)]
    (when input
      (look-at! this (v3+ (.target input) (v3 0 1 0)) (v3 0 1 0)))))