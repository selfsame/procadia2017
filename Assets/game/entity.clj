(ns game.entity
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.seed))

(def PARTS (atom {}))

(deftype ^:once PartHook [^clojure.lang.IFn hook ^UnityEngine.GameObject part])

(defn part [{:keys [prefab type mount-points hooks state id] :as part-map}]
  (let [id (or id (hash (dissoc part-map :hooks)))
        part-map (assoc part-map :id id)]
    (swap! PARTS update-in [type] assoc id part-map)))

(defn parts-typed [k]
  (-> @PARTS k vals))

(defn probability [col]
  (let [total (reduce + (vals col))
        roll (srand total)]
    (loop [xs col
           acc 0]
      (if (< roll (+ acc (last (first xs))))
          (ffirst xs)
          (recur (rest xs) (+ acc (last (first xs))))))))

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
            (when-let [mount (child-with-name obj (name k))]
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
  (when-let [hooks (:update (state o ::hooks))]
    (dorun 
      (map 
        (fn [ph]
          ((.hook ph) o (.part ph)))
        hooks))))

(defn make-entity 
  ([budget] (make-entity :body budget))
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


(part {
  :type :body
  :id :business
  :prefab :parts/business-body
  :mount-points {
    :neck {:head 1}
    :left-arm {:arm 1 :head 1}
    :right-arm {:arm 1 :head 1}
  } 
  :hooks {:aim (fn [root this aim])}})

(part {
  :type :head
  :id :business
  :prefab :parts/business-head
  :hooks {
    :update 
    (fn [root this] (log "part update" root this))
    }})

(part {
  :type :arm
  :id :business
  :prefab :parts/business-arm
  :hooks {:aim (fn [root this aim])}})

(part {
  :type :head
  :id :eyeball
  :prefab :parts/eyeball})

'(do 
  (clear-cloned!)
  (def ph (state (make-entity :body 2))))
