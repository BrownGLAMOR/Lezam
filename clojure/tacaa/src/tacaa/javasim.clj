(ns tacaa.javasim
  (:use (tacaa core))
  (:require (tacaa parser))
  (:import (java.util Random)
           (simulator.parser GameStatusHandler)
           (agents AbstractAgent)
           (agents.modelbased MCKP)
           (se.sics.tasim.aw Message)
           (edu.umich.eecs.tac.props BidBundle QueryReport SalesReport
                                     Query Product Ad UserClickModel
                                     AdvertiserInfo PublisherInfo
                                     SlotInfo QueryType RetailCatalog))
  (:gen-class
   :name tacaa.javasim
   :methods[^{:static true} [simulateAgent [clojure.lang.PersistentHashMap,
                                            agents.AbstractAgent,
                                            String] clojure.lang.PersistentArrayMap]
            ^{:static true} [setupClojureSim [String] clojure.lang.PersistentHashMap]
            ^{:static true} [initClojureSim [edu.umich.eecs.tac.props.PublisherInfo,
                                             edu.umich.eecs.tac.props.SlotInfo,
                                             edu.umich.eecs.tac.props.AdvertiserInfo,
                                             edu.umich.eecs.tac.props.RetailCatalog,
                                             java.util.ArrayList] clojure.lang.PersistentHashMap]
            ^{:static true} [mkPerfectFullStatus [clojure.lang.PersistentHashMap,
                                                  int,
                                                  String,
                                                  int] clojure.lang.PersistentHashMap]
            ^{:static true} [mkFullStatus [clojure.lang.PersistentHashMap,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map,
                                           java.util.Map] clojure.lang.PersistentHashMap]
            ^{:static true} [simQuery [clojure.lang.PersistentHashMap,
                                       edu.umich.eecs.tac.props.Query,
                                       String,
                                       int,
                                       double,
                                       double,
                                       edu.umich.eecs.tac.props.Ad,
                                       int,
                                       boolean] java.util.ArrayList]
            ^{:static true} [simDay [clojure.lang.PersistentHashMap,
                                     String,
                                     int,
                                     edu.umich.eecs.tac.props.BidBundle,
                                     int,
                                     boolean] java.util.ArrayList]
            ^{:static true} [simDayForReports [clojure.lang.PersistentHashMap,
                                               String,
                                               int,
                                               edu.umich.eecs.tac.props.BidBundle,
                                               int] java.util.ArrayList]
            ^{:static true} [setStartSales [clojure.lang.PersistentHashMap,
                                            String,
                                            int,
                                            int,
                                            boolean] clojure.lang.PersistentHashMap]
            ^{:static true} [getActualResults [clojure.lang.PersistentHashMap] clojure.lang.PersistentArrayMap]
            ^{:static true} [printResults [clojure.lang.PersistentArrayMap] void]
            ^{:static true} [compareResults [clojure.lang.PersistentArrayMap
                                             clojure.lang.PersistentArrayMap
                                             String] java.util.ArrayList]]))

(defn setupClojureSim
  [^String file]
  (init-sim-info (tacaa.parser/parse-file file)))


(defn -setupClojureSim
  [file]
  (setupClojureSim file))


(defn simulateAgent
  [^PersistentHashMap status
   ^AbstractAgent agent
   ^String agent-to-replace]
  (simulate-game-with-agent status agent agent-to-replace))

(defn -simulateAgent
  [status agent agent-to-replace]
  (simulateAgent status agent agent-to-replace))

(defn initClojureSim
  [^PublisherInfo pub-info,
   ^SlotInfo slot-info,
   ^AdvertiserInfo adv-info,
   ^RetailCatalog retail-cat,
   ^java.util.ArrayList agents]
  (let [agents (into [] agents)
        query-space (tacaa.parser/mk-queryspace retail-cat)]
    (hash-map :publish-info pub-info
              :slot-info slot-info
              :adv-info (reduce (fn [coll agent]
                                  (assoc coll agent adv-info))
                                {} agents)
              :retail-cat retail-cat
              :agents agents
              :query-space query-space
              :qsf0 (into #{} (filter (fn [^Query query] (= (.getType query)
                                                           QueryType/FOCUS_LEVEL_ZERO)) query-space))
              :qsf1 (into #{} (filter (fn [^Query query] (= (.getType query)
                                                           QueryType/FOCUS_LEVEL_ONE)) query-space))
              :qsf2 (into #{} (filter (fn [^Query query] (= (.getType query)
                                                           QueryType/FOCUS_LEVEL_TWO)) query-space))
              :squash-param (.getSquashingParameter pub-info)
              :prod-query (mk-prod-query-map retail-cat 0)
              :query-type (mk-query-type-map query-space 0)
              :conv-probs (mk-convpr-map)
              :man-bonus (.getManufacturerBonus adv-info)
              :comp-bonus (.getComponentBonus adv-info)
              :lam (.getDistributionCapacityDiscounter adv-info)
              :sales-window (.getDistributionWindow adv-info)
              :targ-effect (.getTargetEffect adv-info)
              :num-slots (.getRegularSlots slot-info)
              :prom-slots (.getPromotedSlots slot-info)
              :prom-bonus (.getPromotedSlotBonus slot-info)
              :USP 10)))

(defn -initClojureSim
  [pub-info
   slot-info
   adv-info
   retail-cat
   agents]
  (initClojureSim pub-info slot-info adv-info retail-cat agents))


(defn mkPerfectFullStatus
  [status
   day
   agent-to-replace
   start-sales]
  (reduce (fn [coll agent]
            (assoc coll :start-sales
                   (assoc (coll :start-sales) agent
                          (assoc ((coll :start-sales) agent)
                            day
                            (if (= agent-to-replace agent)
                              start-sales
                              ((status :capacities) agent))))))
          status (status :agents)))

(defn -mkPerfectFullStatus
  [status
   day
   agent-to-replace
   start-sales]
  (mkPerfectFullStatus status day agent-to-replace start-sales))


(defn mkFullStatus
  [status
   ^java.util.Map squashed-bids,
   ^java.util.Map budgets,
   ^java.util.Map user-pop,
   ^java.util.Map adv-effects,
   ^java.util.Map cont-probs,
   ^java.util.Map reg-res,
   ^java.util.Map prom-res,
   ^java.util.Map capacities,
   ^java.util.Map start-sales,
   ^java.util.Map man-specialties,
   ^java.util.Map comp-specialties,
   ^java.util.Map ads]
  (let [agents (status :agents)]
    (merge status
           {:squashed-bids (reduce (fn [coll ^String agent]
                                     (assoc coll agent
                                            [(into {} ^java.util.Map (.get squashed-bids agent))]))
                                   {} (keys squashed-bids)),
            :budgets (reduce (fn [coll ^String agent]
                               (assoc coll agent
                                      [(into {:total-budget Double/MAX_VALUE} ^java.util.Map (.get budgets agent))]))
                             {} (keys budgets)),
            :ads (reduce (fn [coll ^String agent]
                           (assoc coll agent
                                  [(into {} ^java.util.Map (.get ads agent))]))
                         {} (keys ads)),
            :adv-effects (reduce (fn [coll ^String agent]
                                   (assoc coll agent
                                          (into {} ^java.util.Map (.get adv-effects agent))))
                                 {} (keys adv-effects)),
            :cont-probs (into {} cont-probs),
            :user-pop [(into {} user-pop)],
            :reg-res {:qsf0 (.get reg-res (new Query nil nil)),
                      :qsf1 (.get reg-res (new Query "pg" nil)),
                      :qsf2 (.get reg-res (new Query "pg" "tv"))},
            :prom-res {:qsf0 (.get prom-res (new Query nil nil)),
                      :qsf1 (.get prom-res (new Query "pg" nil)),
                      :qsf2 (.get prom-res (new Query "pg" "tv"))},
            :capacities (reduce (fn [coll [agent val]] (assoc coll agent val)) {} capacities),
            :start-sales (reduce (fn [coll [agent val]] (assoc coll agent [val])) {} start-sales),
            :man-specialties (into {} man-specialties),
            :comp-specialties (into {} comp-specialties)})))

(defn -mkFullStatus
  [status squashed-bids budgets user-pop adv-effects
   cont-probs reg-res prom-res capacities start-sales
   man-specialties comp-specialties ads]
  (mkFullStatus status squashed-bids budgets user-pop adv-effects
                cont-probs reg-res prom-res capacities start-sales
                man-specialties comp-specialties ads))


(defn simQuery
  [status ^Query query agent day bid budget ^Ad ad num-ests perfectsim?]
  (let [squashed-bid (* bid
                        (Math/pow (((status :adv-effects) agent) query)
                                  (status :squash-param)))
        status (assoc status :squashed-bids
                      (assoc (status :squashed-bids) agent
                             (if perfectsim?
                               (assoc ((status :squashed-bids) agent) day
                                      (assoc (((status :squashed-bids) agent) day) query squashed-bid))
                               [{query squashed-bid}])))
        status (assoc status :budgets
                      (assoc (status :budgets) agent
                             (if perfectsim?
                               (assoc ((status :budgets) agent) day
                                      (assoc (assoc (((status :budgets) agent) day) query budget)
                                        :total-budget
                                        budget))
                               [{query budget,
                                 :total-budget budget}])))
        status (assoc status :ads
                      (assoc (status :ads) agent
                             (if perfectsim?
                               (assoc ((status :ads) agent) day
                                      (assoc (((status :ads) agent) day) query ad))
                               [{query ad}])))
                                        ;Pass in day 0 because all arrays are length 1
        aqstatv (loop [n (int 0)
                       aqstatsvec []]
                  (if (not (< n num-ests))
                    aqstatsvec
                    (recur (inc n)
                           (conj aqstatsvec
                                 (((simulate-query status (if perfectsim? day 0) query) agent) query)))))]
    (doto (new java.util.ArrayList)
      (.add (double (/ (reduce + (map (fn [aqstats] (aqstats :imps)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (aqstats :clicks)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (aqstats :cost)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :pos-dist) 0)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :pos-dist) 1)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :pos-dist) 2)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :pos-dist) 3)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :pos-dist) 4)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :is-ratios) 0)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :is-ratios) 1)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :is-ratios) 2)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :is-ratios) 3)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (nth (aqstats :is-ratios) 4)) aqstatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [aqstats] (aqstats :is-ratio)) aqstatv))
                       num-ests))))))

(defn -simQuery
  [status query agent day bid budget ad num-ests perfectsim?]
  (simQuery status query agent day bid budget ad num-ests perfectsim?))


(defn simDay
  [status,
   agent,
   day,
   ^BidBundle bundle,
   num-ests,
   perfectsim?]
  (let [queryspace (status :query-space)
        status (assoc status :squashed-bids
                      (assoc (status :squashed-bids) agent
                             (if perfectsim?
                               (assoc ((status :squashed-bids) agent) day
                                      (mk-single-squashed-bid-map bundle
                                                                  ((status :adv-effects) agent)
                                                                  (status :squash-param)
                                                                  queryspace))
                               [(mk-single-squashed-bid-map bundle
                                                                  ((status :adv-effects) agent)
                                                                  (status :squash-param)
                                                                  queryspace)])))
        status (assoc status :budgets
                      (assoc (status :budgets) agent
                             (if perfectsim?
                               (assoc ((status :budgets) agent) day
                                      (mk-single-budget-map bundle queryspace))
                               [(mk-single-budget-map bundle queryspace)])))
        status (assoc status :ads
                      (assoc (status :ads) agent
                             (if perfectsim?
                               (assoc ((status :ads) agent) day
                                      (mk-single-ad-map bundle queryspace))
                               [(mk-single-ad-map bundle queryspace)])))
                                        ;Pass in day 0 because all arrays are length 1
        astatv (loop [n (int 0)
                       astatsvec []]
                  (if (not (< n num-ests))
                    astatsvec
                    (recur (inc n)
                           (conj astatsvec
                                 ((simulate-expected-day status (if perfectsim? day 0)) agent)))))]
    (doto (new java.util.ArrayList)
      (.add (double (/ (reduce + (map (fn [astats] (astats :total-convs)) astatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [astats] (astats :total-convs)) astatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [astats] (astats :total-cost)) astatv))
                       num-ests)))
      (.add (double (/ (reduce + (map (fn [astats] (astats :total-convs)) astatv))
                       num-ests))))))


(defn -simDay
  [status agent day bundle num-ests perfectsim?]
  (simDay status agent day bundle num-ests perfectsim?))


(defn simDayForReports
  [status agent day bundle startsales]
  (let [queryspace (status :query-space)
        status (assoc status :squashed-bids
                      (assoc (status :squashed-bids) agent
                             (assoc ((status :squashed-bids) agent) day
                                    (mk-single-squashed-bid-map bundle
                                                                ((status :adv-effects) agent)
                                                                (status :squash-param)
                                                                queryspace))))
        status (assoc status :budgets
                      (assoc (status :budgets) agent
                             (assoc ((status :budgets) agent) day
                                    (mk-single-budget-map bundle queryspace))))
        status (assoc status :ads
                      (assoc (status :ads) agent
                             (assoc ((status :ads) agent) day
                                    (mk-single-ad-map bundle queryspace))))
        status (assoc status :start-sales
                      (assoc (status :start-sales) agent
                             (assoc ((status :start-sales) agent) day startsales)))
        stats (simulate-expected-day status day)
        queryreport (mk-query-report status stats agent day)
        salesreport (mk-sales-report status stats agent day)]
    (doto (new java.util.ArrayList)
      (.add queryreport)
      (.add salesreport))))

(defn -simDayForReports
  [status agent day bundle startsales]
  (simDayForReports status agent day bundle startsales))


(defn setStartSales
  [status agent day start-sales perfectsim?]
  (assoc status :start-sales
         (assoc (status :start-sales) agent
                (if perfectsim?
                  (assoc ((status :start-sales) agent) day start-sales)
                  [start-sales]))))


(defn -setStartSales
  [status agent day start-sales perfectsim?]
  (setStartSales status agent day start-sales perfectsim?))


(defn getActualResults
  [status]
  (game-full-summary status))

(defn -getActualResults
  [status]
  (getActualResults status))

(defn printResults
  [results]
  (do
    (doall
     (map (fn [[agent resultmap]]
            (prn agent ": "
                 (assoc resultmap :profit
                        (- (resultmap :revenue)
                           (resultmap :cost)))))
          (sort (fn [key1 key2]
                  (compare
                   (let [statmap (peek key2)
                         profit (- (statmap :revenue)
                                   (statmap :cost))]
                     profit)
                   (let [statmap (peek key1)
                         profit (- (statmap :revenue)
                                   (statmap :cost))]
                     profit))) results)))
    nil))

(defn -printResults
  [results]
  (printResults results))


(defn compareResults
  [results
   actual
   agent]
  (let [act-prof (double (- ((actual agent) :revenue)
                            ((actual agent) :cost)))
        res-prof (double (- ((results agent) :revenue)
                            ((results agent) :cost)))]
    (doto (new java.util.ArrayList)
      (.add (double act-prof))
      (.add (double res-prof)))))

(defn -compareResults
  [results
   actual
   agent]
  (compareResults results actual agent))


;(use 'criterium.core)


                                        ;TODO
                                        ; add sampling to simulation

;(def file1 "/Users/jordanberg/Desktop/tacaa2010/game-tacaa1-15128.slg")
;(def status1 (init-sim-info (tacaa.parser/parse-file file1)))


(comment (defmacro nassoc
   ([m k v]
      `(assoc ~m ~k ~v))
   ([m k1 k2 v]
      `(let [m# (~m ~k1)]
         (assoc ~m ~k1
                (assoc m# ~k2 ~v))))
   ([m k1 k2 k3 v]
      `(let [m1# (~m ~k1)
             m2# (m1# ~k2)]
         (assoc ~m ~k1
                (assoc m1# ~k2
                       (assoc m2# ~k3 ~v)))))
   ([m k1 k2 k3 k4 v]
      `(let [m1# (~m ~k1)
             m2# (m1# ~k2)
             m3# (m2# ~k3)]
         (assoc ~m ~k1
                (assoc m1# ~k2
                       (assoc m2# ~k3
                              (assoc m3# ~k4 ~v))))))
   ([m k1 k2 k3 k4 k5 & more]
      `(let [m1# (~m ~k1)
             m2# (m1# ~k2)
             m3# (m2# ~k3)
             m4# (m3# ~k4)]
         (assoc ~m ~k1
                (assoc m1# ~k2
                       (assoc m2# ~k3
                              (assoc m3# ~k4
                                     (nassoc m4# ~k5 ~@more)))))))))

(comment (defmacro nassoc
   [m k & more]
   `(nassoc1 ~m ~k ~@more))

 (defmacro nassoc1
   ([m k v]
      `(assoc ~m ~k ~v))
   ([m k1 k2 v]
      `(let [m# (~m ~k1)]
         (assoc ~m ~k1
                (assoc m# ~k2 ~v))))
   ([m k1 k2 k3 & more]
      `(let [m1# (~m ~k1)
             m2# (m1# ~k2)]
         (assoc ~m ~k1
                (assoc m1# ~k2
                       (nassoc2 m2# ~k3 ~@more))))))

 (defmacro nassoc2
   ([m k v]
      `(assoc ~m ~k ~v))
   ([m k1 k2 v]
      `(let [m# (~m ~k1)]
         (assoc ~m ~k1
                (assoc m# ~k2 ~v))))
   ([m k1 k2 k3 & more]
      `(let [m1# (~m ~k1)
             m2# (m1# ~k2)]
         (assoc ~m ~k1
                (assoc m1# ~k2
                       (nassoc1 m2# ~k3 ~@more))))))
)
