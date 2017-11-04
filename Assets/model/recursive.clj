(ns model.recursive
    "Recursive functionality for calling functions attached to
     Unity GameObjects.

     Given a GameObject, it will look for the associated data
     (see hard.core/data), which, if it's a map, will look for
     the :fns key, which again, if it's a map, will look for
     the appropriate method key, and finally, which if that is
     a function, will call it with the arguments. Then, it will
     look at all the child GameObjects of that object and repeat the
     process all over again."
    (:require [arcadia.core :as a]
              arcadia.linear
              [clojure.string :as str]
              [hard.core :as hard])
    (:import
      [UnityEngine Debug Resources GameObject PrimitiveType
       Application Color Input Screen Gizmos Camera Component Vector3 Mathf Quaternion]
      ArcadiaState
      Hard.Helper))

(defn recursive-call
  "Given a GameObject, it will look for the associated data
   (see hard.core/data), which, if it's a map, will look for
   the :fns key, which again, if it's a map, will look for
   the appropriate method key, and finally, which if that is
   a function, will call it with the arguments. Then, it will
   look at all the child GameObjects of that object and repeat the
   process all over again."
  [go method & args]
  (Debug/Log "This is a debugging message."))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Testing scene stuff

(def root-object-name
  "Root")

(defn test-scene-update
  [^GameObject go k]
  (Debug/Log (str "Update called on " go)))

(defn setup-hooks
  []
  (let [ro (GameObject/Find root-object-name)]
    (a/hook+ ro :update nil #'test-scene-update)))
