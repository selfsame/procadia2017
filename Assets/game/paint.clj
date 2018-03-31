(ns game.core
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.physics
    tween.core)
  (require clojure.core.server)
  (import [UnityEngine GL RenderTexture Graphics Rect Debug]))

(defn start [o _]
  (clear-cloned!)
  (clone! :sun)
  (clone! :room)
  (clone! :player)
  (clone! :compressed-charcoal)
  (clone! :paper))


(defn color-texture [c]
  (let [t (UnityEngine.Texture2D. 1 1)]
    (.SetPixel t 0 0 c)
    (.Apply t) t))

(defn clear-texture []
  (let [whitemap (UnityEngine.Texture2D. 1 1)]
    (.SetPixel whitemap 0 0 UnityEngine.Color/white)
    (.Apply whitemap)
    whitemap))

(defn blank-render-texture [w h]
  (let [rt (RenderTexture. w h 32)]
    (UnityEngine.Graphics/Blit (clear-texture) rt)
    rt))

(defn draw-texture [rt t x y]
  (let [w (.width rt)
        h (.height rt)]
   (set! RenderTexture/active rt)
   (GL/PushMatrix)
   (GL/LoadPixelMatrix 0 w h 0)
   (Graphics/DrawTexture (Rect. (- x 15) (- y 9) 15 9) t)
   (GL/PopMatrix)
   (set! RenderTexture/active nil)))

(defn texture-coord [o v]
  (let [dims (v3 0.655875229 0 0.98)
        scale (.localScale (.transform o))
        dims2 (v3 (/ (.x dims) (.x scale)) 0 (/ (.z dims) (.z scale)))
        half (v3* dims2 0.5)
        point (v3+ half v)]
    point
    (v3 (/  (.x point) (.x dims2)) 0 (/  (.z point) (.z dims2)))))

(defn draw-scanline [rt t x y len]
  (let [x (* (- 1 x) 512) 
        y (* y 512)
        w (.width rt)
        h (.height rt)]
   (set! RenderTexture/active rt)
   (GL/PushMatrix)
   (GL/LoadPixelMatrix 0 w h 0)
   (Graphics/DrawTexture (Rect. x y len 1) t)
   (GL/PopMatrix)
   (set! RenderTexture/active nil)))

(defn ray-scan [o v]
  (let [v (inverse-transform-point o v) 
        black (state o :brush)
        rt (state o :rt)]
  (dorun
    (map 
      (fn [i]
        (let [a (transform-point o (v3+ v (v3 -1.5 0.1 (- (* i 0.0125) 1))))
              b (transform-point o (v3+ v (v3 1.5 0.1 (- (* i 0.0125) 1))))
              ablative (mask "ablative")]
          (if-let [ah (hit a (local-direction o (v3 1 0 0)) 20.0 ablative)]
            (when-let [bh (hit b (local-direction o (v3 -1 0 0)) 20.0 ablative)]
              (let [aa (texture-coord o (inverse-transform-point o (.point ah)))
                    bb (texture-coord o (inverse-transform-point o (.point bh)))
                    len (* 512 (- (.x aa) (.x bb)))]
                (draw-scanline rt black (.x aa) (.z aa) len))
               ))))
      (range 160)))))


(defn paper-collide [o c _]
  (ray-scan o (.point (first (.contacts c)))))

(defn setup-paper [o _]
  (let [rt (blank-render-texture 512 512)
        brush (color-texture (color 0 0 0))]
    (.SetTexture (.material (cmpt o UnityEngine.Renderer)) "_MainTex" rt)
    (state+ o :rt rt)
    (state+ o :brush brush)
    (hook+ o :on-collision-stay #'paper-collide)))

'(start nil nil)
