(ns tacaa.viz
 (:require (tacaa core parser)
            (incanter core charts stats datasets)))
            
            
(defn plot-dists
  [status day num-ests]
  (let [stats-maps-expected (map (fn [_] (stats-summary-expected status day)) (range num-ests))
        stats-maps-random (map (fn [_] (stats-summary-random status day)) (range num-ests))
        map-of-vecs-expected (maps-to-map-of-vecs stats-maps-expected)
        map-of-vecs-random (maps-to-map-of-vecs stats-maps-random)
        sampled-stats (day-summary status day)]
    (map (fn [[k1 v1] [k2 v2]]
           (let [mean1 (calc-mean v1)
                 sd1 (calc-std-dev v1 mean1)
                 mean2 (calc-mean v2)
                 sd2 (calc-std-dev v2 mean2)
                 actual (sampled-stats k1)
                 actualv (double 0.0)
                 allv (concat v1 v2 [actual])
                 xmin (find-min allv)
                 xmax (find-max allv)]
             (doto (incanter.charts/function-plot (partial gauss-pdf mean1 sd1)
                                                  xmin
                                                  xmax
                                                  :series-label "expected"
                                                  :legend true
                                                  :title k1)
               (incanter.charts/add-function (partial gauss-pdf mean2 sd2)
                                             xmin
                                             xmax
                                             :series-label "random")
               (incanter.charts/add-pointer actual actualv :text "actual")
               incanter.core/view)))
         map-of-vecs-expected
         map-of-vecs-random)))
