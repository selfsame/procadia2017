(ns game.entity
  (use
    arcadia.core
    arcadia.linear
    hard.core
    hard.seed)
  (require
    [arcadia.internal.benchmarking :as bench]))

(defonce PARTS (volatile! {}))

(deftype ^:once PartHook [^clojure.lang.IFn hook ^UnityEngine.GameObject part])

(defn part [{:keys [prefab type mount-points hooks state id] :as part-map}]
  (let [id (or id (hash (dissoc part-map :hooks)))
        part-map (assoc part-map :id id)
        types (if (seqable? type) type (list type))]
    (run! #(vswap! PARTS update ,,, % assoc ,,, id part-map) types)))

(defn parts-typed [k]
  (-> @PARTS k vals))

(defn probability [col]
  (let [      total (reduce + (vals col))
        ^long roll  (srand total)]
    (loop [[[part chance] & xs] (seq col) ; convert map to [k v] seq
           ^long acc 0]
      (let [^long new-acc (unchecked-add acc chance)]
        (if (< roll new-acc)
          part
          (recur xs new-acc))))))

(defn attach
  "parts is an atom containing a vector of maps, created in make-entity"
  [mount m budget parts]
  (when (pos? budget)
    (when-let [obj (clone! (:prefab m))]
      (swap! parts conj (assoc m :object obj))
      (state+ obj :procjam/part (or (:state m) {}))
      (parent! obj mount)
      (position! obj (>v3 mount))
      (rotation! obj (.rotation (.transform mount)))
      (run!
        (fn [[k v]]
          (when-let [mount (child-named obj (name k))]
            (when-let [m (srand-nth (vec (parts-typed (probability v))))]
              (attach mount m (dec budget) parts))))
        (:mount-points m)))))

(defn update-vals [m f]
  (persistent! (reduce-kv (fn [m k v] (assoc! m k (f v))) (transient {}) m)))

(defn extract-hooks [m]
  (let [obj (:object m)]
    (update-vals (:hooks m) #(list (PartHook. % obj)))))

(defn entity-start [^UnityEngine.GameObject o _]
  (state+ o :entity? true)
  (let [input    (state o :input)
        hooks    (state o ::hooks)]
    (run!
      (fn [^PartHook ph] ((.hook ph) o (.part ph)))
      (:start hooks))))

(defn entity-update [^UnityEngine.GameObject o _]
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
  ([budget] (make-entity :feet budget))
  ([start-type budget] (make-entity start-type budget (srand-int 1000000)))
  ([start-type budget seed]
    (seed! seed)
    (let [root (clone! :entity)
          parts (atom [])]
      (when-let [start (srand-nth (vec (parts-typed start-type)))]
        (attach root start budget parts))
      (state+ root ::seed seed)
      (state+ root ::start-type start-type)
      (state+ root ::parts @parts)
      (state+ root ::hooks
        (update-vals
          ;; TODO: Speed up this reduction by avoiding a nested merge/concat if possible,
          ;; or at least use a faster data structure? This needs more detailed analysis.
          (reduce
            (fn [xs m] (merge-with concat xs (extract-hooks m)))
            {} @parts) #(into-array PartHook %)))
      (hook+ root :start ::start #'entity-start)
      (hook+ root :update ::update #'entity-update)
      (let [hp (min 4 (reduce #(+ %1 (get %2 :hp 1)) 0 @parts))
            power (reduce #(+ %1 (get %2 :power 0)) 0 @parts)]
        (state+ root :hp hp)
        (state+ root :max-hp hp)
        (state+ root :power power))
      (skin-color! root (color (?f 1)(?f 1)(?f 1)))
      (rotate! root (v3 0 (?f 360) 0))
      root)))


'(do
  (clear-cloned!)
  (make-entity :feet 20))
