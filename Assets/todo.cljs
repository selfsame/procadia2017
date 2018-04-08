
[need]
([x] bounding walls around level)
([ ] player :hp
  ([ ] base + form * player multiplier)
  ([ ] killing things heals a bit)
  ([ ] new form - calc ratio and apply to new max-hp))
([x] death
  ([x] restart game))
([ ] win screen)
([x] HUD
  ([x] hp meter)
  ([x] x/y enemies killed))
([ ] sound fx
  ([ ] bullet/impact)
  ([ ] form swap)
  ([ ] game over))

[want]
([ ] death screen (form, kills, killed by))
([ ] tentacles don't collide with bounds)




[postcompo fixes]
([x] fix bullet line-redering)
([x] add delta to fire rate)
([x] remove the quality settings (at least just shadows))
([x] tween bug)

[new features]
([/] minimap - camera / render texture
  ([x] setup minimap camera based on the level size)
  ([x] enemy blips)
  ([ ] revealing the map))
([ ] new body parts
  ([x] tank treads - turns slowly)
  ([ ] ball - velocity)
  ([ ] mine)
  ([ ] shotgun/machine)
  ([ ] organic connector variants (neck arm head)))
([ ] better sound FX)
([ ] shoot prisoner spheres to move
  ([ ] bullet/wall sound))

[bugs]
([ ] can't reliably shoot when there's no enemies)
([ ] entities not starting on spawn points)