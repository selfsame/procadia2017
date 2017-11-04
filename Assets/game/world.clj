(ns game.world
  (use
    arcadia.core
    arcadia.linear
    hard.core))




(defn arm [entity]
  {:object (GameObject. "arm")
   :mounts [{:point (v3 1 0 0) :type :weapon}]
   :update (fn [this input])
   :hp 10})


