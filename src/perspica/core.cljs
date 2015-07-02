(ns ^:figwheel-always perspica.core
    (:require[om.core :as om :include-macros true]
             [om.dom :as d :include-macros true]
             [goog.dom :as dom]))

(enable-console-print!)

(def app-state (atom {:sketchpad-width 500
                      :sketchpad-height 500
                      :brush
                      {
                       :limit 5
                       :dim [3 2]
                       :hover-dim [0 0]}}))

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

(defn brush-size-grid [data owner]
  (reify om/IRender
    (render [_]
      (apply
       (partial d/table #js {:id "brush-size-grid"
                             :onMouseLeave
                             (fn []
                               (om/transact!
                                data
                                #(assoc % :hover-dim (:dim %))))})
       (for [x (range (:limit data))]
         (apply
          (partial d/tr nil)
          (for [y (range (:limit data))]
            (brush-table-element data owner x y))))))))

(defn palette [data owner]
  (reify om/IRender
    (render [_]
      (d/div
       #js {:id "texture-palette"}
       (let [textures ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j"]]
         (clj->js (map #(d/ul #js {:className "texture"} %) textures)))))))

(defn main-app [data owner]
  (reify om/IRender
    (render [_]
      (d/div
       nil
       (d/div
        #js {:id "left-options-bar"}
        (om/build palette data)
        (om/build brush-size-grid (:brush data))
        (d/canvas #js {:id "perspective-grid"} "perspective-grid")
        (d/div #js {:id "perspective-selection"} "perspective-selection"
               (d/li nil "1 pt")
               (d/li nil "2 pt")
               (d/li nil "3 pt")
               (d/li nil "4 pt")
               (d/li nil "5 pt")))
       (d/div
        #js {:id "sketchpad-frame"}
        "sketchpad-frame"
        (d/canvas #js {:id "sketch-pad"
                       :width (:sketchpad-width data)
                       :height (:sketchpad-height data)})
        (d/div #js {:id "time-machine"}))))))

(om/root
 main-app
 app-state
 {:target (. js/document (getElementById "app"))})


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
