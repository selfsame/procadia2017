(ns game.entity
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.seed)
  (:require [arcadia.internal.benchmarking :as bench]))

(def PARTS (atom {}))

(deftype ^:once PartHook [^clojure.lang.IFn hook ^UnityEngine.GameObject part])

(defn part [{:keys [prefab type mount-points hooks state id] :as part-map}]
  (let [id (or id (hash (dissoc part-map :hooks)))
        part-map (assoc part-map :id id)]
    (swap! PARTS update-in [type] assoc id part-map)))

(defn parts-typed [k]
  (-> @PARTS k vals))

(defn probability-1
  "Original implementation"
  [col]
  (let [total (reduce + (vals col))
        roll (srand total)]
    (loop [xs col
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
  "DPF reimplementation #3 - destructured"
  [col]
  (let [total (reduce + (vals col))
        roll (srand total)]
    (loop [[[part chance] & xs] (seq col)
           acc 0]
      (let [new-acc (+ acc chance)]
        (if (< roll new-acc)
          part
          (recur xs new-acc))))))


(def probability probability-1)

(defn child-with-name
  "shallow child-named"
  [obj n]
  (->> obj children (filter #(= (.name %) n)) first))

(defn attach [mount m budget parts]
  (when (pos? budget)
    (when-let [obj (clone! (:prefab m))]
      (swap! parts conj (assoc m :object obj))
      (parent! obj mount)
      (position! obj (>v3 mount))
      (rotation! obj (.rotation (.transform mount)))
      (local-scale! obj (local-scale mount))
      (dorun
        (map
          (fn [[k v]]
            (when-let [mount (child-named obj (name k))]
              (when-let [m (srand-nth (vec (parts-typed (probability v))))]
                (attach mount m (dec budget) parts))))
          (:mount-points m))))))

(defn extract-hooks [m]
  (let [obj (:object m)]
    (into {}
      (map
        (fn [[k v]] [k [(PartHook. v obj)]])
        (:hooks m)))))

(defn update-vals [m f]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn entity-update [^UnityEngine.GameObject o _]
  (let [input (state o :input)]
    (dorun
      (map
        (fn [ph]
          ((.hook ph) o (.part ph)))
        (:update (state o ::hooks))))
    (dorun
      (map
        (fn [ph]
          ((.hook ph) o (.part ph) (:movement input)))
        (:move (state o ::hooks))))
    (dorun
      (map
        (fn [ph]
          ((.hook ph) o (.part ph) (:mouse-intersection input)))
        (:aim (state o ::hooks))))))

(defn make-entity
  ([budget] (make-entity :feet budget))
  ([start-type budget]
    (let [root (clone! :entity)
          parts (atom [])]
      (when-let [start (srand-nth (vec (parts-typed start-type)))]
        (attach root start budget parts))
      (state+ root ::parts @parts)
      (state+ root ::hooks
        (update-vals
          (reduce
            (fn [xs m] (merge-with concat xs (extract-hooks m)))
            {} @parts) #(into-array PartHook %)))
      (hook+ root :update ::update #'entity-update)
      root)))




'(do
  (clear-cloned!)
  (def ph (state (make-entity :feet 3))))








;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Benchmarking code - DPF

(defn bench1 []
  (let [n 4000000
        input {:arm 10 :leg 5 :belt 2 :head 1}
        b1 (bench/n-timing n (probability-1 input))
        ; b2 (bench/n-timing n (probability-2 input))
        b3 (bench/n-timing n (probability-3 input))
        b4 (bench/n-timing n (probability-4 input))]
    [b1 #_b2 b3 b4]))
    ;
    ; {
    ;  :probability-1
    ;  ;; 2 is very slow so omit for now
    ; ;  :probability-2
    ; ;  (bench/n-timing n (probability-2 input))
    ;  :probability-3
    ;  (bench/n-timing n (probability-3 input))
    ;  :probability-4
    ;  (bench/n-timing n (probability-4 input))
    ; }))
