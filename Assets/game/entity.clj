(ns game.entity
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.seed))

(def PARTS (atom {}))

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


(defn attach [mount m budget]
  (when (pos? budget)
    (when-let [obj (clone! (:prefab m))]
      (parent! obj mount)
      (position! obj (>v3 mount))
      (rotation! obj (.rotation (.transform mount)))
      (dorun
        (map 
          (fn [[k v]]
            (when-let [mount (child-named obj (name k))]
              (when-let [m (srand-nth (vec (parts-typed (probability v))))]
                (attach mount m (dec budget)))))
          (:mount-points m))))))

(defn make-entity [start-type budget]
  (let [root (clone! :entity)
        parts (atom [])]
    (when-let [start (srand-nth (vec (parts-typed start-type)))]
      (attach root start budget))
    root))


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
  :prefab :parts/business-head})

(part {
  :type :arm
  :id :business
  :prefab :parts/business-arm})

'(do (clear-cloned!)
  (make-entity :body 2)
  )