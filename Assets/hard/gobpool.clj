(ns hard.gobpool 
  (:import 
    [UnityEngine HideFlags GameObject] 
    [Pooled]))

(Pooled/addTag "pooled")
(Pooled/addTag "clone")

(defn ^UnityEngine.GameObject -tag-clone [^UnityEngine.GameObject o ^System.String n] 
  (set! (.name o) n)
  (set! (.tag o) "clone") o)

(defmacro ^UnityEngine.GameObject -clone [^clojure.lang.Keyword k]
  (let []
  `(hard.gobpool/-tag-clone (UnityEngine.Object/Instantiate (~'UnityEngine.Resources/Load  ~(name k))) ~(name k))))

(defn destroy-tagged [tag]
  (dorun (map #(UnityEngine.Object/DestroyImmediate %) (GameObject/FindGameObjectsWithTag tag))))

(defprotocol IGobPool
  (-reuse   [a])
  (-recycle [a b])
  (-stats   [a]))

(deftype GobPool [^|System.Object[]|                pool 
               ^System.Int64 ^:volatile-mutable  idx]
  IGobPool
  (-stats [a]
    (list (inc idx) '/ (.Length pool)))
  (-reuse [a]
    (try 
      (set! idx (dec idx))
      (aget pool (inc idx))
      (catch Exception e (set! idx (inc idx)) nil)))
  (-recycle [a b]
    (try 
      (set! idx (inc idx)) 
      (aset pool idx b) nil
      (catch Exception e  (set! idx (dec idx)) nil))))

(defn pool-prep [o]
  `((set! (.tag ~o) "pooled")
    (.SetParent (.transform ~o) nil false)  
    (set! (.hideFlags ~o) UnityEngine.HideFlags/HideAndDontSave)
    (set! (.position (.transform ~o)) (UnityEngine.Vector3. 0 -100000 0))))

(defn reuse-prep [o]
  `((set! (.tag ~o) "clone") 
    (set! (.hideFlags ~o) UnityEngine.HideFlags/None)
    (set! (.position (.transform ~o)) (UnityEngine.Vector3. 0 0 0))))

(defmacro gobpool [length type-sym model]
  (let [gob (with-meta (gensym 'gob) {:tag 'UnityEngine.GameObject})
        sym (with-meta (symbol (str "*" type-sym)) {:tag 'UnityEngine.GameObject})
        pool (with-meta (symbol (str "<>" type-sym)) {:tag 'UnityEngine.GameObject})
        return (with-meta (symbol (str "!" type-sym)) {:tag System.Boolean})
        o# (gensym)]
    `(~'let [~gob  ~model]
      ~@(pool-prep gob)
      (declare ~pool)
      (when (bound? (var ~pool)) (dorun (map ~'arcadia.core/destroy-immediate (~'.pool ~pool))))
      (~'def ~pool (new ~GobPool (~'make-array ~'System.Object ~length) -1))
      (~'defn ~return
        [~(with-meta (symbol "a") {:tag 'UnityEngine.GameObject})] 
        (~'hard.gobpool/-recycle ~pool ~'a)
        ~@(pool-prep 'a))
      (~'defn ~sym []
        (~'if-let [~(with-meta o# {:tag 'UnityEngine.GameObject}) (~'hard.gobpool/-reuse ~pool)]
          (if (~'arcadia.core/null-obj? ~o#) (~sym) 
            (do ~@(reuse-prep o#) ~o#))
          (let [~o# (~'hard.gobpool/-tag-clone (~'arcadia.core/instantiate ~gob) ~(str type-sym))]
            (do ~@(reuse-prep o#) ~o#))))
      ~(mapv keyword [pool sym return]))))
