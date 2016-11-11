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

(defrecord Vector [x y])

(defrecord Frame [ctx origin x-axis y-axis])

(defn make-frame
  "Create a frame with the provided origin, height and width"
  [ctx x y width height]
  (Frame.
    ctx
    (Vector. x y)
    (Vector. (+ x width) y)
    (Vector. x (+ y height))
    ))

(defn to-frame-x
  [{:keys [origin x-axis y-axis] :as frame} x]
  (+ (:x origin) (* (:x x-axis) x)))

(defn to-frame-y
  [{:keys [origin x-axis y-axis] :as frame} y]
  (+ (:y origin) (* (:y y-axis) y)))

(defn to-frame-coord
  "Transform a vector to fit the frame"
  ;; TODO - Does not yet take into account the rotations
  [frame v]
  (Vector.
    (to-frame-x frame (:x v))
    (to-frame-y frame (:y v))
    ))

;; TODO - Factorize
(defn line-to
  [frame x y]
  (let [p (to-frame-coord frame (Vector. x y))]
    (canvas/line-to
      (:ctx frame)
      (:x p)
      (:y p)
      ))
  frame)

;; TODO - Factorize
(defn move-to
  [frame x y]
  (let [p (to-frame-coord frame (Vector. x y))]
    (canvas/move-to
      (:ctx frame)
      (:x p)
      (:y p)
      ))
  frame)

(defn stroke
  [frame]
  (canvas/stroke (:ctx frame))
  frame)

(defn ellipse
  [frame x y w h]
  ;; The implementation is weird because canvas/ellipse is bugged
  (-> (:ctx frame)
    (canvas/save)
    (canvas/translate (to-frame-x frame x) (to-frame-y frame y))
    (canvas/ellipse {:x 0 :y 0
                     :rw (* w (get-in frame [:x-axis :x]))
                     :rh (* h (get-in frame [:y-axis :y]))})
    (canvas/restore)))

(defn circle
  [frame x y r]
  (ellipse frame x y r r))

(defn render-beside
  [ratio render-form1 render-form2]
  (fn [frame]
    (let [x-orign (get-in frame [:origin :x])
          x-scale (get-in frame [:x-axis :x])
          l-frame (update-in frame [:x-axis :x] #(* ratio %))
          r-frame (-> frame
                    (assoc-in [:origin :x] (+ x-orign (* x-scale ratio)))
                    (assoc-in [:x-axis :x] (* x-scale (- 1.0 ratio)))
                    )]
        (render-form1 l-frame)
        (render-form2 r-frame)
        frame
      )))


;; -------------------------------------------------------------
;; Basic frame objects
;; -------------------------------------------------------------

(defrecord Line [line-start line-end])
(defrecord Person [height width])

(defn make-line [x0 y0 x1 y1]
  (Line. (Vector. x0 y0) (Vector. x1 y1)))

(defn make-person [h w]
  (Person. h w))


;; -------------------------------------------------------------
;; Game state
;; -------------------------------------------------------------

(defonce game-state
  (atom
    {:title "Draw shapes on the board"
     :lines [(make-line 0.0 0.0 1.0 1.0) , (make-line 0.0 1.0 1.0 0.0)]
     :persons [(make-person 1.0 0.5)]
     }))


;; -------------------------------------------------------------
;; Drawing simple entities
;; -------------------------------------------------------------

(defn render-line
  "Render a line on the screen"
  [{:keys [line-start line-end]} frame]
  (-> frame
    (move-to (:x line-start) (:y line-start))
    (line-to (:x line-end) (:y line-end))
    (stroke)
    ))


;; -------------------------------------------------------------
;; Rendering complex shape
;; -------------------------------------------------------------

(defn render-person
  "Render a person like form: the line gives the perimeter"
  [{:keys [height width] :as person} frame]
  (let [head-base (* height 0.85)
        head-diag (- height head-base)
        head-ypos (+ head-base (/ head-diag 2))
        body-base (* height 0.45)
        body-xpos (* width 0.5)
        arms-base (* height 0.55)
        arm-width (* width 0.4)]
    (-> frame
      ;; The legs
      (move-to 0 0)
      (line-to body-xpos body-base)
      (line-to width 0)
      ;; The body
      (move-to body-xpos body-base)
      (line-to body-xpos head-base)
      ;; The arms
      (move-to body-xpos head-base)
      (line-to (- body-xpos arm-width) arms-base)
      (move-to body-xpos head-base)
      (line-to (+ body-xpos arm-width) arms-base)
      (stroke)
      ;; The head
      (circle body-xpos head-ypos (/ head-diag 2))
      )))


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
      (let [frame (make-frame ctx 0 0 WIDTH HEIGHT)]
        (canvas/stroke-width ctx 6)
        (doseq [l (:lines state)]
          (render-line l frame))
        (doseq [p (:persons state)]
          ((render-beside 0.5
             (partial render-person p)
             (partial render-person p))
            frame))
        ))))

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
