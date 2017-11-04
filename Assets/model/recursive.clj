;; Copyright © 2017 Douglas P. Fields, Jr. Some Rights Reserved.
;; Web: https://symbolics.lisp.engineer/
;; E-mail: symbolics@lisp.engineer
;; Twitter: @LispEngineer

(ns model.recursive
    "Recursive functionality for calling functions attached to
     Unity GameObjects.

     Given a GameObject, it will look for the associated data
     (see hard.core/data), which, if it's a map, will look for
     the :fns key, which again, if it's a map, will look for
     the appropriate method key, and finally, which if that is
     a function, will call it with the arguments. Then, it will
     look at all the child GameObjects of that object and repeat the
     process all over again.

     Author: Douglas P. Fields, Jr.
    "
    (:require [arcadia.core :as a]
              arcadia.linear
              [clojure.string :as str]
              [hard.core :as hard])
    (:import
      [UnityEngine Debug Resources GameObject PrimitiveType
       Application Color Input Screen Gizmos Camera Component Vector3 Mathf Quaternion]
       ArcadiaState
       Hard.Helper))

(defn walk-gos
  "Recursively calls the specified function on the provided GameObject
   and then all child gameobjects, in a depth-first fashion (i.e., walks
   the first child, then the children of the first child, before moving on
   to the second child.
   --
   Calls (func go ~@args) on each one.
   --
   Returns nothing. This should be called for side effects only...
   --
   TODO: Consider only calling this on ACTIVE game objects?
   "
  [^GameObject go func & args]
  ;; First call the function on the current GameObject
  (apply func go args)
  ;; Now get all the child objects
  (let [t (.transform go)
        num-children (.childCount t)]
    #_(Debug/Log (str "Object " (.name go) " has " num-children " children"))
    (loop [child-num 0]
      (when (< child-num num-children)
        (let [child-t (.GetChild t child-num)
              child (.gameObject child-t)]
          (apply walk-gos child func args)
          (recur (inc child-num)))))))

;; Test of the above:
#_(walk-gos (GameObject/Find "Root") #(Debug/Log (.name %)))

(defn recursive-call
  "Given a GameObject, it will look for the associated data
   (see hard.core/data), which, if it's a map, will look for
   the :fns key, which again, if it's a map, will look for
   the appropriate method key, and finally, which if that is
   a function, will call it with the arguments. Then, it will
   look at all the child GameObjects of that object and repeat the
   process all over again.
   --
   The function is called as (func game-object method-keyword ~@args)
   --
   Example:
   (recursive-call root-game-object :update 37 ...)
   will call the {:fn {:update #'func}} on root-game-object's hard/data and each
   of its hierarchical children game objects, with the args like:
   (#'func current-game-object :update 37 ...)
   --
   TODO: Make a version which uses Arcadia GameObject state instead?
   "
  [go method & args]
  (walk-gos go
    (fn [igo & args]
      (let [data (hard/data igo)]
        (when (and (map? data)
                   (map? (:fns data))
                   (ifn? (get-in data [:fns method])))
          (apply (get-in data [:fns method]) igo method args))))))
  ;; (Debug/Log (str "Object: " (.name go) ", Data: " (hard/data go))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Testing scene stuff

;; To set up the testing scene, load it in Unity, start the
;; Arcadia REPL, then:
#_(do
  ;; The below crashes my REPL (using Ruby REPL with rlwrap) for some reason
  (require '[model.recursive :reload true])
  ;; So restart the REPL after that and continue...
  (in-ns 'model.recursive)
  (setup-hooks)
)
;; Then verify that the hook is shown on the Main Camera in the Unity Editor

(def root-object-name "Root")

(def main-camera-name "Main Camera")

(def was-data-setup
  "Was the data set up on our test scene objects yet?"
  (volatile! false))

(defn data-setup
  "Sets up the data on the game object with a single :update :fns."
  [ro]
  (hard/data! ro
    {:fns {:update (fn [go meth & args]
                       (Debug/Log (str ":fns " meth " on " (.name go) " with " args)))}}))

(defn data-setup-all
  "Sets up the data on all game objects starting at the root, recursively."
  []
  ;; #'data-setup would work as well as data-setup
  ;; but works better if we expect that var (function) to change.
  (walk-gos (GameObject/Find root-object-name) data-setup))

(defn test-scene-update
  "A per-frame update of the whole scene. Attach this to the MainCamera
   for example, and hook it to :update."
  [^GameObject go k]
  #_(Debug/Log (str "Update called on " go " with k of " k))
  ;; Set up our data objects the first time
  (when-not @was-data-setup
    (Debug/Log "Setting up data...")
    (data-setup-all)
    (vreset! was-data-setup true))
  ;; Do an update of all the data in the Root GameObject
  (let [go (GameObject/Find root-object-name)]
    (recursive-call go :update :plus :some :args)))

(defn setup-hooks
  "Set up the hooks for our test scene. This needs to be called
   once when the scene is created, by the REPL."
  []
  (let [ro (GameObject/Find main-camera-name)]
    ;; Not really sure what the point of k is.
    (a/hook+ ro :update :none #'test-scene-update)))
