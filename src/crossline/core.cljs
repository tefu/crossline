(ns ^:figwheel-always crossline.core
    (:require[om.core :as om :include-macros true]
             [om.dom :as d :include-macros true]
             [goog.events :as events]
             [sablono.core :as html :include-macros true]))

(enable-console-print!)

(def app-state (atom {:sketchpad-width 500
                      :sketchpad-height 500
                      :brush {:limit 5
                              :dim [3 2]
                              :hover-dim [0 0]}
                      :theta 0
                      :phi 0}))

(defn brush-table-element-classes
  [x y brush-coord hover-coord]
  (letfn [(inside? [[width height]]
            (and (<= x width)
                 (<= y height)))]
    (clojure.string/join
     " "
     [(cond
        (and (not (inside? brush-coord)) (inside? hover-coord)) "plus"
        (and (inside? brush-coord) (not (inside? hover-coord))) "minus")
      (if (inside? brush-coord) "selected")
      (if (inside? hover-coord) "hover")])))

(defn brush-table-element [data owner x y]
  (d/td
   #js {:className
        (brush-table-element-classes x y (:dim data) (:hover-dim data))
        :onClick
        (fn [] (om/transact! data #(assoc % :dim [x y])))
        :onMouseOver
        (fn [] (om/transact! data #(assoc % :hover-dim [x y])))}
   ""))

(defn brush-size-grid-table [data owner table-rows]
  (d/table #js {:id "brush-size-grid"
                :onMouseLeave
                (fn []
                  (om/transact!
                   data
                   #(assoc % :hover-dim (:dim %))))}
           table-rows))

(defn brush-size-grid [data owner]
  (om/component
   (brush-size-grid-table
    data owner
    (clj->js
     (for [x (range (:limit data))]
       (d/tr
        nil
        (clj->js
         (for [y (range (:limit data))]
           (brush-table-element data owner x y)))))))))

(defn palette [data owner]
  (om/component
   (d/div
    #js {:id "texture-palette"}
    (let [textures ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j"]]
      (clj->js (map #(d/ul #js {:className "texture"} %) textures))))))

(defn canvas-coordinates [event]
  [(.-offsetX event) (.-offsetY event)])

(defn slope [[x1 y1] [x2 y2]]
  (/ (- y2 y1) (- x2 x1)))

(defn distance [[x1 y1] [x2 y2]]
  (.sqrt js/Math (+ (.pow js/Math (- x2 x1) 2)
                    (.pow js/Math (- y2 y1) 2))))

;; Find the circumscribed circle given coordinates of a triangle's three points.
;; Using the equation from here: http://paulbourke.net/geometry/circlesphere/
(defn triangle->circle [[x1 y1] [x2 y2] [x3 y3]]
  (let [slope-a (slope [x1 y1] [x2 y2]) 
        slope-b (slope [x2 y2] [x3 y3])
        x-center (/ (+ (* slope-a slope-b (- y1 y3))
                       (* slope-b (+ x1 x2))
                       (- (* slope-a (+ x2 x3))))
                    (* 2 (- slope-b slope-a)))
        y-center (- (/ (+ y1 y2) 2)
                    (/ (- x-center (/ (+ x1 x2) 2)) slope-a))
        radius (distance [x1 y1] [x-center y-center])]
    [x-center y-center radius]))

(defn draw-arc [canvas event]
  (let [click-coords (canvas-coordinates event)
        left [0 250]
        right [500 250]
        [arc-x arc-y radius] (triangle->circle left click-coords right)
        ctx  (.getContext canvas "2d")]
    (.beginPath ctx)
    (.arc ctx arc-x arc-y radius 0 (* 2 (.-PI js/Math)))
    (.stroke ctx)))

(defn sketchpad [data owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [sketchpad (om/get-node owner "sketchpad-ref")]
        (events/listen sketchpad "mousedown" #(draw-arc sketchpad %))))

    om/IRender
    (render [_]
      (d/div nil (d/canvas #js {:id "sketchpad"
                                :ref "sketchpad-ref"
                                :width (:sketchpad-width data)
                                :height (:sketchpad-height data)
                                })))))

(defn main-app [data owner]
  (om/component
   (d/div
    nil
    (d/div
     #js {:id "left-options-bar"}
     (om/build palette data)
     (om/build brush-size-grid (:brush data))
     (d/div #js {:id "perspective-grid"}
            (d/input #js {:type "text"
                          :value (:theta data)
                          :onChange
                          (fn [e]
                            (om/transact!
                             data #(assoc % :theta
                                          (js/parseInt (.. e -target -value)))))})
            (d/input #js {:type "text"
                          :value (:phi data)
                          :onChange
                          (fn [e]
                            (om/transact!
                             data #(assoc % :phi
                                          (js/parseInt (.. e -target -value)))))}))
     (d/div #js {:id "perspective-selection"} "perspective-selection"
            (d/li nil "1 pt")
            (d/li nil "2 pt")
            (d/li nil "3 pt")
            (d/li nil "4 pt")
            (d/li nil "5 pt")))
    (d/div
     #js {:id "sketchpad-frame"}
     "sketchpad-frame"
     (om/build sketchpad data)
     (d/div #js {:id "time-machine"})))))

(om/root
 main-app
 app-state
 {:target (. js/document (getElementById "app"))})



(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
