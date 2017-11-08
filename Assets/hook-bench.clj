(ns hook-bench
  (use arcadia.core)
  (import
    [UnityEngine Input]))

(defn f [o _])

(defn setup []
  (if-let [c (object-named "Cube")] (destroy-immediate c))
  (let [o (create-primitive :cube)]
    (hook+ o :update
      (fn [o _]
        (cond 
          (Input/GetKeyDown "1")
          (hook+ 
            (arcadia.core/instantiate (UnityEngine.Resources/Load "empty"))
            :start #'f)
          (Input/GetKeyDown "2")
          (arcadia.core/instantiate (UnityEngine.Resources/Load "empty-hook")))))))


(setup)