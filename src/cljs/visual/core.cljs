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
;; The frame object on which we can draw
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

(defn scale-vector [ratio v]
  (Vector. (* ratio (:x v)) (* ratio (:y v))))

(defn vector-sum [u v]
  (Vector. (+ (:x v) (:x u)) (+ (:y v) (:y u))))

(defn rotate-vector
  [angle {:keys [x y] :as v}]
  (let [sin (js/Math.sin angle)
        cos (js/Math.cos angle)]
    (Vector.
      (- (* x cos) (* y sin))
      (+ (* x sin) (* y cos))
      )))

(defn to-frame-vector
  "Transform a vector to fit the frame"
  [{:keys [origin x-axis y-axis] :as frame} v]
  (vector-sum
    origin
    (vector-sum
      (scale-vector (:x v) x-axis)
      (scale-vector (:y v) y-axis)
      )))

(defn to-frame-x [frame x]
  (:x (to-frame-vector frame (Vector. x 0))))

(defn to-frame-y [frame y]
  (:y (to-frame-vector frame (Vector. 0 y))))


;; -------------------------------------------------------------
;; Primitive methods to draw on the frame
;; -------------------------------------------------------------

(defn- forward-action-to-canvas
  [frame action x y]
  (let [p (to-frame-vector frame (Vector. x y))]
    (action (:ctx frame) (:x p) (:y p)))
  frame)

(defn line-to [frame x y]
  (forward-action-to-canvas frame canvas/line-to x y))

(defn move-to [frame x y]
  (forward-action-to-canvas frame canvas/move-to x y))

(defn stroke [frame]
  (canvas/stroke (:ctx frame)) frame)

(defn ellipse
  "Draw an elipse in the frame (the implementation workarounds a bug in canvas/ellipse)"
  [frame x y w h]
  (-> (:ctx frame)
    (canvas/save)
    (canvas/translate (to-frame-x frame x) (to-frame-y frame y))
    (canvas/ellipse {:x 0 :y 0
                     :rw (* w (get-in frame [:x-axis :x]))
                     :rh (* h (get-in frame [:y-axis :y]))})
    (canvas/restore)))

(defn circle [frame x y r]
  (ellipse frame x y r r))


;; -------------------------------------------------------------
;; Means of combination to draw on the frame
;; -------------------------------------------------------------

(defn render
  "A renderer is a function: Frame -> Frame + a display effect"
  [renderer frame] (renderer frame))

(defn- axis-renderer
  "Allows to combine two rendering functions to render two pictures:
   Each picture will be on one side of the an axis"
  [get-origin get-span ratio form1 form2]
  (fn [frame]
    (let [origin  (get-in frame get-origin)
          scale   (get-in frame get-span)
          frame-1 (update-in frame get-span #(* ratio %))
          frame-2 (-> frame
                    (assoc-in get-origin (+ origin (* scale ratio)))
                    (assoc-in get-span (* scale (- 1.0 ratio)))
                    )]
      (form1 frame-1)
      (form2 frame-2)
      )))

(defn beside
  "Combine two rendering functions to render side to side"
  [ratio left-form right-form]
  (axis-renderer [:origin :x] [:x-axis :x] ratio left-form right-form))

(defn above
  "Combine two rendering functions to render one on top of the other"
  [ratio top-form bottom-form]
  (axis-renderer [:origin :y] [:y-axis :y] ratio top-form bottom-form))

(defn rotate
  "Transform a rendering function to be rotated"
  [angle render-form]
  (fn [frame]
    (let [rotation #(rotate-vector angle %)
          r-frame (-> frame
                    (update-in [:x-axis] rotation)
                    (update-in [:y-axis] rotation)
                    )]
        (render-form r-frame))))


;; -------------------------------------------------------------
;; Basic GUI objects
;; -------------------------------------------------------------

(defrecord Line [line-start line-end])
(defrecord Person [height width])

(defn make-line [x0 y0 x1 y1]
  (Line. (Vector. x0 y0) (Vector. x1 y1)))

(defn make-person [h w]
  (Person. h w))

(defn line-renderer
  "Create a render function for a line"
  [{:keys [line-start line-end]}]
  (fn [frame]
    (-> frame
      (move-to (:x line-start) (:y line-start))
      (line-to (:x line-end) (:y line-end))
      (stroke)
      )))

(defn person-renderer
  "Create a render function for a person"
  [{:keys [height width] :as person}]
  (let [head-base (* height 0.85)
        head-diag (- height head-base)
        head-ypos (+ head-base (/ head-diag 2))
        body-base (* height 0.45)
        body-xpos (* width 0.5)
        arms-base (* height 0.55)
        arm-width (* width 0.4)]
    (fn [frame]
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
        ))))


;; -------------------------------------------------------------
;; TODO - Add a way to describe as data structure
;; -------------------------------------------------------------

;; TODO


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
;; Main rendering component
;; -------------------------------------------------------------

(defn main-entity
  "Draw the main entity in the canvas"
  []
  (canvas/entity
    @game-state
    (fn update [_] @game-state)
    (fn draw [ctx state]
      (let [frame (make-frame ctx 0 0 WIDTH HEIGHT)]
        (canvas/stroke-width ctx 6)
        (doseq [l (:lines state)]
          (render
            (rotate 0.2 (line-renderer l))
            frame))
        (doseq [p (:persons state)]
          (render
            (beside 0.3
              (person-renderer p)
              (above 0.4
                (person-renderer p)
                (person-renderer p)))
            frame))
        ))))

(defn main-render
  "Prepare the main canvas and render the whole page afterwards"
  []
  (reagent/create-class
    {:component-did-mount
     (fn did-mount []
       (let [main-canvas (canvas/init (js/document.getElementById "board") "2d")]
         (canvas/add-entity main-canvas :main-entity (main-entity))
         (canvas/draw-loop main-canvas)
         ))
     :reagent-render
     (fn render []
       [:div
        [:h1 (:title @game-state)]
        [:canvas#board {:width WIDTH :height HEIGHT}]
        ])
     }))

(reagent/render [main-render]
  (js/document.getElementById "app"))
