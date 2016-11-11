(ns visual.core
  (:require
    [cljs.core.async :as async :refer [put! chan <!]]
    [monet.canvas :as canvas]
    [monet.geometry :as geom]
    [reagent.core :as reagent :refer [atom]]
    ))

(enable-console-print!)

;; -------------------------------------------------------------
;; Constants
;; -------------------------------------------------------------

(defonce WIDTH 500)
(defonce HEIGHT 500)

(defonce game-state
  (atom {:title "Draw shapes on the board"}))


;; -------------------------------------------------------------
;; Draw loop
;; -------------------------------------------------------------

(defn main-game-entity
  "Draw the main game entity in the canvas"
  []
  (canvas/entity
    @game-state
    (fn update [_] @game-state)
    (fn draw [ctx state]

      )))


;; -------------------------------------------------------------
;; Main rendering component
;; -------------------------------------------------------------

(defn main-render
  "Render the space ship game"
  []
  (reagent/create-class
    {:component-did-mount
     (fn did-mount []
       (let [ship-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity ship-canvas :game-entity (main-game-entity))
         (canvas/draw-loop ship-canvas)
         ))
     :reagent-render
     (fn render []
       [:div
        [:h1 (:title @game-state)]
        [:canvas#board {:width WIDTH :height HEIGHT}]
        ])
     }))

(reagent/render
  [main-render]
  (js/document.getElementById "app"))
