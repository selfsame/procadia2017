(ns input.core
  (:require [arcadia.core :as arc]
            [arcadia.linear :as lin]
            [magic.api :as m]
            [game.data])
  (:import [UnityEngine Input Vector3 Camera Physics Plane Ray]))

(def input-axes {:horizontal "Horizontal"
                 :vertical "Vertical"})

(def input-buttons {:fire 0
                    :special 1})

(deftype Control [
  ^:volatile-mutable ^Vector3 movement
  ^:volatile-mutable ^Vector3 aim
  ^:volatile-mutable ^Vector3 target
  ^:volatile-mutable ^clojure.lang.PersistentHashSet down
  ^:volatile-mutable ^clojure.lang.PersistentHashSet pressed
  ^:volatile-mutable ^clojure.lang.PersistentHashSet up])

(defn new-control []
  (let [v (lin/v3 0)
        s #{}]
    (Control. v v (lin/v3 1 0 0) s s s)))

(defn get-buttons [fun]
  (let [fire (fun 0)
        special (fun 1)]
    (cond (and fire special) #{:fire :special}
          fire               #{:fire}
          special            #{:special}
          :else              #{})))

(m/defn get-mouse-on-plane!
  [^Vector3 player-pos]
  (let [plane (Plane. Vector3/up player-pos)
        ray (.ScreenPointToRay Camera/main Input/mousePosition)
        dist (float 0)
        max-dist (float 100)]
    (or (if (.Raycast plane ray (by-ref dist))
            (.GetPoint ray dist)
            (.GetPoint ray max-dist))
        (lin/v3 0))))

(defn push-input!
  "A function that grabs a referenced object from this object's state atom and updates 
  its state to include input from the player this frame.
  Generally this function should be associated with the update message on an empty, 
  and the referenced object should be the player."
  [this key]
  (when-let [other (arc/state this :output-obj)]
    (let [^Control control (or (arc/state other :input) 
                               (arc/state (arc/state+ other :input (new-control)) :input))
          ^Vector3 player-pos (.position (.transform other))
          ^Vector3 target (get-mouse-on-plane! player-pos)
          ^Vector3 movement (.TransformDirection 
                              (.transform @game.data/CAMERA-AXIS)
                              (.normalized (lin/v3 
                                (Input/GetAxisRaw "Horizontal") 0
                                (Input/GetAxisRaw "Vertical"))))
          ^Vector3 aim (.normalized (lin/v3- target player-pos))]
      (set! (.movement control) movement)
      (set! (.aim control) aim)
      (set! (.target control) target)
      (set! (.down control) (get-buttons #(Input/GetMouseButtonDown %)))
      (set! (.pressed control) (get-buttons #(Input/GetMouseButton %)))
      (set! (.up control) (get-buttons #(Input/GetMouseButtonUp %))))))
