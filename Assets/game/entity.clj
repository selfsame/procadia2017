(ns game.entity
  (use
    arcadia.core
    arcadia.linear
    hard.core))

(defn make-entity [budget]
  (clone! :player))