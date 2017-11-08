(ns game.entity
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.seed)
  (:require [arcadia.internal.benchmarking :as bench]))

(defonce
  ^{:doc "We use a volatile because they're faster than atoms and we don't need the
   thread-safety of an atom (or so I think). It's semantically the same as an
   atom for @, but uses vswap! and vreset! in place of swap! and reset!"}
  PARTS
  (volatile! {}))

(deftype ^:once PartHook [^clojure.lang.IFn hook ^UnityEngine.GameObject part])

(defn part [{:keys [prefab type mount-points hooks state id] :as part-map}]
  (let [id (or id (hash (dissoc part-map :hooks)))
        part-map (assoc part-map :id id)]
    (vswap! PARTS update ,,, type assoc ,,, id part-map)))

(defn parts-typed [k]
  (-> @PARTS k vals))

(defn probability-1
  "Original implementation"
  [col]
  (let [total (reduce + (vals col))
        roll  (srand total)]
    (loop [xs  col
           acc 0]
      (if (< roll (+ acc (last (first xs))))
          (ffirst xs)
          (recur (rest xs) (+ acc (last (first xs))))))))

(defn probability-2
  "DPF reimplementation #1 - apply +. Slower (!)."
  [col]
  (let [total (apply + (vals col))
        roll (srand total)]
    (loop [xs col
           acc 0]
      (if (< roll (+ acc (last (first xs))))
          (ffirst xs)
          (recur (rest xs) (+ acc (last (first xs))))))))

(defn probability-3
  "DPF reimplementation #2 - Reducing function for inner loop."
  [col]
  (let [total (reduce + (vals col))
        roll (srand total)]
    (reduce
      (fn [acc [part chance]]
        (let [new-acc (+ acc chance)]
          (if (< roll new-acc)
            (reduced part)
            new-acc)))
      0 col)))

(defn probability-4
  "DPF reimplementation #3 - destructured. This is the fastest by about 10% over -1.
   Adding type hints and using unchecked-add seems to add minimal performance."
  [col]
  (let [      total (reduce + (vals col))
        ^long roll  (srand total)]
    (loop [[[part chance] & xs] (seq col) ; convert map to [k v] seq
           ^long acc 0]
      (let [^long new-acc (unchecked-add acc chance)]
        (if (< roll new-acc)
          part
          (recur xs new-acc))))))

(def probability probability-4)

(defn child-with-name-1
  "shallow child-named"
  [obj n]
  ;; TODO: DPF: (first (filter ...)) should probably be use "some" instead,
  ;; but reduce with reduced may be the fastest.
  (->> obj children (filter #(= (.name %) n)) first))

(defn child-with-name-2
  "Shallow child-named. DPF Reimplementation #1 using reduce/reduced. About 30% faster than -1.
   reduce is rediculously fast."
  [obj name]
  ;; I find threading macros to be a challenge to readability
  (let [chlds (children obj)]
    (reduce (fn [_ c] (when (= (.name c) name) (reduced c))) nil chlds)))

(defn child-with-name-3
  "Shallow child-named. DPF Reimplementation #2 using some. About 0-10% slower than -2, which
   was as expected, but sometimes 0-10% faster than -2 depending on inputs, which wasn't."
  [obj name]
  (let [chlds (children obj)]
    (some #(when (= name (.name %)) %) chlds)))

(def child-with-name child-with-name-3)

(defn attach
  "parts is an atom containing a vector of maps, created in make-entity"
  [mount m budget parts]
  (when (pos? budget)
    (when-let [obj (clone! (:prefab m))]
      (swap! parts conj (assoc m :object obj))
      (parent! obj mount)
      (position! obj (>v3 mount))
      (rotation! obj (.rotation (.transform mount)))
      (run!
        (fn [[k v]]
          (when-let [mount (child-named obj (name k))]
            (when-let [m (srand-nth (vec (parts-typed (probability v))))]
              (attach mount m (dec budget) parts))))
        (:mount-points m)))))

(defn update-vals-1
  "Original implementation"
  [m f]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn update-vals-2
  "DPF Reimplementation #1 of update-vals-1 using reduce-kv and transients
   (vastly faster, like 7x)"
  [m f]
  (persistent! (reduce-kv (fn [m k v] (assoc! m k (f v))) (transient {}) m)))

(def update-vals update-vals-2)

(defn extract-hooks [m]
  (let [obj (:object m)]
    (update-vals (:hooks m) #(list (PartHook. % obj)))))

(defn entity-update [^UnityEngine.GameObject o _]
  ;; TODO: Regression test this. DPF refactored without a test harness
  (let [input    (state o :input)
        hooks    (state o ::hooks)
        movement (:movement input)
        mouse-x  (:mouse-intersection input)]
    ;; run! is preferred over (dorun (map ...))
    (run!
      (fn [^PartHook ph] ((.hook ph) o (.part ph)))
      (:update hooks))
    (run!
      (fn [^PartHook ph] ((.hook ph) o (.part ph) movement))
      (:move hooks))
    (run!
      (fn [^PartHook ph] ((.hook ph) o (.part ph) mouse-x))
      (:aim hooks))))

(defn skin-color! [o c]
  ;; TODO: Regression test this. DPF refactored without a test harness
  (let [mrs  (.GetComponentsInChildren o UnityEngine.MeshRenderer)
        smrs (.GetComponentsInChildren o UnityEngine.SkinnedMeshRenderer)]
    (run!
      #(set! (.color %) c)
      ;; TODO: I feel the below can be heavily optimized. Several concats,
      ;; a map and a filter...
      (filter
        #(= (.name %) "SKIN (Instance)")
        (mapcat #(.materials %) (concat mrs smrs))))))

(defn make-entity
  ;; TODO: Regression test this. DPF refactored without a test harness
  ([budget] (make-entity :feet budget))
  ([start-type budget]
    (let [root (clone! :entity)
          parts (atom [])]
      (when-let [start (srand-nth (vec (parts-typed start-type)))]
        (attach root start budget parts))
      (state+ root ::parts @parts)
      (state+ root ::hooks
        (update-vals
          ;; TODO: Speed up this reduction by avoiding a nested merge/concat if possible,
          ;; or at least use a faster data structure? This needs more detailed analysis.
          (reduce
            (fn [xs m] (merge-with concat xs (extract-hooks m)))
            {} @parts) #(into-array PartHook %)))
      ;; JP says that this hook+ function is running extremely slowly.
      ;; In production we can probably safely use entity-update as it won't
      ;; be redefined, instead of giving the var (#'entity-update = (var entity-update)) explicitly
      (hook+ root :update ::update #'entity-update)
      (skin-color! root (color (?f 1)(?f 1)(?f 1)))
      (rotate! root (v3 0 (?f 360) 0))
      root)))




'(do
  (clear-cloned!)
  (def ph (state (make-entity :feet 10))))








;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Benchmarking code - DPF

(defmacro safe-n-timing
  "This is required because using too high an N will cause a spurious
   System.NullReferenceException. Hypothesis: Mono cannot keep up with
   the garbage created by ClojureCLR."
  [n func]
  `(try
    (bench/n-timing ~n ~func)
    (catch System.Exception e# -1.0)))

(defn bench1
  "Benchmark probability-# functions"
  []
  (let [n 500000
        input {:arm 10 :leg 5 :belt 2 :head 1}
        b1 (safe-n-timing n (probability-1 input))
        b2 (safe-n-timing n (probability-2 input))
        b3 (safe-n-timing n (probability-3 input))
        b4 (safe-n-timing n (probability-4 input))]
    [b1 b2 b3 b4]))

(defn bench2
  "Benchmark update-vals functions"
  []
  (let [n 250000
        input {:a 1 :b 2 :c 3 :d 4}
        func inc
        b1 (safe-n-timing n (update-vals-1 input func))
        b2 (safe-n-timing n (update-vals-2 input func))]
    [b1 b2]))

(defn bench3
  "Benchmark child-with-name - using the RecursiveTest scene."
  []
  (let [n 250000
        root (object-named "Root")
        child-to-find1 "Child2" ; Child1 through Child5 available
        child-to-find2 "Child5" ; Child1 through Child5 available
        b1 (safe-n-timing n
             (do (child-with-name-1 root child-to-find1)
                 (child-with-name-1 root child-to-find2)))
        b2 (safe-n-timing n
             (do (child-with-name-2 root child-to-find1)
                 (child-with-name-2 root child-to-find2)))
        b3 (safe-n-timing n
             (do (child-with-name-3 root child-to-find1)
                 (child-with-name-3 root child-to-find2)))
       ]
    [b1 b2 b3]))
