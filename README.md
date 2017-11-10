# procadia2017


## setup

* use the `2017.1.0` version of Unity if possible
* `git clone --recursive` or clone Arcadia into `Assets`

# Participants

* Joseph Parker - GitHub selfsame
* Douglas P. Fields, Jr. - GitHub LispEngineer
* Joshua Suskalo - GitHub IGJoshua
* Ramsey Nasser - Github nasser
* Tims Gardner - Github timsgardener


# parts

Our entites are collections of parts, which are defined like:

```clj
(use 'game.entity)

(part {
  :type :body
  :id :business
  :prefab :parts/business-body
  :hp 1
  :power 0
  :mount-points {
    :neck {:head 1}
    :left-arm {:arm 1}
    :right-arm {:arm 1}} 
  :hooks {:update (fn [root entity])}
  :state {:foo 1}
  :ai (behaviour [o]
  		(wait 1)
  		(fire o)
  		(AND (aim o)
             (wait 1))
        (end-fire o))})
 ```
* `:type` user taxonomy, `:mount-points` declare which types they allow. value can be a keyword or collection of keywords for multiple type registration
* `:id` optional, helps with redefining parts without registering duplicates
* `:prefab` corresponds to a prefab on a path within `Assets/Resources`
* `:hp` part's contribution to total entity `:hp` state, default is 1
* `:power` contribution to total entity difficulty, (should only be used with weapons), default is 0
* `:mount-points` keys should match names of gameobjects in the part, vals are a probability map of which type is chosen. Note you can have a chance of no part (`{nil 10}`)
* `:hooks` are a map of functions that will be routed from the root entity.  The first arg is the root object, second is the part object.  You may want to use a var if you plan on re-evaluating the function at runtime.
* `:state` will be placed into the part state by the `:procjam/part` key
* `:ai` function that returns a collection of `tween.core/timeline` style functions.  Recommended to use the `behaviour` macro, for more details see the AI section below.


Parts will match the rotation of their mount point transform.  Our convention is `Z+ is forward` and `Y+ is up`, where an arm should extend along the `Z+` axis.

Part meshes that use the `SKIN` material will have a color assigned.  If your blend is saved in `Assets/blends`, you can just name a material "SKIN".

# project structure

* `game.core` sets up the game loop
* `game.world` constructs game maps
* `game.entity` assembles parts into entities
* `game.data` ns with no deps for global stuff
* `game.play` game system functions (damage/kill), generic part hooks
* `game.ai` composable behaviour functions
* `game.fx` special effect fns

`part` definitions can go in user namespaces like `selfsame.clj`


# AI

Entity AI is described as timeline state machines (see https://github.com/selfsame/tween#timelines). Parts can contribute sequences via an `:ai` entry, which are concatenated and cycled through.

The `:ai` value should be a function that returns a collection of double wrapped functions (due to using `timeline-1`).

```clj
(part {
 :type :brain
 :ai (fn [root] 
       [(fn [] #(log "tick"))
        (fn [] (wait 1))])})
```

The `game.ai/behaviour` macro makes this a bit sleeker:

```clj
(part {
 :type :brain
 :ai (behaviour [o]
	   #(log "tick")
	    (wait 1))})
```

It's recommended to read through the [timeline docs](https://github.com/selfsame/tween#timelines) and `game.ai` before designing behaviour sequences. 

Here's a rundown of the default behavior:

```clj
(wait (?f 1))

;truthy while the player is far away, so the timeline parks here
(NOT (player-in-range? o 40))

; charge is always truthy, so we compose with a wait
; we also move on if the player is close enough
(AND (charge o)
     (NOT (player-in-range? o 6))
     (wait (?f 0.3 1.5)))

; due to our input model, we need to clear the :movement
; that was turned on during `charge`
(stop o)

; turn on :fire button
(fire o)

; aim is always truthy
(AND (aim o)
     (wait (?f 0.5 2.0)))

(end-fire o)

; falsey, sets :movement
(wander o)

; spend some time walking
(wait (?f 0.5 1.0))

; compose strafe and aim (both truthy)
(AND (strafe o (rand-nth [-1 1]))
     (aim o)
     (wait (?f 0.5 2.0)))

(stop o)
```