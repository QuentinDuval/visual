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


;; -------------------------------------------------------------
;; Create the main objects
;; -------------------------------------------------------------

(defrecord Point [x y])
(defrecord Line [line-start line-end])

(defn make-line [x0 y0 x1 y1]
  (Line. (Point. x0 y0) (Point. x1 y1)))


;; -------------------------------------------------------------
;; Game state
;; -------------------------------------------------------------

(defonce game-state
  (atom
    {:title "Draw shapes on the board"
     :lines [(make-line 100 100 100 100)]}
    ))


;; -------------------------------------------------------------
;; Drawing entities
;; -------------------------------------------------------------

(defn render-line
  "Render a line on the screen"
  [ctx line]
  (-> ctx
    (canvas/fill-style "red")
    (canvas/fill-rect
      {:x (:x (:line-start line))
       :y (:y (:line-start line))
       :w (:x (:line-end line))
       :h (:y (:line-end line))})
    ))

(defn main-game-entity
  "Draw the main game entity in the canvas"
  []
  (canvas/entity
    @game-state
    (fn update [_] @game-state)
    (fn draw [ctx state]
      (doseq [l (:lines state)]
        (render-line ctx l))
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
