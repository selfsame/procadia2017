# procadia2017


## setup

* use the `2017.1.0` version of Unity if possible
* `git clone --recursive` or clone Arcadia into `Assets`

# Participants

* Joseph Parker - GitHub selfsame
* Douglas P. Fields, Jr. - GitHub LispEngineer
* Joshua Suskalo - GitHub IGJoshua


# parts

Our entites are collections of parts, which are defined like:

```clj
(use 'game.entity)

(part {
  :type :body
  :id :business
  :prefab :parts/business-body
  :mount-points {
    :neck {:head 1}
    :left-arm {:arm 1 }
    :right-arm {:arm 1 }} 
  :hooks {:update (fn [root entity])}
  :state {:foo 1}})
 ```
* `:type` user taxonomy, `:mount-points` declare which types they allow
* `:id` optional, helps with redefining parts without registering duplicates
* `:prefab` corresponds to a prefab on a path within `Assets/Resources`
* `:mount-points` keys should match names of gameobjects in the part, vals are a probability map of which type is chosen. Note you can have a chance of no part (`{nil:10}`)
* `:hooks` are a map of functions that will be routed from the root entity.  The first arg is the root object, second is the part object.  You may want to use a var if you plan on re-evaluating the function at runtime.
* `:state` will be placed into the part state by the `:procjam/part` key


Parts will match the rotation of their mount point transform.  Our convention is `Z+ is forward` and `Y+ is up`, where an arm should extend along the `Z+` axis.

Part meshes that use the `SKIN` material will have a color assigned.  If your blend is saved in `Assets/blends`, you can just name a material "SKIN".

