(ns tacaa.imptune
  (:import (simulator.predictions ImpressionEstimatorTune)))

(defn random-perm
  [p1 p2 p3 p4 p5 p6 p7 p8]
  (seq (ImpressionEstimatorTune/runImpressionEstimator p1 p2 p3 p4 p5 p6 p7 p8)))


(defn perc-change
  [v1 v2]
  (Math/abs ^Double (/ (- v2 v1) v1)))

(defn compare-perm
  [new-sol best-sol]
  (let [[new-parms [new-imp-err new-rank-err]] new-sol
        [best-params [best-imp-err best-rank-err]] best-sol]
    (if (< new-imp-err
           best-imp-err)
      (if (>= new-rank-err
             best-rank-err)
        new-sol
        (if (> (perc-change new-imp-err best-imp-err)
               (* 2 (perc-change best-rank-err new-rank-err)))
          new-sol
          best-sol))
      (if (>= new-rank-err
             best-rank-err)
        (if (> (perc-change best-rank-err new-rank-err)
               (* 2 (perc-change new-imp-err best-imp-err)))
          new-sol
          best-sol)
        best-sol))))


(defn test-perm
  [best-sol p1 p2 p3 p4 p5 p6 p7 p8]
  (let [new-sol [[p1 p2 p3 p4 p5 p6 p7 p8] (vec (random-perm p1 p2 p3 p4 p5 p6 p7 p8))]]
    (compare-perm new-sol best-sol)))

(defn find-best-perm
  [iters]
  (loop [iters iters
         best-sol [[0 0 0 0] [1000 1000]]]
    (do
      (prn "Current Best Solutions: " best-sol "  iters left: " iters)
      (if (not (> iters 0))
        best-sol
        (recur (dec iters)
               (test-perm best-sol (rand) (rand) (rand) (rand)
                          (+ (rand) (rand)) (+ (rand) (rand))
                          (+ (rand) (rand)) (+ (rand) (rand))))))))
