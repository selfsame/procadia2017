(ns game.data)

(defonce PLAYER (atom nil))
(defonce INPUT (atom nil))
(defonce CAMERA (atom nil))
(defonce CAMERA-AXIS (atom nil))
(defonce AIM (atom nil))
(defonce MENU (atom nil))
(defonce SWAPPED (atom nil))
(defonce SIZE (atom 0))
(defonce LEVEL (atom 0))
(def levels [
  [1 2 2]
  [2 3 5]
  [3 4 8]
  [5 4 10]
  [8 5 15]
  [12 5 15]
  [100 7 20]
  [100 7 30]])