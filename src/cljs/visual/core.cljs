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

(defonce WIDTH 800)
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
     :lines [(make-line 100 100 200 200)]}
    ))


;; -------------------------------------------------------------
;; Drawing entities
;; -------------------------------------------------------------

(defn render-line
  "Render a line on the screen"
  [ctx {:keys [line-start line-end]}]
  (-> ctx
    (canvas/stroke-width 6)
    (canvas/stroke-style "black")
    (canvas/move-to (:x line-start) (:y line-start))
    (canvas/line-to (:x line-end) (:y line-end))
    (canvas/stroke)
    ))




#_(defn render-rect
  "Render a rectangle on the screen"
  [ctx rect]
  (-> ctx
    (canvas/fill-rect
      {:x (:x (:line-start rect))
       :y (:y (:line-start rect))
       :w (:x (:line-end rect))
       :h (:y (:line-end rect))})
    ))


;; -------------------------------------------------------------
;; Rendering complex shape
;; -------------------------------------------------------------

(defn render-person
  "Render a person like form: the line gives the perimeter"
  [ctx person]
  (-> ctx
    (canvas/save)
    ;; The legs
    (canvas/move-to 0 0)
    (canvas/line-to 75 100)
    (canvas/line-to 150 0)
    ;; The body
    (canvas/move-to 75 100)
    (canvas/line-to 75 200)
    ;; The arms
    (canvas/move-to 75 200)
    (canvas/line-to 10 150)
    (canvas/move-to 75 200)
    (canvas/line-to 140 150)
    (canvas/stroke)

    ;; The head
    (canvas/circle {:x 75 :y 220 :r 20})
    ;; Draw
    (canvas/restore)
    ))





;; -------------------------------------------------------------
;; Main rendering component
;; -------------------------------------------------------------

(defn main-game-entity
  "Draw the main game entity in the canvas"
  []
  (canvas/entity
    @game-state
    (fn update [_] @game-state)
    (fn draw [ctx state]
      (doseq [l (:lines state)]
        (render-line ctx l))
      (render-person ctx 1)
      )))

(defn main-render
  "Render the space ship game"
  []
  (reagent/create-class
    {:component-did-mount
     (fn did-mount []
       (let [main-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity main-canvas :main-entity (main-game-entity))
         (canvas/draw-loop main-canvas)
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
