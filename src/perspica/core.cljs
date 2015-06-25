(ns ^:figwheel-always perspica.core
    (:require[om.core :as om :include-macros true]
             [om.dom :as d :include-macros true]
             [goog.dom :as dom]))

(enable-console-print!)

(def app-state (atom {:sketchpad-width 500
                      :sketchpad-height 500
                      :brush-limit 5
                      :brush-dim [3 2]
                      :brush-hover-dim [0 0]
                      }))

(defn make-brush [data owner]
  (apply
   (partial d/table #js {:id "brush-size"})
   (for [x (range (:brush-limit data))]
     (apply
      (partial d/tr nil)
      (for [y (range (:brush-limit data))]
        (let [inside? (fn [[width height]]
                        (and (<= x width)
                             (<= y height)))]
          (d/td
           #js {:className
                (clojure.string/join
                 " "
                 [(cond
                    (and (not (inside? (:brush-dim data)))
                         (inside? (:brush-hover-dim data)))
                    "plus"
                    (and (inside? (:brush-dim data))
                         (not (inside? (:brush-hover-dim data))))
                    "minus"
                    (inside? (:brush-dim data))
                    "selected"
                    :else "")
                  (if (inside? (:brush-hover-dim data))
                    "hover"
                    "")])
                :onClick
                #(swap! app-state assoc :brush-dim [x y])
                :onMouseOver
                #(swap! app-state assoc :brush-hover-dim [x y])}
           "a")))))))

(defn make-palette [data owner]
  (d/div
   #js {:id "texture-palette"}
   (let [textures ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j"]]
     (clj->js (map #(d/ul #js {:className "texture"} %) textures)))))

(defn main-app [data owner]
  (reify om/IRender
    (render [_]
      (d/div
       nil
       (d/div
        #js {:id "left-options-bar"}
        (make-palette data owner)
        (make-brush data owner)
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
