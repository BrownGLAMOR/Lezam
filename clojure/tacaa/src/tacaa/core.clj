(ns tacaa.core
  (:require (tacaa parser)
            (criterium core stats well))
  (:import (java.util Random)
           (simulator.parser GameStatusHandler)
           (agents AbstractAgent)
           (agents.modelbased MCKP)
           (agents.rulebased2010 EquatePPSSimple2010 EquateROISimple2010)
           (se.sics.tasim.aw Message)
           (edu.umich.eecs.tac.props BidBundle QueryReport SalesReport
                                     Query Product Ad UserClickModel
                                     AdvertiserInfo PublisherInfo
                                     SlotInfo QueryType RetailCatalog)))

(set! *warn-on-reflection* true)


(defn calc-mean
  [lst]
  (/ (reduce + lst) (count lst)))


(defn calc-std-dev
  ([lst] (calc-std-dev lst (calc-mean lst)))
  ([lst mean]
     (let [diffs (map (fn [x] ((fn [y] (* y y)) (- mean x))) lst)
           sumdiff (reduce + diffs)]
       (Math/sqrt (/ sumdiff (dec (count lst)))))))

(defn calc-mean-and-std
  [lst]
  (let [mean (calc-mean lst)]
    [mean (calc-std-dev lst mean)]))


;Borrowed from clojure core, added new param (rnd)
(defn my-shuffle
  "Return a random permutation of coll"
  ([^java.util.Collection coll]
     (let [al (java.util.ArrayList. coll)]
       (java.util.Collections/shuffle al)
       (clojure.lang.RT/vector (.toArray al))))
  ([^java.util.Collection coll ^java.util.Random rnd]
     (let [al (java.util.ArrayList. coll)]
       (java.util.Collections/shuffle al rnd)
       (clojure.lang.RT/vector (.toArray al)))))


(defn find-min
  [lst]
  (loop [min Double/MAX_VALUE lst lst]
    (if (not (seq lst))
      min
      (recur
       (if (< (first lst) min)
         (double (first lst))
         min)
       (rest lst)))))

(defn find-max
  [lst]
  (loop [max (- Double/MAX_VALUE) lst lst]
    (if (not (seq lst))
      max
      (recur
       (if (> (first lst) max)
         (double (first lst))
         max)
       (rest lst)))))

(defn mk-convpr-map
  []
  {:qsf0 0.11,
   :qsf1 0.23,
   :qsf2 0.36})

(defn mk-adv-effect-map
  [status]
  (let [^UserClickModel user-click (status :user-click)
        agents (status :agents)
        queryspace (status :query-space)]
    (reduce (fn [coll val]
              (assoc coll (agents val)
                     (reduce (fn [coll2 query]
                               (assoc coll2 query
                                      (.getAdvertiserEffect user-click
                                                            (.queryIndex user-click query)
                                                            val)))
                             {} queryspace)))
            {} (range (count agents)))))

(defn mk-cont-prob-map
  [status]
  (let [^UserClickModel user-click (status :user-click)
        agents (status :agents)
        queryspace (status :query-space)]
    (reduce (fn [coll query]
              (assoc coll query (.getContinuationProbability user-click
                                                           (.queryIndex user-click query))))
            {} queryspace)))

(defn mk-man-spec-map
  [status]
  (let [adv-info (status :adv-info)
        agents (status :agents)
        queryspace (status :query-space)]
    (reduce (fn [coll val]
              (assoc coll val (.getManufacturerSpecialty ^AdvertiserInfo (adv-info val))))
            {} agents)))

(defn mk-comp-spec-map
  [status]
  (let [adv-info (status :adv-info)
        agents (status :agents)
        queryspace (status :query-space)]
    (reduce (fn [coll val]
              (assoc coll val (.getComponentSpecialty ^AdvertiserInfo (adv-info val))))
            {} agents)))

(defn mk-capacity-map
  [status]
  (let [adv-info (status :adv-info)
        agents (status :agents)
        queryspace (status :query-space)]
    (reduce (fn [coll val]
              (assoc coll val (.getDistributionCapacity ^AdvertiserInfo (adv-info val))))
            {} agents)))

(defn mk-single-budget-map
  [^BidBundle bundle queryspace]
  (let [tot-budget (.getCampaignDailySpendLimit bundle)]
    (reduce (fn [coll ^Query query]
              (assoc coll query  ; budget cannot be less than tot-budget
                     (Math/min (.getDailyLimit bundle query) tot-budget)))
            {:total-budget tot-budget}
            queryspace)))

(defn mk-budget-map
  [status]
  (let [bid-bundle (status :bid-bundle)
        agents (status :agents)
        queryspace (status :query-space)]
    (reduce (fn [coll agent]
              (assoc coll agent
                     (let [bundles (bid-bundle agent)]
                       (reduce (fn [coll2 day]
                                 (let [bundle (bundles day)]
                                   (conj coll2
                                         (mk-single-budget-map bundle queryspace))))
                               [] (range 59)))))
            {} agents)))

(defn mk-single-ad-map
  [^BidBundle bundle queryspace]
  (reduce (fn [coll ^Query query]
            (assoc coll query
                   (.getAd bundle query)))
          {} queryspace))

(defn mk-ad-map
  [status]
  (let [bid-bundle (status :bid-bundle)
        agents (status :agents)
        queryspace (status :query-space)]
    (reduce (fn [coll agent]
              (assoc coll agent
                     (let [bundles (bid-bundle agent)]
                       (reduce (fn [coll2 day]
                                 (let [bundle (bundles day)]
                                   (conj coll2
                                         (mk-single-ad-map bundle queryspace))))
                               [] (range 59)))))
            {} agents)))


(defn mk-single-squashed-bid-map
  [^BidBundle bundle adv-effect squash-param queryspace]
  (reduce (fn [coll ^Query query]
            (assoc coll query
                   (* (.getBid bundle query)
                      (Math/pow (adv-effect query)
                                squash-param))))
          {} queryspace))

(defn mk-squashed-bid-map
  [status]
  (let [bid-bundle (status :bid-bundle)
        adv-effects (status :adv-effects)
        agents (status :agents)
        queryspace (status :query-space)
        squash-param (double (status :squash-param))]
    (reduce (fn [coll agent]
              (assoc coll agent
                     (let [bundles (bid-bundle agent)
                           adv-effect ((status :adv-effects) agent)]
                       (reduce (fn [coll2 day]
                                 (conj coll2
                                       (mk-single-squashed-bid-map
                                        (bundles day)
                                        adv-effect
                                        squash-param
                                        queryspace)))
                               [] (range 59)))))
            {} agents)))


(defn mk-prod-query-map
  ([status] (mk-prod-query-map (status :retail-cat) 0))
  ([retail-cat dummy]
     (reduce (fn [coll ^Product prod]
               (let [man (.getManufacturer prod)
                     comp (.getComponent prod)]
                 (assoc coll prod [(new Query)
                                   (new Query nil comp)
                                   (new Query man nil)
                                   (new Query man comp)])))
             {} (seq retail-cat))))

(defn mk-query-type-map
  ([status] (mk-query-type-map (status :query-space) 0))
  ([query-space dummy]
     (reduce (fn [coll ^Query query]
               (assoc coll query
                      (let [qtype (.getType query)]
                        (if (= qtype QueryType/FOCUS_LEVEL_ZERO)
                          :qsf0
                          (if (= qtype QueryType/FOCUS_LEVEL_ONE)
                            :qsf1
                            :qsf2)))))
             {} query-space)))

(defn min-reg-res
  [status queryspace]
  (let [query-reports (status :query-report)
        squashed-bids (status :squashed-bids)
        agents (status :agents)]
    (find-min
     (map (fn [agent]
            (let [query-report (query-reports agent)
                  squashed-bid (squashed-bids agent)]
              (find-min
               (map (fn [day]
                      (let [^QueryReport qr (query-report day)
                            sb (squashed-bid day)]
                        (find-min
                         (filter identity
                                 (map (fn [^Query query] (if (> (.getImpressions qr query) 0)
                                                   (sb query)
                                                   nil))
                                      queryspace)))))
                    (range 59)))))
          agents))))

(defn min-prom-res
  [status queryspace]
  (let [query-reports (status :query-report)
        squashed-bids (status :squashed-bids)
        agents (status :agents)]
    (find-min
     (map (fn [agent]
            (let [query-report (query-reports agent)
                  squashed-bid (squashed-bids agent)]
              (find-min
               (map (fn [day]
                      (let [^QueryReport qr (query-report day)
                            sb (squashed-bid day)]
                        (find-min
                         (filter identity
                                 (map (fn [^Query query]
                                        (if (> (.getPromotedImpressions qr query) 0)
                                          (sb query)
                                          nil))
                                      queryspace)))))
                    (range 59)))))
          agents))))

(defn partialsums
  [start lst]
  (lazy-seq
    (if-let [lst (seq lst)] 
          (cons start (partialsums (+ start (first lst)) (rest lst)))
          (list start))))

(defn moving-sum
  [window lst]
  (let [start (reduce + (take window lst))
        diffseq (map - (drop window lst) lst)]
    (partialsums start diffseq)))

(defn mk-start-sales-map
  [status]
  (let [agents (status :agents)
        queryspace (status :query-space)
        sales-report (status :sales-report)
        capacities (status :capacities)
        sales-window (status :sales-window)]
    (reduce (fn [coll agent]
              (assoc coll agent
                     (let [sales-report (sales-report agent)
                           capacity (capacities agent)]
                       (vec (moving-sum (dec sales-window)
                                        (reduce (fn [coll2 day]
                                                  (let [^SalesReport sr (sales-report day)]
                                                    (conj coll2
                                                          (reduce + (map (fn [^Query query]
                                                                           (.getConversions sr query))
                                                                         queryspace)))))
                                                (vec (replicate (dec sales-window) (/ capacity sales-window)))
                                                (range 59)))))))
            {} agents)))

(defn mk-random-search-pools-map
  ([status day random]
     (mk-random-search-pools-map
      ((status :user-pop) day)
      (status :retail-cat)
      (status :prod-query)
      random))
  ([userpops retail-cat prod-query ^java.util.Random random]
     (loop [retail-cat (seq retail-cat) search-maps {}]
       (if (not (seq retail-cat))
         search-maps
         (let [prod (first retail-cat)
               [uns uis uf0 uf1 uf2 ut] (userpops prod)
               umap {:is uis, :f0 uf0, :f1 uf1, :f2 uf2}
               [f0q f1cq f1mq f2q] (prod-query prod)]
           (recur (rest retail-cat)
                  (loop [umap umap search-map {}]
                    (if (not (seq umap))
                      (assoc search-maps prod search-map)
                      (let [[utype users] (first umap)]
                        (recur (rest umap)
                               (loop [users users search-map search-map]
                                 (if (not (> users 0))
                                   search-map
                                   (recur (dec users)
                                          (cond
                                           (= utype :is) (let [rnum (.nextDouble random)
                                                               q (if (< rnum (/ 1 3))
                                                                   f0q
                                                                   (if (< rnum (/ 2 3))
                                                                     f2q
                                                                     (if (< (.nextDouble random) 0.5)
                                                                       f1cq
                                                                       f1mq)))]
                                                           (assoc search-map q
                                                                  (if (search-map q)
                                                                    (let [coll (search-map q)]
                                                                      [(inc (first coll))
                                                                       (peek coll)])
                                                                    [1 0])))
                                           (= utype :f0) (assoc search-map f0q
                                                                (if (search-map f0q)
                                                                  (let [coll (search-map f0q)]
                                                                    [(first coll)
                                                                     (inc (peek coll))])
                                                                  [0 1]))
                                           (= utype :f1) (let [q (if (< (.nextDouble random) 0.5)
                                                                   f1cq
                                                                   f1mq)]
                                                           (assoc search-map q
                                                                  (if (search-map q)
                                                                    (let [coll (search-map q)]
                                                                      [(first coll)
                                                                       (inc (peek coll))])
                                                                    [0 1])))
                                           (= utype :f2) (assoc search-map f2q
                                                                (if (search-map f2q)
                                                                  (let [coll (search-map f2q)]
                                                                    [(first coll)
                                                                     (inc (peek coll))])
                                                                  [0 1]))
                                           :else search-map))))))))))))))

(defn mk-expected-search-pools-map
  ([status day]
     (mk-expected-search-pools-map
      ((status :user-pop) day)
      (seq (status :retail-cat))
      (status :prod-query)))
  ([userpops retail-cat prod-query]
     (reduce (fn [coll prod]
               (assoc coll prod
                      (let [[uns uis uf0 uf1 uf2 ut] (userpops prod)
                            [f0q f1cq f1mq f2q] (prod-query prod)
                            isf0 (if (> (rem uis 3) 0)
                                   (int (+ (/ uis 3) 1))
                                   (int (/ uis 3)))
                            isf1 (if (== (rem uis 3) 2)
                                   (int (+ (/ uis 3) 1))
                                   (int (/ uis 3)))
                            isf1c (int (/ isf1 2))
                            isf1m (int (+ (/ isf1 2) 0.5))
                            isf2 (int (/ uis 3))
                            uf1c (int (/ uf1 2))
                            uf1m (int (+ (/ uf1 2) 0.5))
                            uf0 (if (coll f0q)
                                  [(+ (first (coll f0q)) isf0) (+ (peek (coll f0q)) uf0)]
                                  [isf0 uf0])
                            uf1c (if (coll f1cq)
                                   [(+ (first (coll f1cq)) isf1c) (+ (peek (coll f1cq)) uf1c)]
                                   [isf1c uf1c])
                            uf1m (if (coll f1mq)
                                   [(+ (first (coll f1mq)) isf1m) (+ (peek (coll f1mq)) uf1m)]
                                   [isf1m uf1m])
                            uf2 (if (coll f2q)
                                  [(+ (first (coll f2q)) isf2) (+ (peek (coll f2q)) uf2)]
                                  [isf2 uf2])]
                        {f0q uf0,
                         f1cq uf1c,
                         f1mq uf1m,
                         f2q uf2})))
             {} retail-cat)))


(defn mk-search-queue
  [search-maps random singleq?]
  (if (not singleq?)
    (my-shuffle
     (flatten (map (fn [[prod search-map]]
                     (map (fn [[q [uis us]]]
                            [(replicate uis {:is q :prod prod})
                             (replicate us {:s q :prod prod})])
                          search-map))
                   search-maps))
     random)
    (my-shuffle
     (flatten (map (fn [[prod search-map]]
                     (map (fn [[q [uis us]]]
                            (if (= q singleq?)
                              [(replicate uis {:is q :prod prod})
                               (replicate us {:s q :prod prod})]
                              []))
                          search-map))
                   search-maps))
     random)))


(defn init-sim-info
  ([status]
     (let [agents (status :agents)
           ^AdvertiserInfo adv-info ((status :adv-info) (agents 0))
           ^SlotInfo slot-info (status :slot-info)
           status (merge status
                         {:squash-param (.getSquashingParameter ^PublisherInfo (status :publish-info)),
                          :adv-effects (mk-adv-effect-map status)})
           status (merge status
                         {:prod-query (mk-prod-query-map status),
                          :query-type (mk-query-type-map status),
                          :cont-probs (mk-cont-prob-map status),
                          :conv-probs (mk-convpr-map),
                          :man-specialties (mk-man-spec-map status),
                          :comp-specialties (mk-comp-spec-map status),
                          :man-bonus (.getManufacturerBonus adv-info),
                          :comp-bonus (.getComponentBonus adv-info),
                          :capacities (mk-capacity-map status),
                          :lam (.getDistributionCapacityDiscounter adv-info),
                          :sales-window (.getDistributionWindow adv-info),
                          :targ-effect (.getTargetEffect adv-info),
                          :squashed-bids (mk-squashed-bid-map status),
                          :budgets (mk-budget-map status),
                          :ads (mk-ad-map status),
                          :num-slots (.getRegularSlots slot-info),
                          :prom-slots (.getPromotedSlots slot-info),
                          :prom-bonus (.getPromotedSlotBonus slot-info),
                          :USP 10})]
       (merge status {:reg-res (zipmap [:qsf0 :qsf1 :qsf2]
                                       (map (partial min-reg-res status)
                                            [(status :qsf0) (status :qsf1) (status :qsf2)])),
                      :prom-res (zipmap [:qsf0 :qsf1 :qsf2]
                                        (map (partial min-prom-res status)
                                             [(status :qsf0) (status :qsf1) (status :qsf2)])),
                      :start-sales (mk-start-sales-map status)}))))


(defn asgn-rank-meta
  [qagents q prom-slots reg-res prom-res squash-param]
  (loop [newqagents []
         qagents qagents
         slot 1]
    (if (not (seq qagents))
      newqagents
      (let [qagent (first qagents)
            agent (first qagent)
            bid (peek qagent)
            is-prom (and (<= slot prom-slots) (>= bid prom-res))
            res (if is-prom prom-res reg-res)
            restqagents (seq (rest qagents))
            nextbid (if restqagents
                      (peek (first restqagents))
                      0)
            nextbid (if (< nextbid res)
                      res
                      nextbid)
            adveffect (nth qagent 1)
            cpc (/ nextbid (Math/pow adveffect
                                     squash-param))
            metadata {:cpc cpc
                      :is-prom is-prom
                      :pos slot}]
        (recur
         (conj newqagents (with-meta qagent metadata))
         restqagents
         (inc slot))))))


(defn rank-agents-query
  [qagents reg-res]
  (sort (fn [key1 key2] (compare (peek key2) (peek key1)))
        (filter (fn [key] (and (>= (peek key) reg-res) ;bid >= reg-res
                              (<= (peek key) (nth key 3)))) ; bid <= budget
                qagents)))

(defn rank-agents
  ([status day singleq?]
     (rank-agents day
                  singleq?
                  (status :squashed-bids)
                  (status :budgets)
                  (status :ads)
                  (status :adv-effects)
                  (status :prom-slots)
                  (status :reg-res)
                  (status :prom-res)
                  (status :squash-param)
                  (status :agents)
                  (status :query-space)
                  (status :query-type)))
  ([day singleq? squashed-bids budgets ads adv-effects prom-slots reg-res prom-res squash-param agents queryspace query-type]
     (if (not singleq?)
       (reduce (fn [coll query]
                 (assoc coll query
                        (asgn-rank-meta
                         (rank-agents-query
                          (map (fn [agent] [agent
                                           ((adv-effects agent) query)
                                           (((ads agent) day) query)
                                           (((budgets agent) day) query)
                                           (((squashed-bids agent) day) query)])
                               agents)
                          (reg-res (query-type query)))
                         query
                         prom-slots
                         (reg-res (query-type query))
                         (prom-res (query-type query))
                         squash-param)))
               {} queryspace)
       {singleq? (asgn-rank-meta
                  (rank-agents-query
                   (map (fn [agent] [agent
                                    ((adv-effects agent) singleq?)
                                    (((ads agent) day) singleq?)
                                    (((budgets agent) day) singleq?)
                                    (((squashed-bids agent) day) singleq?)])
                        agents)
                   (reg-res (query-type singleq?)))
                  singleq?
                  prom-slots
                  (reg-res (query-type singleq?))
                  (prom-res (query-type singleq?))
                  squash-param)})))


(defn remove-from-query
  [to-remove qagents prom-slots reg-res prom-res squash-param q query-type]
  (let [idx (loop [qagents qagents
                   idx 0]
              (if (not (seq qagents))
                idx
                (let [qagent (first qagents)
                      agent (first qagent)]
                  (if (= to-remove agent)
                    idx
                    (recur (rest qagents) (inc idx))))))
        qagents (if (== idx (count qagents))
                  qagents
                  (if (== idx 0)
                    (rest qagents)
                    (if (== idx (dec (count qagents)))
                      (pop qagents)
                      (vec (concat (subvec qagents 0 idx)
                                   (subvec qagents (+ 1 idx) (count qagents)))))))]
    (asgn-rank-meta qagents
                    q
                    prom-slots
                    (reg-res (query-type q))
                    (prom-res (query-type q))
                    squash-param)))


(defn re-rank-query
  [ranked-agents stats budgets prom-slots reg-res prom-res squash-param q day query-type]
  (let [qagents (ranked-agents q)
        to-remove (filter (fn [qagent]
                            (let [[agent
                                   adv-effect
                                   ad
                                   budget
                                   sbid] qagent
                                   bid (/ sbid (Math/pow adv-effect squash-param))
                                   cost (((stats agent) q) :cost)]
                              (if (< (+ cost bid)
                                     budget)
                                false
                                true)))
                          qagents)
        to-remove (map (fn [qagent] (first qagent)) to-remove)]
    (if (not (seq to-remove))
      ranked-agents
      (loop [qagents qagents
             to-remove to-remove]
        (if (not (seq to-remove))
          (assoc ranked-agents q qagents)
          (let [agent-to-remove (first to-remove)
                qagents (remove-from-query agent-to-remove
                                           qagents
                                                 prom-slots
                                                 reg-res
                                                 prom-res
                                                 squash-param
                                                 q
                                                 query-type)]
            (recur qagents (rest to-remove))))))))


(defn remove-from-all-auctions
  [to-remove ranked-agents prom-slots reg-res prom-res squash-param queryspace query-type]
  (reduce (fn [coll q]
            (let [qagents (ranked-agents q)
                  qagents (remove-from-query to-remove
                                             qagents
                                             prom-slots
                                             reg-res
                                             prom-res
                                             squash-param
                                             q
                                             query-type)]
              (assoc coll q qagents)))
          {} queryspace))

(defn check-total-budgets
  [ranked-agents queryspace stats budgets prom-slots reg-res prom-res squash-param q day query-type]
  (let [qagents (ranked-agents q)
        to-remove (filter (fn [qagent]
                            (let [[agent
                                   adv-effect
                                   ad
                                   budget
                                   sbid] qagent
                                   bid (/ sbid (Math/pow adv-effect squash-param))
                                   ourbudgets ((budgets agent) day)
                                   total-budget (ourbudgets :total-budget)
                                   total-cost ((stats agent) :total-cost)]
                              (if (< (+ total-cost bid)
                                     total-budget)
                                false
                                true)))
                          qagents)
        to-remove (map (fn [qagent] (first qagent)) to-remove)]
    (if (not (seq to-remove))
      ranked-agents
      (loop [ranked-agents ranked-agents
             to-remove to-remove]
        (if (not (seq to-remove))
          ranked-agents
          (let [agent-to-remove (first to-remove)
                ranked-agents (remove-from-all-auctions agent-to-remove
                                                        ranked-agents
                                                        prom-slots
                                                        reg-res
                                                        prom-res
                                                        squash-param
                                                        queryspace
                                                        query-type)]
            (recur ranked-agents (rest to-remove))))))))


(defn re-rank-agents
  [ranked-agents singleq? queryspace stats budgets prom-slots reg-res prom-res squash-param q day query-type]
  (let [re-ranked-agents (re-rank-query ranked-agents
                                     stats
                                     budgets
                                     prom-slots
                                     reg-res
                                     prom-res
                                     squash-param
                                     q
                                     day
                                     query-type)
        re-ranked-agents (if (and (== (count ranked-agents)
                                      (count re-ranked-agents))
                                  (not singleq?))
                                        ;check total budgets if we didn't
                                        ;already remove query and we are
                                        ; evaluating more than 1 query
                           (check-total-budgets re-ranked-agents
                                                queryspace
                                                stats
                                                budgets
                                                prom-slots
                                                reg-res
                                                prom-res
                                                squash-param
                                                q
                                                day
                                                query-type)
                           re-ranked-agents)]
    re-ranked-agents))

(defn eta
  [p x]
  (/ (* p x)
     (+ (* p x)
        (- 1 p))))

(defn sample-without-rep
  [num-samp max-val ^java.util.Random random]
  (if (> max-val num-samp)
    (loop [samps []]
      (if (== (count samps) num-samp)
        samps
        (recur (let [samp (.nextInt random (int max-val))]
                 (if (some #{samp} samps)
                   samps
                   (conj samps samp))))))
    (vec (range max-val))))

(defn sample-avg-pos
  [stats queryspace random]
  (reduce (fn [coll query]
            (let [lenlst (map (fn [[agent astats]]
                                (count ((astats query) :pos-vec)))
                              coll)
                  max-imps (find-max lenlst)
                  samps (sample-without-rep 10 max-imps random)]
              (loop [agents (keys coll)
                     allstats coll]
                (if (not (seq agents))
                  allstats
                  (let [agent (first agents)
                        astats (allstats agent)]
                    (recur (rest agents)
                           (assoc allstats agent
                                  (assoc astats query
                                         (let [aqstats (astats query)
                                               pos-vec (aqstats :pos-vec)
                                               pos-vec-len (count pos-vec)
                                               imps (int (aqstats :imps))
                                               possum (double (aqstats :pos-sum))]
                                           {:imps imps,
                                            :prom-imps (aqstats :prom-imps),
                                            :pos-sum possum,
                                            :avg-pos (if (== imps 0)
                                                       Double/NaN
                                                       (/ possum
                                                          imps)),
                                            :savg-pos (if (or (== imps 0)
                                                              (== pos-vec-len 0))
                                                        Double/NaN
                                                        (loop [allsamps samps
                                                              pos-sum (double 0)
                                                              num-imps (int 0)]
                                                         (if (not (seq allsamps))
                                                           (if (== num-imps 0)
                                                             Double/NaN
                                                             (/ pos-sum num-imps))
                                                           (let [samp (first allsamps)]
                                                             (if (< samp pos-vec-len)
                                                               (let [pos (int (pos-vec samp))]
                                                                 (if (> pos 0)
                                                                   (recur (rest allsamps)
                                                                          (+ pos-sum
                                                                             pos)
                                                                          (inc num-imps))
                                                                   (recur (rest allsamps)
                                                                          pos-sum
                                                                          num-imps)))
                                                               (recur (rest allsamps)
                                                                      pos-sum
                                                                      num-imps)))))),
                                            :clicks (aqstats :clicks),
                                            :cost (aqstats :cost),
                                            :convs (aqstats :convs),
                                            :revenue (aqstats :revenue)})))))))))
          stats queryspace))

(defn simulate-day
  ([status day search-queue ^java.util.Random random singleq?]
     (let [agents (status :agents)
           queryspace (status :query-space)
           query-type (status :query-type)
           bids (status :squashed-bids)
           budgets (status :budgets)
           ads (status :ads)
           conv-probs (status :conv-probs)
           start-sales (status :start-sales)
           cont-probs (status :cont-probs)
           adv-effects (status :adv-effects)
           man-specs (status :man-specialties)
           comp-specs (status :comp-specialties)
           capacities (status :capacities)
           lam (status :lam)
           sales-window (status :sales-window)
           num-slots (status :num-slots)
           prom-slots (status :prom-slots)
           reg-res (status :reg-res)
           prom-res (status :prom-res)
           squash-param (double (status :squash-param))
           targ-effect (status :targ-effect)
           prom-bonus (status :prom-bonus)
           comp-bonus (status :comp-bonus)
           man-bonus (status :man-bonus)
           USP (status :USP)
           ;;;;;;;;;;;;;;;;;;
           ranked-agents (rank-agents day singleq? bids budgets ads adv-effects prom-slots
                                      reg-res prom-res squash-param agents queryspace query-type)]
       (loop [search-queue search-queue
              ranked-agents ranked-agents
              stats (reduce (fn [coll agent]
                              (assoc! coll agent
                                     (reduce (fn [coll2 query]
                                               (assoc! coll2 query
                                                       {:imps 0,
                                                        :prom-imps 0,
                                                        :pos-sum 0,
                                                        :pos-vec [],
                                                        :clicks 0,
                                                        :cost 0,
                                                        :convs 0,
                                                        :revenue 0}))
                                             (transient {:total-cost 0,
                                                         :total-revenue 0,
                                                         :total-convs 0}) queryspace)))
                            (transient {}) agents)]
         (if (not (seq search-queue))
           (sample-avg-pos
            (reduce (fn [coll [agent astats]]
                      (assoc coll agent (persistent! astats)))
                    {} (persistent! stats))
            queryspace
            random)
           (let [smap (first search-queue)
                 isis (contains? smap :is)
                 q (if isis
                     (smap :is)
                     (smap :s))
                 ^Product prod (smap :prod)                 
                 ranked-agents (re-rank-agents ranked-agents singleq? queryspace stats
                                               budgets prom-slots reg-res
                                               prom-res squash-param q day
                                               query-type)                 
                 qagents (ranked-agents q)
                 stats (loop [qagents qagents
                              stats stats
                              cont? true]
                         (if (not (seq qagents))
                           stats
                           (let [qagent (first qagents)
                                 metadata (meta qagent)
                                 pos (metadata :pos)]
                             (if (> pos num-slots)
                                        ;out of auction, but add to pos-vec for sampling
                               (let [agent (first qagent)
                                     astats (stats agent)
                                     aqstats (astats q)
                                     aqstats {:imps (aqstats :imps),
                                              :prom-imps (aqstats :prom-imps),
                                              :pos-sum (aqstats :pos-sum),
                                              :pos-vec (conj (aqstats :pos-vec) -1),
                                              :clicks (aqstats :clicks),
                                              :cost (aqstats :cost),
                                              :convs (aqstats :convs),
                                              :revenue (aqstats :revenue)}
                                     astats (assoc! astats q aqstats)
                                     stats (assoc! stats agent astats)]
                                 (recur (rest qagents) stats cont?))
                               (if cont?
                                 (let [[agent ;still in auction
                                        adv-effect
                                        ^Ad ad
                                        budget
                                        bid] qagent
                                        is-prom (metadata :is-prom)
                                        cpc (metadata :cpc)
                                        astats (stats agent)
                                        ftarg (if (and ad (not (.isGeneric ad)))
                                                (if (= prod (.getProduct ad))
                                                  (+ 1 targ-effect)
                                                  (/ 1 (+ 1 targ-effect)))
                                                1)
                                        fprom (if is-prom
                                                (+ 1 prom-bonus)
                                                1)
                                        ftargprom (* ftarg fprom)
                                        clickpr (if (> ftargprom 1)
                                                  (eta adv-effect ftargprom)
                                                  adv-effect)
                                        click? (< (.nextDouble random) clickpr)
                                        conv? (if (and click? (not isis))
                                                (let [convpr (conv-probs (query-type q))
                                                      cap (capacities agent)
                                                      comp-spec (comp-specs agent)
                                                      convs (astats :total-convs)
                                                      capstart ((start-sales agent) day)
                                                      totconv (+ convs capstart)
                                                      convpr (if (> totconv cap)
                                                               (* convpr
                                                                  (Math/pow lam (- totconv
                                                                                   cap)))
                                                               convpr)
                                                      convpr (if (= comp-spec
                                                                    (.getComponent prod))
                                                               (eta convpr (+ 1 comp-bonus))
                                                               convpr)]
                                                  (< (.nextDouble random) convpr))
                                                false)
                                        USP (if conv?
                                              (if (= (man-specs agent)
                                                    (.getManufacturer prod))
                                                (* USP (+ 1 man-bonus))
                                                USP)
                                             USP)
                                        cont? (if (not conv?)
                                                (< (.nextDouble random) (cont-probs q))
                                                false)
                                        astats (if conv?
                                                 (assoc!
                                                  (assoc!
                                                   (assoc! astats :total-convs (inc (astats :total-convs)))
                                                   :total-revenue (+ (astats :total-revenue) USP))
                                                  :total-cost (+ (astats :total-cost) cpc))
                                                 (if click?
                                                   (assoc! astats :total-cost (+ (astats :total-cost) cpc))
                                                   astats))
                                        aqstats (astats q)
                                        aqstats (if conv?
                                                  {:imps (inc (aqstats :imps)),
                                                   :prom-imps (if is-prom
                                                                (inc (aqstats :prom-imps))
                                                               (aqstats :prom-imps)),
                                                   :pos-sum (+ (aqstats :pos-sum)
                                                               pos),
                                                   :pos-vec (conj (aqstats :pos-vec) pos),
                                                   :clicks (inc (aqstats :clicks)),
                                                   :cost (+ (aqstats :cost)
                                                            cpc),
                                                   :convs (inc (aqstats :convs)),
                                                   :revenue (+ (aqstats :revenue) USP)}
                                                  (if click?
                                                    {:imps (inc (aqstats :imps)),
                                                     :prom-imps (if is-prom
                                                                  (inc (aqstats :prom-imps))
                                                                  (aqstats :prom-imps)),
                                                     :pos-sum (+ (aqstats :pos-sum)
                                                                 pos),
                                                     :pos-vec (conj (aqstats :pos-vec) pos),
                                                     :clicks (inc (aqstats :clicks)),
                                                     :cost (+ (aqstats :cost)
                                                              cpc),
                                                     :convs (aqstats :convs),
                                                     :revenue (aqstats :revenue)}
                                                    {:imps (inc (aqstats :imps)),
                                                     :prom-imps (if is-prom
                                                                  (inc (aqstats :prom-imps))
                                                                  (aqstats :prom-imps)),
                                                     :pos-sum (+ (aqstats :pos-sum)
                                                                 pos),
                                                     :pos-vec (conj (aqstats :pos-vec) pos),
                                                     :clicks (aqstats :clicks),
                                                     :cost (aqstats :cost),
                                                     :convs (aqstats :convs),
                                                     :revenue (aqstats :revenue)}))
                                        astats (assoc! astats q aqstats)
                                        stats (assoc! stats agent astats)]
                                   (recur (rest qagents) stats cont?))
                                 (let [agent (first qagent) ;agent not continuing
                                       is-prom (metadata :is-prom)
                                       astats (stats agent)
                                       aqstats (astats q)
                                       aqstats {:imps (inc (aqstats :imps)),
                                                :prom-imps (if is-prom
                                                             (inc (aqstats :prom-imps))
                                                             (aqstats :prom-imps)),
                                                :pos-sum (+ (aqstats :pos-sum)
                                                            pos),
                                                :pos-vec (conj (aqstats :pos-vec) pos),
                                                :clicks (aqstats :clicks),
                                                :cost (aqstats :cost),
                                                :convs (aqstats :convs),
                                                :revenue (aqstats :revenue)}
                                       astats (assoc! astats q aqstats)
                                       stats (assoc! stats agent astats)]
                                   (recur (rest qagents) stats cont?)))))))]
             (recur (rest search-queue)
                    ranked-agents
                    stats)))))))

(defn simulate-expected-day
  ([status day]
     (let [random (java.util.Random.)
           search-pool (mk-expected-search-pools-map status day)
           search-queue (mk-search-queue search-pool random false)]
       (simulate-day status day search-queue random false))))

(defn simulate-random-day
  ([status day]
     (let [random (java.util.Random.)
           search-pool (mk-random-search-pools-map status day random)
           search-queue (mk-search-queue search-pool random false)]
       (simulate-day status day search-queue random false))))


(defn simulate-query
  [status day query]
  (let [random (java.util.Random.)
        search-pool (mk-expected-search-pools-map status day)
        search-queue (mk-search-queue search-pool random query)]
    (simulate-day status day search-queue random query)))

(defn combine-queries
  [stats]
  (reduce (fn [coll [agent astats]]
            (assoc coll agent
                   (reduce
                    (fn [coll2 [query qstats]]
                      (if (keyword? query)
                        coll2
                        {:imps (+ (qstats :imps)
                                 (coll2 :imps)),
                        :prom-imps (+ (qstats :prom-imps)
                                      (coll2 :prom-imps)),
                        :clicks (+ (qstats :clicks)
                                   (coll2 :clicks)),
                        :cost (+ (qstats :cost)
                                 (coll2 :cost)),
                        :convs (+ (qstats :convs)
                                  (coll2 :convs)),
                        :revenue (+ (qstats :revenue)
                                    (coll2 :revenue))}))
                    (peek (first astats)) (rest astats))))
          {} stats))

(defn combine-stats-days
  [statslst]
  (reduce (fn [coll vals1]
            (reduce (fn [coll2 [agent vals2]]
                      (assoc coll2 agent
                             {:imps (+ ((coll agent) :imps)
                                       (vals2 :imps)),
                              :prom-imps (+ ((coll agent) :prom-imps)
                                            (vals2 :prom-imps)),
                              :clicks (+ ((coll agent) :clicks)
                                         (vals2 :clicks)),
                              :cost (+ ((coll agent) :cost)
                                       (vals2 :cost)),
                              :convs (+ ((coll agent) :convs)
                                        (vals2 :convs)),
                              :revenue (+ ((coll agent) :revenue)
                                          (vals2 :revenue))}))
                    {} vals1))
          (first statslst) (rest statslst)))

(defn mk-query-report
  [status stats agent day]
  (let [astats (stats agent)
        agents (status :agents)
        num-agents (count agents)
        qs (status :query-space)
        ads (status :ads)
        ad ((ads agent) day)
        qr (new QueryReport)]
    (doall (map (fn [^Query query]
                  (let [aqstats (astats query)]  ;Add our own info
                    (.addQuery qr
                               query
                               (int (- (aqstats :imps)
                                       (aqstats :prom-imps)))
                               (int (aqstats :prom-imps))
                               (int (aqstats :clicks))
                               (double (aqstats :cost))
                               (double (aqstats :pos-sum))))
                  (.setAd qr query ^Ad (ad query))
                                        ;Add opp avg pos info
                  (loop [agent-idx (int 0)]
                    (if (not (< agent-idx num-agents))
                      nil
                      (let [^String agent2 (agents agent-idx)
                            aqstats2 ((stats agent2) query)]
                        (.setAd qr query (str "adv" (inc agent-idx)) ^Ad (((ads agent2) day) query))
                        (.setPosition qr query (str "adv" (inc agent-idx)) (double (aqstats2 :savg-pos)))
                        (recur (inc agent-idx))))))
                qs))
    qr))

(defn mk-sales-report
  [status stats agent day]
  (let [astats (stats agent)
        qs (status :query-space)
        sr (new SalesReport)]
    (do
      (doall (map (fn [^Query query]
                    (.addQuery sr query)
                    (.addConversions sr query (int ((astats query) :convs)))
                    (.addRevenue sr query (double ((astats query) :revenue))))
                  qs))
      sr)))


(defn stats-get-convs
  [status stats agent]
  (let [astats (stats agent)]
    (reduce + (map (fn [query]
                     ((astats query) :convs))
                   (status :query-space)))))

(defn simulate-game-with-agent
  [status ^AbstractAgent agent agent-to-replace]
  (if (not (some #{agent-to-replace} (status :agents)))
    (prn "There is no such agent(" agent-to-replace "to replace")
    (let [adv-effect ((status :adv-effects) agent-to-replace)
          squash-param (status :squash-param)
          queryspace (status :query-space)]
      (do
        (.sendSimMessage agent
                         (new Message "blank" "blank" (status :publish-info)))
        (.sendSimMessage agent
                         (new Message "blank" "blank" (status :slot-info)))
        (.sendSimMessage agent
                         (new Message "blank" "blank" (status :retail-cat)))
        (.sendSimMessage agent
                         (new Message "blank" "blank" ((status :adv-info) agent-to-replace)))
        (.initBidder agent)
        (.setModels agent (.initModels agent))
        (loop [day 0
               statslst []]
          (if (>= day 59)
            (combine-stats-days (map combine-queries statslst))
            (let [start-time (. System (nanoTime))]
              (.setDay agent day)
              (when (>= day 2)
                (let [oldday (- day 2)
                      oldstats (statslst oldday)
                      qr (mk-query-report status oldstats agent-to-replace oldday)
                      sr (mk-sales-report status oldstats agent-to-replace oldday)]
                  (.handleQueryReport agent qr)
                  (.handleSalesReport agent sr)
                  (.updateModels agent sr qr)))
              (let [sales-window (int (status :sales-window))
                    startconvslst (if (< day (- sales-window 1))
                                    (concat (replicate (- (- sales-window 1) day)
                                                       (/ ((status :capacities) agent-to-replace)
                                                          sales-window))
                                            (map (fn [past-day]
                                                   (stats-get-convs status
                                                                    (statslst past-day)
                                                                    agent-to-replace))
                                                 (range day)))
                                    (map (fn [past-day]
                                           (stats-get-convs status
                                                            (statslst past-day)
                                                            agent-to-replace))
                                         (range (- day (- sales-window 1)) day)))
                    startconvs (reduce + startconvslst)
                    bundle (do
                             (.handleStartSales agent (into-array Integer/TYPE startconvslst))
                             (.getBidBundle agent (.getModels agent)))
                    status (assoc status :bid-bundle
                                  (assoc (status :bid-bundle) agent-to-replace
                                         (assoc ((status :bid-bundle) agent-to-replace) day bundle)))
                    status (assoc status :budgets
                                  (assoc (status :budgets) agent-to-replace
                                         (assoc ((status :budgets) agent-to-replace) day
                                                (mk-single-budget-map bundle queryspace))))
                    status (assoc status :ads
                                  (assoc (status :ads) agent-to-replace
                                         (assoc ((status :ads) agent-to-replace) day
                                                (mk-single-ad-map bundle queryspace))))
                    status (assoc status :squashed-bids
                                  (assoc (status :squashed-bids) agent-to-replace
                                         (assoc ((status :squashed-bids) agent-to-replace) day
                                                (mk-single-squashed-bid-map bundle
                                                                            adv-effect
                                                                            squash-param
                                                                            queryspace))))
                    status (assoc status :start-sales
                                  (assoc (status :start-sales) agent-to-replace
                                         (assoc ((status :start-sales) agent-to-replace)
                                           day
                                           startconvs)))
                    stats (simulate-expected-day status day)]
                (do
                  (.handleBidBundle agent bundle)
                  ;(prn "Seconds spent on day " day ": " (/ (double (- (. System (nanoTime)) start-time)) 1000000000.0))
                  (recur (inc day) (conj statslst stats)))))))))))


(defn simulate-game
  [status]
  (let [statslst (map (fn [day] (combine-queries (simulate-expected-day status day))) (range 59))]
    (combine-stats-days statslst)))

(defn game-full-summary
  [status]
  (let [agents (status :agents)
        queryspace (status :query-space)
        queryreports (status :query-report)
        salesreports (status :sales-report)]
    (reduce (fn [coll1 agent]
              (assoc coll1 agent
                     (let [aqueryreports (queryreports agent)
                           asalesreports (salesreports agent)]
                       (reduce (fn [coll2 day]
                                 (let [^QueryReport qrs (aqueryreports day)
                                       ^SalesReport srs (asalesreports day)]
                                   (reduce (fn [coll3 ^Query query]
                                             {:imps (+ (coll3 :imps)
                                                       (.getImpressions qrs query)),
                                              :prom-imps (+ (coll3 :prom-imps)
                                                            (.getPromotedImpressions qrs query)),
                                              :clicks (+ (coll3 :clicks)
                                                         (.getClicks qrs query)),
                                              :convs (+ (coll3 :convs)
                                                        (.getConversions srs query)),
                                              :cost (+ (coll3 :cost)
                                                       (.getCost qrs query)),
                                              :revenue (+ (coll3 :revenue)
                                                          (.getRevenue srs query))})
                                           coll2 queryspace)))
                               {:imps 0,
                                :prom-imps 0,
                                :clicks 0,
                                :convs 0,
                                :cost 0,
                                :revenue 0}
                               (range 59)))))
            {} agents)))


(defn game-full-dists
  [status numests]
  (let [actualstats (game-full-summary status)
        statsmaplst (map (fn [_] (simulate-game status)) (range numests))
        statslst (reduce (fn [coll agent]
                           (assoc coll agent
                                  (reduce (fn [coll2 statsmap]
                                            (let [stats (statsmap agent)]
                                              {:imps (conj (coll2 :imps)
                                                        (stats :imps)),
                                               :prom-imps (conj (coll2 :prom-imps)
                                                             (stats :prom-imps)),
                                               :clicks (conj (coll2 :clicks)
                                                          (stats :clicks)),
                                               :convs (conj (coll2 :convs)
                                                         (stats :convs)),
                                               :cost (conj (coll2 :cost)
                                                        (stats :cost)),
                                               :revenue (conj (coll2 :revenue)
                                                           (stats :revenue))}))
                                          (let [stats ((first statsmaplst) agent)]
                                            (reduce (fn [coll key]
                                                      (assoc coll key [(stats key)]))
                                                    {} (keys stats)))
                                          (rest statsmaplst))))
                         {} (status :agents))
        statsdists (reduce (fn [coll [agent statsmap]]
                             (assoc coll agent
                                    {:imps (calc-mean-and-std (statsmap :imps)),
                                     :prom-imps (calc-mean-and-std (statsmap :prom-imps)),
                                     :clicks (calc-mean-and-std (statsmap :clicks)),
                                     :convs (calc-mean-and-std (statsmap :convs)),
                                     :cost (calc-mean-and-std (statsmap :cost)),
                                     :revenue (calc-mean-and-std (statsmap :revenue))}))
                           {} statslst)
        samplst (loop [agents (status :agents)
                       samplst []]
                  (if (not (seq agents))
                    samplst
                    (recur (rest agents)
                           (let [agent (first agents)
                                 aactualstats (actualstats agent)
                                 astatsdists (statsdists agent)]
                             (loop [;allkeys (keys astatsdists)
                                    allkeys #{:imps}
                                    samplst samplst]
                               (if (not (seq allkeys))
                                 samplst
                                 (recur (rest allkeys)
                                        (let [key (first allkeys)
                                              stat (aactualstats key)
                                              statdist (astatsdists key)]
                                          (conj samplst
                                                (let [std-dev (peek statdist)]
                                                  (if (== std-dev 0.0)
                                                    0.0
                                                    (/ (- stat (first statdist))
                                                      std-dev))))))))))))]
    (calc-mean-and-std samplst)))


(defn stats-summary
  ([stats]
     (reduce (fn [coll [agent statmap]]
               (assoc coll agent
                      (- (statmap :total-revenue)
                         (statmap :total-cost))))
             {} stats)))

(defn stats-summary-expected
  ([status day]
     (stats-summary (simulate-expected-day status day))))

(defn stats-summary-random
  ([status day]
     (stats-summary (simulate-random-day status day))))

(defn day-summary
  [status day]
  (let [agents (status :agents)
        queryspace (status :query-space)
        queryreports (status :query-report)
        salesreports (status :sales-report)]
    (reduce (fn [coll agent]
              (assoc coll agent
                     (let [^QueryReport queryreport ((queryreports agent) day)
                           ^SalesReport salesreport ((salesreports agent) day)]
                       (reduce +
                               (map (fn [^Query query] (- (.getRevenue salesreport query)
                                                  (.getCost queryreport query)))
                                    queryspace)))))
            {} agents)))


(defn map-to-map-of-vecs
  [map]
  (persistent! (reduce (fn [coll [k v]]
                         (assoc! coll k [v]))
                       (transient {})
                       map)))

(defn maps-to-map-of-vecs
  [maps]
  (loop [m (map-to-map-of-vecs (first maps))
         maps (rest maps)]
    (if (not (seq maps))
      m
      (let [m2 (first maps)
            m (reduce (fn [coll [k v]] (assoc coll k (conj (coll k) v))) m m2)]
        (recur m (rest maps))))))

(defn stats-dists
  [status day num-ests expected?]
  (let [stats-maps (if expected?
                     (map (fn [_] (stats-summary-expected status day)) (range num-ests))
                     (map (fn [_] (stats-summary-random status day)) (range num-ests)))
        map-of-vecs (maps-to-map-of-vecs stats-maps)
        sampled-stats (day-summary status day)]
    (reduce (fn [coll [k v]]
              (let [mean (calc-mean v)
                    std-dev (calc-std-dev v mean)]
                (if (== std-dev 0.0)
                  (assoc coll k 0.0)
                  (assoc coll k (/ (- mean (sampled-stats k)) std-dev)))))
            {}
            map-of-vecs)))


(defn all-stats-dists
  [status num-ests expected?]
  (let [all-stats (map (fn [day] (stats-dists status day num-ests expected?)) (range 59))
        sample-lst (reduce (fn [coll val]
                             (reduce (fn [coll2 [k val2]]
                                       (conj coll2 val2))
                                     coll
                                     val))
                           []
                           all-stats)
        mean (calc-mean sample-lst)
        std-dev (calc-std-dev sample-lst mean)]
    [mean std-dev]))


(defn gauss-pdf
  [mean sd x]
  (let [var (* sd sd)
        meandiff (- x mean)
        mdsq (* meandiff meandiff)]
    (* (/ 1.0 (Math/sqrt (* 2 Math/PI var)))
       (Math/exp (- (/ mdsq (* 2 var)))))))


(defn convpr-penalty
  [lam rem-cap sol-weight]
  (let [rem-cap (double rem-cap)
        sol-weight (double sol-weight)]
    (if (< rem-cap 0)
      (let [arem-cap (Math/abs rem-cap)]
        (if (<= sol-weight 0)
          (Math/pow lam
                    arem-cap)
          (loop [psum (double 0.0)
                 n (double (+ arem-cap 1))]
            (if (not (<= n (+ arem-cap sol-weight)))
              (/ psum sol-weight)
              (recur (+ psum
                        (Math/pow lam
                                  n))
                     (inc n))))))
      (if (<= sol-weight 0)
        1.0
        (if (>= rem-cap sol-weight)
          1.0
          (loop [psum (double rem-cap)
                 n (int 1)]
            (if (not (<= n (- sol-weight rem-cap)))
              (/ psum sol-weight)
              (recur (+ psum
                        (Math/pow lam
                                  n))
                     (inc n)))))))))


(defn solution-weight
  [stats convprs query-type rem-cap comp-spec comp-bonus lam]
  (loop [weight 0]
    (let [penalty (convpr-penalty lam rem-cap weight)
          new-weight (reduce
                      +
                      (map (fn [[^Query query stat]]
                             (let [convpr (* (convprs (query-type query))
                                             penalty)
                                   qcomp (.getComponent query)]
                               (* (stat :clicks)
                                  (if (= comp-spec
                                         qcomp)
                                    (eta convpr
                                         (+ 1 comp-bonus))
                                    (if (= nil
                                           qcomp)
                                      (+ (* (eta convpr
                                                 (+ 1 comp-bonus))
                                            (/ 1.0 3.0))
                                         (* convpr
                                            (/ 2.0 3.0)))
                                      convpr)))))
                           stats))]
      (if (< (Math/abs (double (- new-weight
                                  weight)))
             1.0)
        new-weight
        (recur new-weight)))))


(defn eval-per-q-sol
  [stats agent convprs query-type capacity start-sales comp-spec man-spec comp-bonus man-bonus usp lam]
  (let [stats (reduce (fn [coll [query stat]]
                        (assoc coll query ((stat agent) query)))
                      {} stats)
        rem-cap (- capacity start-sales)
        weight (solution-weight stats convprs query-type rem-cap
                                comp-spec comp-bonus lam)
        penalty (convpr-penalty lam rem-cap weight)]
    (reduce + (map (fn [[^Query query stat]]
                     (- (* (* (stat :clicks)
                              (* (convprs (query-type query))
                                 penalty))
                           (let [qman (.getManufacturer query)]
                             (if (= man-spec
                                    qman)
                               (* usp
                                  (+ 1 man-bonus))
                               (if (= nil
                                      qman)
                                 (+ (* (* usp
                                          (+ 1 man-bonus))
                                       (/ 1.0 3.0))
                                    (* usp
                                       (/ 2.0 3.0)))
                                 usp))))
                        (stat :cost)))
                   stats))))


(defn simq-indep
  [status day]
  (let [stats (reduce (fn [coll query]
                     (assoc coll query (simulate-query status day query)))
                      {} (status :query-space))
        qprofits (reduce (fn [coll agent]
                          (assoc coll agent (eval-per-q-sol stats agent (status :conv-probs)
                                                            (status :query-type)
                                                            ((status :capacities) agent)
                                                            (((status :start-sales) agent) day)
                                                            ((status :comp-specialties) agent)
                                                            ((status :man-specialties) agent)
                                                            (status :comp-bonus)
                                                            (status :man-bonus)
                                                            (status :USP)
                                                            (status :lam))))
                         {} (status :agents))
        dprofits (stats-summary-expected status day)
        actual (day-summary status day)]
    [(map (fn [agent] (let [actprof (actual agent)]
                       (if (not (== actprof 0.0))
                         (double (/ (- (qprofits agent)
                                       actprof)
                                    actprof))
                         (if (not (== (qprofits agent) 0.0))
                           (if (< (qprofits agent) 0.0)
                             -1
                             1)
                           0.0))))
          (status :agents))
     (map (fn [agent] (let [actprof (actual agent)]
                       (if (not (== actprof 0.0))
                         (double (/ (- (dprofits agent)
                                       actprof)
                                    actprof))
                         (if (not (== (dprofits agent) 0.0))
                           (if (< (dprofits agent) 0.0)
                             -1
                             1)
                           0.0))))
          (status :agents))]))

(defn bootstrap-mean-and-std
  [lst]
  (let [stats (criterium.core/bootstrap-bca
               lst
               (juxt criterium.stats/mean criterium.stats/variance)
               1000
               [0.5 0.95 0.05]
               criterium.well/well-rng-1024a)]
    (do
      (prn (criterium.core/outliers lst))
      [(first (first stats)) (Math/sqrt (first (second stats)))])))


(defn remove-outliers
  [lst]
  (let [mean (calc-mean lst)
        std-dev (calc-std-dev lst mean)
        newlst (filter (fn [x] (and (> x
                                      (- mean
                                         (* std-dev
                                            4)))
                                   (< x
                                      (+ mean
                                         (* std-dev
                                            4)))))
                       lst)]
    (if (== (count lst) (count newlst))
      newlst
      (remove-outliers newlst))))


(defn simq-dists
  [status]
  (let [results (map (fn [day] (simq-indep status day)) (range 59))
        qdiffs (flatten (map first results))
        qdiffsrem (remove-outliers qdiffs)
        ddiffs (vec (flatten (map peek results)))
        ddiffsrem (remove-outliers ddiffs)]
    (do
      [(calc-mean-and-std ddiffsrem)
       (calc-mean-and-std ddiffs)
       (calc-mean-and-std qdiffsrem)
       (calc-mean-and-std qdiffs)])))

;(use 'criterium.core)


                                        ;TODO
                                        ; add sampling to simulation

(def file1 "/Users/jordanberg/Desktop/tacaa2010/game-tacaa1-15129.slg")
(def status1 (init-sim-info (tacaa.parser/parse-file file1)))


(defn crap
  []
  (doall
   (for [x (range 15127 15150) :let [file (str "/Users/jordanberg/Desktop/tacaa2010/game-tacaa1-" x ".slg")]]
     (do
       (prn "File: " file)
       (prn (doall (simq-dists (init-sim-info (tacaa.parser/parse-file file)))))))))
