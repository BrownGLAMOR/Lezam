(ns tacaa.test
  (:require (tacaa parser)
            (criterium core stats well)
            ;(incanter core charts stats datasets)
            )
  (:use tacaa.core)
  (:import (java.util Random)
           (simulator.parser GameStatusHandler)
           (agents AbstractAgent)
           (agents.modelbased MCKP)
           (se.sics.tasim.aw Message)
           (edu.umich.eecs.tac.props BidBundle QueryReport SalesReport
                                     Query Product Ad UserClickModel
                                     AdvertiserInfo PublisherInfo
                                     SlotInfo QueryType RetailCatalog)))

;*****helper function to generate test bundles*
;generate-test-bundle: (number vector) -> (num -> num -> 'a)-> 'a vector
;Input: 
;- vec, a vector of numbers
;- fun, a function that takes in two numbers and outputs an 'a object
;Output: a 60-vector of 'a constructed with values from vec.
; here, particularly, we get 'a objects containing two queries with the budgets
; for query1 drawn from vec and query2 drawn from (- max-vec vec)
(defn generate-test-vector [fun vect]
  (let
    [max-val (apply max vect)]
    (loop [n 0
           coll (transient [])]
      (if (>= n (count vect))
        (persistent! coll)
        (recur (inc n)
               (conj! coll (fun (vect n) max-val)))))))

;*****get-exp-convs*****
(clojure.core/println "testing get-exp-convs")
(clojure.core/println (= (tacaa.core/get-exp-convs 1 0.996 1 0.0) 0))
(clojure.core/println (= (tacaa.core/get-exp-convs 1 0.5 -2 5) 1.125))

(clojure.core/println "testing convr-penalty2")
;(valid) base cases
(clojure.core/println (= (tacaa.core/convpr-penalty2 0.996 -1 0) 0.996))
(clojure.core/println (= (tacaa.core/convpr-penalty2 0.996 1 0) 1))
(clojure.core/println (= (tacaa.core/convpr-penalty2 0.996 1 0.8) 1))
(clojure.core/println (= (tacaa.core/convpr-penalty2 0.996 1 1.5) 1))
;recursive cases
(clojure.core/println (= (tacaa.core/convpr-penalty2 0.5 -1 1.5) 0.25))
(clojure.core/println (= (tacaa.core/convpr-penalty2 0.5 1 2.5) (/ 2. 3.)))
;4-argument function
(clojure.core/println (= (tacaa.core/convpr-penalty2 1 0.5 -2 5) 0.225))

;*****mk-adv-effect-map*****
(clojure.core/println "testing mk-adv-effect-map")
(let
  [test-query (new Query "lioneer" "tv")
   test-query2 (new Query "pg" "audio")
   test-UCM (new UserClickModel (into-array [test-query]) (into-array ["agent1"]))
   agents-vec ["agent1", "agent2"]
   query-coll [test-query, test-query2]
   test-UCM2 (new UserClickModel (into-array query-coll) (into-array agents-vec))]
  (do (.setAdvertiserEffect test-UCM 0 0 0.8)
    (.setAdvertiserEffect test-UCM2 0 0 0.8)
    (.setAdvertiserEffect test-UCM2 1 0 0.7)
    (.setAdvertiserEffect test-UCM2 0 1 0.6)
    (.setAdvertiserEffect test-UCM2 1 1 0.5)
    ;base cases
    (clojure.core/println (= (mk-adv-effect-map {}) {})) ;empty agent vector
    (clojure.core/println (= (mk-adv-effect-map {:agents ["agent1"], :user-click test-UCM}) {"agent1" {}})) ;empty query space
    ;recursive cases
    (clojure.core/println (= (mk-adv-effect-map {:agents ["agent1"], :user-click test-UCM, :query-space [test-query]}) {"agent1" {test-query 0.8}})) ;empty query space
    (clojure.core/println (= (mk-adv-effect-map {:agents agents-vec, :user-click test-UCM2, :query-space query-coll}) 
                             {"agent1" {test-query 0.8, test-query2 0.7},
                              "agent2" {test-query 0.6, test-query2 0.5}}))))

;*****mk-cont-prob-map*****
(clojure.core/println "testing mk-cont-prob-map")
(let
  [test-query (new Query "lioneer" "tv")
   test-query2 (new Query "pg" "audio")
   test-UCM (new UserClickModel (into-array [test-query]) (into-array ["agent1"]))
   query-coll [test-query, test-query2]
   test-UCM2 (new UserClickModel (into-array query-coll) (into-array ["agent1"]))]
  (do
    (.setContinuationProbability test-UCM 0 0.5)
    (.setContinuationProbability test-UCM2 0 0.5)
    (.setContinuationProbability test-UCM2 1 0.75)
    ;base case
    (clojure.core/println (= (mk-cont-prob-map {}) {})) ;empty query space
    ;recursive cases
    (clojure.core/println (= (mk-cont-prob-map {:agents ["agent1"], :user-click test-UCM, :query-space [test-query]}) {test-query 0.5}))
    (clojure.core/println (= (mk-cont-prob-map {:agents ["agent1"], :user-click test-UCM2, :query-space (into-array query-coll)}) 
                             {test-query 0.5, test-query2 0.75}))))

;*****mk-man-spec-map*****
(clojure.core/println "testing mk-man-spec-map")
(let
  [test-info1 (new AdvertiserInfo)
   test-info2 (new AdvertiserInfo)
   agents-vec ["agent1", "agent2"]
   test-adv-info (zipmap agents-vec [test-info1, test-info2])]
  (do
    (.setManufacturerSpecialty test-info1 "lioneer")
    (.setManufacturerSpecialty test-info2 "pg")
    ;base case
    (clojure.core/println (= (mk-man-spec-map {}) {}))
    ;recursive cases
    (clojure.core/println (= (mk-man-spec-map {:adv-info {"agent1" test-info1} :agents ["agent1"]}) {"agent1" "lioneer"}))
    (clojure.core/println (= (mk-man-spec-map {:adv-info test-adv-info :agents agents-vec}) 
                             {"agent1" "lioneer" "agent2" "pg"}))))

;*****mk-comp-spec-map*****
(clojure.core/println "testing mk-comp-spec-map")
(let
  [test-info1 (new AdvertiserInfo)
   test-info2 (new AdvertiserInfo)
   agents-vec ["agent1", "agent2"]
   test-adv-info (zipmap agents-vec [test-info1, test-info2])]
  (do
    (.setComponentSpecialty test-info1 "tv")
    (.setComponentSpecialty test-info2 "audio")
    ;base case
    (clojure.core/println (= (mk-comp-spec-map {}) {}))
    ;recursive cases
    (clojure.core/println (= (mk-comp-spec-map {:adv-info {"agent1" test-info1} :agents ["agent1"]}) {"agent1" "tv"}))
    (clojure.core/println (= (mk-comp-spec-map {:adv-info test-adv-info :agents agents-vec}) 
                             {"agent1" "tv" "agent2" "audio"}))))

;*****mk-capacity-map*****
(clojure.core/println "testing mk-capacity-map")
(let
  [test-info1 (new AdvertiserInfo)
   test-info2 (new AdvertiserInfo)
   agents-vec ["agent1", "agent2"]
   test-adv-info (zipmap agents-vec [test-info1, test-info2])]
  (do
    (.setDistributionCapacity test-info1 25)
    (.setDistributionCapacity test-info2 50)
    ;base case
    (clojure.core/println (= (mk-capacity-map {}) {}))
    ;recursive cases
    (clojure.core/println (= (mk-capacity-map {:adv-info {"agent1" test-info1} :agents ["agent1"]}) {"agent1" 25}))
    (clojure.core/println (= (mk-capacity-map {:adv-info test-adv-info :agents agents-vec}) 
                             {"agent1" 25 "agent2" 50}))))

;*****mk-single-budget-map*****
(clojure.core/println "testing mk-single-budget-map")
(let
  [test-bundle (new BidBundle)
   test-query1 (new Query "lioneer" "tv")
   test-query2 (new Query "pg" "audio")]
  (do
    (.setCampaignDailySpendLimit test-bundle 1000.0)
    (.addQuery test-bundle test-query1 4.0 (new Ad) 100.0)
    (.addQuery test-bundle test-query2 6.0 (new Ad) 2000.0)
    ;base case (empty query space)
    (clojure.core/println (= (mk-single-budget-map test-bundle []) {:total-budget 1000.0}))
    ;recursive cases
    (clojure.core/println (= (mk-single-budget-map test-bundle [test-query1]) {:total-budget 1000.0, test-query1 100.0}))
    (clojure.core/println (= (mk-single-budget-map test-bundle [test-query1 test-query2]) 
                             {:total-budget 1000.0 test-query1 100.0 test-query2 1000.0}))))

;*****mk-budget-map*****
(clojure.core/println "testing mk-budget-map")
;makes a test BidBundle with varying budgets
(defn make-bundle-budget [k max-val]
  (let
    [test-query1 (new Query "lioneer" "tv")
     test-query2 (new Query "pg" "audio")]
   (doto (new BidBundle)
     (.setCampaignDailySpendLimit (* (inc k) 1000.0))
     (.addQuery test-query1 1.0 (new Ad) (double k))
     (.addQuery test-query2 1.0 (new Ad) (double (- max-val k))))))
;makes a test result map from queries to budgets, + total budget
(defn make-query-map [k max-val]
    (let
      [test-query1 (new Query "lioneer" "tv")
	   test-query2 (new Query "pg" "audio")]
      {:total-budget (* (inc k) 1000.0) test-query1 (double k) test-query2 (double (- max-val k))}))
;tests
(let
   [test-bundle-coll1 (generate-test-vector make-bundle-budget (vec (range 59)))
    test-bundle-coll2 (generate-test-vector make-bundle-budget (vec (range 0 118 2)))
    agents-vec ["agent1" "agent2"]
    queries-vec [(new Query "lioneer" "tv") (new Query "pg" "audio")]
    test-bundle-map1 {"agent1" test-bundle-coll1}
    test-bundle-map2 (zipmap agents-vec [test-bundle-coll1 test-bundle-coll2])]
  (do
    ;base case
    (clojure.core/println (= (mk-budget-map {}) {}))
    ;recursive cases (on a simple two-query query space)
    (clojure.core/println (= (mk-budget-map {:bid-bundle test-bundle-map1 :agents ["agent1"] :query-space queries-vec})
                             {"agent1" (generate-test-vector make-query-map (vec (range 59)))}))
    (clojure.core/println (= (mk-budget-map {:bid-bundle test-bundle-map2 :agents agents-vec :query-space queries-vec})
                             {"agent1" (generate-test-vector make-query-map (vec (range 59)))
                              "agent2" (generate-test-vector make-query-map (vec (range 0 118 2)))}))))

;*****mk-single-ad-map*****
(clojure.core/println "testing mk-single-ad-map")
(let
  [test-bundle (new BidBundle)
   test-query1 (new Query "lioneer" "tv")
   test-query2 (new Query "pg" "audio")]
  (do
    (.addQuery test-bundle test-query1 4.0 (new Ad (new Product "lioneer" "tv")) 100.0)
    (.addQuery test-bundle test-query2 6.0 (new Ad) 2000.0)
    ;base case (empty query space)
    (clojure.core/println (= (mk-single-ad-map test-bundle []) {}))
    ;recursive cases
    (clojure.core/println (= (mk-single-ad-map test-bundle [test-query1]) {test-query1 (new Ad (new Product "lioneer" "tv"))}))
    (clojure.core/println (= (mk-single-ad-map test-bundle [test-query1 test-query2]) 
                             {test-query1 (new Ad (new Product "lioneer" "tv"))
                              test-query2 (new Ad)}))))

;*****mk-ad-map*****
(clojure.core/println "testing mk-ad-map")
;makes a BidBundle with varying ad types
(defn make-bundle-ad [k max-val]
  (let
    [test-query1 (new Query "lioneer" "tv")
     test-query2 (new Query "pg" "audio")]
    (if (even? k)
      (doto (new BidBundle)
        (.addQuery test-query1 1.0 (new Ad (new Product "lioneer" "tv")))
        (.addQuery test-query2 1.0 (new Ad)))
      (doto (new BidBundle)
        (.addQuery test-query1 1.0 (new Ad))
        (.addQuery test-query2 1.0 (new Ad (new Product "pg" "audio")))))))
;makes a sample output map with varying ad types
(defn make-test-ad-map [k max-val]
  (let
    [test-query1 (new Query "lioneer" "tv")
     test-query2 (new Query "pg" "audio")]
    (if (even? k)
      {test-query1 (new Ad (new Product "lioneer" "tv"))
       test-query2 (new Ad)}
      {test-query1 (new Ad)
       test-query2 (new Ad (new Product "pg" "audio"))})))
;tests
(let
   [test-bundle-coll1 (generate-test-vector make-bundle-ad (vec (range 59)))
    test-bundle-coll2 (generate-test-vector make-bundle-ad (vec (range 0 118 2)))
    agents-vec ["agent1" "agent2"]
    queries-vec [(new Query "lioneer" "tv") (new Query "pg" "audio")]
    test-bundle-map1 {"agent1" test-bundle-coll1}
    test-bundle-map2 (zipmap agents-vec [test-bundle-coll1 test-bundle-coll2])]
  (do
    ;base case
    (clojure.core/println (= (mk-ad-map {}) {}))
    ;recursive cases (on a simple two-query query space)
    (clojure.core/println (= (mk-ad-map {:bid-bundle test-bundle-map1 :agents ["agent1"] :query-space queries-vec})
                             {"agent1" (generate-test-vector make-test-ad-map (vec (range 59)))}))
    (clojure.core/println (= (mk-ad-map {:bid-bundle test-bundle-map2 :agents agents-vec :query-space queries-vec})
                             {"agent1" (generate-test-vector make-test-ad-map (vec (range 59)))
                              "agent2" (generate-test-vector make-test-ad-map (vec (range 0 118 2)))}))))

;*****mk-single-squashed-bid-map*****
(clojure.core/println "testing mk-single-squashed-bid-map")
(let
  [test-bundle (new BidBundle)
   test-query1 (new Query "lioneer" "tv")
   test-query2 (new Query "pg" "audio")
   test-adv-effect-map {test-query1 0.81, test-query2 0.25}
   squash-param 0.5]
  (do
    (.addQuery test-bundle test-query1 2.0 (new Ad))
    (.addQuery test-bundle test-query2 8.0 (new Ad))
    ;base case (empty queryspace)
    (clojure.core/println (= (mk-single-squashed-bid-map test-bundle test-adv-effect-map squash-param []) {}))
    ;recursive cases
    (clojure.core/println (= (mk-single-squashed-bid-map test-bundle 
                                                         test-adv-effect-map 
                                                         squash-param 
                                                         [test-query1]) 
                             {test-query1 1.8}))
    (clojure.core/println (= (mk-single-squashed-bid-map test-bundle 
                                                         test-adv-effect-map 
                                                         squash-param 
                                                         [test-query1 test-query2]) 
                             {test-query1 1.8 test-query2 4.0}))))

;*****mk-sqashed-bid-map*****
(clojure.core/println "testing mk-squashed-bid-map")
;makes a BidBundle with varying bids
(defn make-bundle-squashed-bid [k max-val]
  (let
    [test-query1 (new Query "lioneer" "tv")
     test-query2 (new Query "pg" "audio")]
    (doto (new BidBundle)
      (.addQuery test-query1 (double k) (new Ad))
      (.addQuery test-query2 (double (- max-val k)) (new Ad)))))
;makes a resulting map with varying squashed-bids
(defn make-test-squashed-bid-map [adv-map k max-val]
  (let
    [test-query1 (new Query "lioneer" "tv")
     test-query2 (new Query "pg" "audio")
     squash-param 0.5]
   {test-query1 (* k (Math/pow (adv-map test-query1) squash-param))
    test-query2 (* (double (- max-val k)) (Math/pow (adv-map test-query2) squash-param))}))

;tests
(let  
  [test-bundle-coll1 (generate-test-vector make-bundle-squashed-bid (vec (range 59)))
   test-bundle-coll2 (generate-test-vector make-bundle-squashed-bid (vec (range 0 118 2)))
   agents-vec ["agent1" "agent2"]
   test-query1 (new Query "lioneer" "tv")
   test-query2 (new Query "pg" "audio")
   queries-vec [test-query1 test-query2]
   test-bundle-map1 {"agent1" test-bundle-coll1}
   test-bundle-map2 (zipmap agents-vec [test-bundle-coll1 test-bundle-coll2])
   test-adv-effect-map {"agent1" {test-query1 0.81, test-query2 0.25}
                        "agent2" {test-query1 0.64, test-query2 0.36}}
   squash-param 0.5]
  (do
    ;base case
    (clojure.core/println (= (mk-squashed-bid-map {:squash-param squash-param}) {}))
    ;recursive cases
    (clojure.core/println (= (mk-squashed-bid-map {:bid-bundle test-bundle-map1 :agents ["agent1"] :query-space queries-vec
                                                   :adv-effects test-adv-effect-map :squash-param squash-param})
                             {"agent1" (generate-test-vector (partial make-test-squashed-bid-map (test-adv-effect-map "agent1")) (vec (range 59)))}))
    (clojure.core/println (= (mk-squashed-bid-map {:bid-bundle test-bundle-map2 :agents agents-vec :query-space queries-vec
                                                   :adv-effects test-adv-effect-map :squash-param squash-param})
                             {"agent1" (generate-test-vector (partial make-test-squashed-bid-map (test-adv-effect-map "agent1")) (vec (range 59)))
                              "agent2" (generate-test-vector (partial make-test-squashed-bid-map (test-adv-effect-map "agent2")) (vec (range 0 118 2)))}))))

;*****mk-prod-query-map*****
(clojure.core/println "testing mk-prod-query-map")
;just an easy way to generate a particular resulting test vector
(defn make-test-query-vec [manufacturer component]
  [(new Query) (new Query nil component)
   (new Query manufacturer nil) (new Query manufacturer component)])
;only testing the 1-argument version
(let
  [prod1 (new Product "lioneer" "tv")
   prod2 (new Product "pg" "audio")]
  (do
    ;base case
    (clojure.core/println (= (mk-prod-query-map {}) {}))
    ;recursive case
    (clojure.core/println (= (mk-prod-query-map {:retail-cat [prod1]}) {prod1 (make-test-query-vec "lioneer" "tv")}))
    (clojure.core/println (= (mk-prod-query-map {:retail-cat [prod1 prod2]}) 
                             {prod1 (make-test-query-vec "lioneer" "tv")
                              prod2 (make-test-query-vec "pg" "audio")}))))

;*****mk-query-type-map*****
(clojure.core/println "testing mk-query-type-map")
(let
  [query-no-info (new Query)
   query-man-only (new Query "pg" nil)
   query-comp-only (new Query nil "audio")
   query-man-comp (new Query "pg" "audio")]
  (do
    ;base case
    (clojure.core/println (= (mk-query-type-map {}) {}))
    ;recursive cases
    (clojure.core/println (= (mk-query-type-map {:query-space [query-no-info]}) {query-no-info :qsf0}))
    (clojure.core/println (= (mk-query-type-map {:query-space [query-man-only query-comp-only]}) 
                             {query-man-only :qsf1 query-comp-only :qsf1}))
    (clojure.core/println (= (mk-query-type-map {:query-space [query-no-info query-man-only query-comp-only query-man-comp]}) 
                             {query-man-only :qsf1 query-comp-only :qsf1 query-man-comp :qsf2 query-no-info :qsf0}))))

;*****min-reg-res*****
(clojure.core/println "testing min-reg-res")
;makes a 60-vector of QueryReports with varying zero and nonzero numbers for impressions
(defn make-test-queryreport-vec [k max-val]
  (let
    [test-query1 (new Query "lioneer" "tv")
     test-query2 (new Query "pg" "audio")
     gen-impressions (fn [i fun] (if (fun i) i 0))]
    (doto (new QueryReport)
      (.setImpressions test-query1 (gen-impressions k even?) 0)
      (.setImpressions test-query2 (gen-impressions (- max-val k) odd?) 0))))
;makes a 60-vector of maps relating sample queries to effective bid scores
(defn make-fake-squashed-bid-map [expected-min k max-val]
  ;here, expected min is just the minimal effective bid score for a particular agent
  ;we expect if we feed this to min-reg-res
  (let
    [test-query1 (new Query "lioneer" "tv")
     test-query2 (new Query "pg" "audio")]
    {test-query1 (double (+ expected-min k))
     test-query2 (double (+ expected-min (- max-val k)))}))

(let
  [test-query1 (new Query "lioneer" "tv")
   test-query2 (new Query "pg" "audio")
   query-vec [test-query1 test-query2]
   qr-vec1 (generate-test-vector make-test-queryreport-vec (vec (range 2 61)))
   qr-vec2 (generate-test-vector make-test-queryreport-vec (vec (range 1 60)))
   sb-vec1 (generate-test-vector (partial make-fake-squashed-bid-map 45.0) (vec (range 59)))
   sb-vec2 (generate-test-vector (partial make-fake-squashed-bid-map 30.0) (vec (range 59)))
   qr-map1 {"agent1" qr-vec1}
   qr-map2 {"agent1" qr-vec1 "agent2" qr-vec2}
   sb-map1 {"agent1" sb-vec1}
   sb-map2 {"agent1" sb-vec1 "agent2" sb-vec2}]
  (do
    ;base case
    (clojure.core/println (= (min-reg-res {} []) java.lang.Double/MAX_VALUE))
    (clojure.core/println (= (min-reg-res {:query-report qr-map1 :squashed-bids sb-map1 :agents ["agent1"]} query-vec)
                             45.0))
    (clojure.core/println (= (min-reg-res {:query-report qr-map2 :squashed-bids sb-map2 :agents ["agent1" "agent2"]} query-vec)
                             31.0)) ;don't get 30.0 here because we expect min-reg-res to skip the first index because it has no impressions
    ))

(clojure.core/println "testing mk-expected-search-pools")
(let
  [prod1 (new Product "pg" "audio")
   prod2 (new Product "lioneer" "tv")
   u-pops {prod1 [10 20 30 40 50 60]
           prod2 [60 50 40 30 20 10]}
   r-cat [prod1 prod2]
   p-query {prod1 [(new Query) (new Query nil "audio") (new Query "pg" nil) (new Query "pg" "audio")]
            prod2 [(new Query) (new Query nil "tv") (new Query "lioneer" nil) (new Query "lioneer" "tv")]}]
  (clojure.core/println (mk-expected-search-pools-map u-pops r-cat p-query)))