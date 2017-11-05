(ns input.core
  (:require [arcadia.core :as arc]
            [arcadia.linear :as lin])
  (:import [UnityEngine Input Vector3 Camera Physics Plane Ray]))

(def input-axes {:horizontal "Horizontal"
                 :vertical "Vertical"})

(def input-buttons {:fire 0
                    :special 1})

(defn get-buttons
  "Returns a set of keys from the map bttns.
   The returned keys have values which return a truthy value when passed to fun."
  [fun bttns]
  (set (map first
            (filter
             (fn [[key val]] val)
             (map (fn [[key val]] [key (fun val)])
                  (seq bttns))))))

(defn update-input-state!
  [old-state ^Vector3 player-pos layer]
  {:movement (.normalized (lin/v2 (Input/GetAxisRaw (input-axes :horizontal))
                              (Input/GetAxisRaw (input-axes :vertical))))
   ;; TODO: update aim to use a raycast from the camera through the mouse rather
   ;; than a direct mouse position, so that aiming doesn't care about camera angle
   :aim (let [plane (Plane. Vector3/up player-pos)
              ray (.ScreenPointToRay Camera/main Input/mousePosition)
              dist (float 0.0)
              max-dist (float 100.0)]
          (if (.Raycast plane ray (by-ref dist))
            (.GetPoint ray dist)
            (.GetPoint ray max-dist)))

   #_(.normalized (lin/v2- (let [pos (.ScreenToWorldPoint Camera/main Input/mousePosition)]
                                (lin/v2 (.x pos) (.y pos)))
                              (lin/v2 (.x player-pos) (.y player-pos))))
   :buttons-down (get-buttons #(Input/GetMouseButtonDown %) input-buttons)
   :buttons-pressed (get-buttons #(Input/GetMouseButton %) input-buttons)
   :buttons-up (get-buttons #(Input/GetMouseButtonUp %) input-buttons)})

(defn push-input!
  "A function that grabs a referenced object from this object's state atom and updates its state to include input from the player this frame.

  Generally this function should be associated with the update message on an empty, and the referenced object should be the player."
  [this key]
  (let [other (arc/state this :output-obj)]
    (when other
      (arc/update-state other :input update-input-state!
                        (.position (.transform other))
                        (arc/state this :raycast-layer)))))
