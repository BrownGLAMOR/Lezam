(ns tacaa.parser
  (:import (java.io InputStream FileInputStream)
           (se.sics.isl.transport Transportable)
           (se.sics.tasim.logtool LogReader ParticipantInfo)
           (edu.umich.eecs.tac Parser TACAAConstants)
           (edu.umich.eecs.tac.props BankStatus SlotInfo ReserveInfo PublisherInfo
                                     SalesReport QueryReport BidBundle RetailCatalog
                                     UserClickModel AdvertiserInfo UserPopulationState
                                     Ad Product Query QueryType)))

(set! *warn-on-reflection* true)

(def file "/Users/jordanberg/Documents/workspace/Clients/game1.slg")

(def messages (atom []))

(def day (atom -1))

(defn mk-game-parser
  ([^String file] (mk-game-parser file (new LogReader (new FileInputStream file))))
  ([^String file reader]
     (proxy [Parser] [reader]
       (message [from to content]
                (swap! messages conj [from to @day content]))
       (dataUpdated
        ([agent type value]
           (when (instance? UserPopulationState value)
             (swap! messages conj [agent type @day value])))
        ([type content] ()))
       (nextDay [date serverTime] (reset! day date)))))


(defn mk-queryspace
  "Returns a query space given a retail catalog"
  [retail]
  (reduce (fn [coll ^Product val] (let [man (.getManufacturer val)
                                       comp (.getComponent val)
                                       f1cq (new Query nil comp)
                                       f1mq (new Query man nil)
                                       f2q (new Query man comp)]
                                   (reduce conj coll [f1cq f1mq f2q]))) #{(new Query)} retail))

(defn remove-nan-bids
  ([^BidBundle bundle ^BidBundle lastbundle queryspace]
     (let [^BidBundle newbundle (reduce (fn [^BidBundle coll ^Query val]
                                          (do
                                            (if (not (.containsQuery bundle val))
                                              (.addQuery coll val 0 (new Ad) 0)
                                              (let [bid (.getBid bundle val)
                                                    newbid (if (or (Double/isNaN bid) (< bid 0))
                                                             (.getBid lastbundle val)
                                                             bid)
                                                    budget (.getDailyLimit bundle val)
                                                    newbudget (if (Double/isNaN budget)
                                                                (.getDailyLimit lastbundle val)
                                                                budget)
                                                    ad (.getAd bundle val)
                                                    newad (if (nil? ad)
                                                            (.getAd lastbundle val)
                                                            ad)]
                                                (.addQuery coll val newbid newad newbudget)))
                                            coll))
                                        (new BidBundle) queryspace)
           total-budget (.getCampaignDailySpendLimit bundle)]
       (do (if (Double/isNaN total-budget)
             (.setCampaignDailySpendLimit newbundle (.getCampaignDailySpendLimit lastbundle))
             (.setCampaignDailySpendLimit newbundle total-budget))
           newbundle)))
  ([^BidBundle bundle queryspace]
     (let [^BidBundle newbundle (reduce (fn [^BidBundle coll ^Query val]
                                          (do
                                            (if (not (.containsQuery bundle val))
                                              (.addQuery coll val 0 (new Ad) 0)
                                              (let [bid (.getBid bundle val)
                                                    newbid (if (or (Double/isNaN bid) (< bid 0))
                                                             0.0
                                                             bid)
                                                    budget (.getDailyLimit bundle val)
                                                    newbudget (if (Double/isNaN budget)
                                                                Double/MAX_VALUE
                                                                budget)
                                                    ad (.getAd bundle val)
                                                    newad (if (nil? ad)
                                                            (new Ad)
                                                            ad)]
                                                (.addQuery coll val newbid newad newbudget)))
                                            coll))
                                        (new BidBundle) queryspace)
           total-budget (.getCampaignDailySpendLimit bundle)]
       (do (if (Double/isNaN total-budget)
             (.setCampaignDailySpendLimit newbundle Double/MAX_VALUE)
             (.setCampaignDailySpendLimit newbundle total-budget))
           newbundle))))

(defn mk-empty-bundle
  "When we make a new bundle in AA it sets the default total budget
   to be NaN, but we don't want any NaNs.  So create a new bundle and
   set a new total budget before returning"
  []
  (let [bundle (new BidBundle)]
    (do (.setCampaignDailySpendLimit bundle Double/MAX_VALUE)
        bundle)))

(defn check-bid-bundles
  [status]
  (let [bid-bundles (status :bid-bundle)]
    (assoc status :bid-bundle
           (reduce (fn [coll [agent val]]
                     (assoc coll agent
                            (let [bundles (bid-bundles agent)
                                  numbundles (count bundles)]
                              (if (== numbundles 59)
                                bundles
                                (reduce conj
                                        bundles ;replicate bundles as needed
                                        (vec (replicate (- 59 numbundles)
                                                        (peek bundles))))))))
                   {} bid-bundles))))

(defn parse-messages
  "Parses all of the messages into a game status"
  ([status messages]
     (if (seq messages)
       (recur
        (let [[from to day content] (first messages)
              from ((status :partic-names) from)
              to ((status :partic-names) to)]
          (cond
           (instance? BidBundle content) (if (< day 59)
                                           (if (status :bid-bundle)
                                             (let [bid-bundle (status :bid-bundle)]
                                               (if (bid-bundle from)                                          
                                                 (let [bundles (bid-bundle from)
                                                       numbundles (count bundles)
                                                       lastbundle (peek bundles)
                                                       newbundle (remove-nan-bids content
                                                                                  lastbundle
                                                                                  (status :query-space))]
                                                   (if (< day numbundles) ;too many bundles
                                                     (assoc status :bid-bundle
                                                            (assoc bid-bundle from
                                                                   (conj (pop bundles) newbundle))) ;remove one
                                                     (assoc status :bid-bundle
                                                            (assoc bid-bundle from
                                                                   (reduce conj
                                                                           bundles ;replicate bundles as needed
                                                                           (conj (vec (replicate (- day numbundles)
                                                                                                 lastbundle))
                                                                                 newbundle))))))
                                                 (let [newbundle (remove-nan-bids content (status :query-space))]
                                                   (assoc status :bid-bundle
                                                          (assoc bid-bundle from
                                                                 (reduce conj []
                                                                         (conj (vec (replicate day
                                                                                               (mk-empty-bundle)))
                                                                               newbundle)))))))
                                             (let [newbundle (remove-nan-bids content (status :query-space))]
                                               (assoc status :bid-bundle
                                                      {from (reduce conj []
                                                                  (conj (vec (replicate day
                                                                                        (mk-empty-bundle)))
                                                                        newbundle))})))
                                           (let [bid-bundle (status :bid-bundle)
                                                 bundles (bid-bundle from)
                                                 numbundles (count bundles)
                                                 lastbundle (peek bundles)]
                                             (if (< numbundles 59)
                                               (assoc status :bid-bundle
                                                      (assoc bid-bundle from
                                                             (reduce conj
                                                                     bundles ;replicate bundles as needed
                                                                     (vec (replicate (- 59 numbundles)
                                                                                     lastbundle)))))
                                               status)))
           (instance? SalesReport content) (if (> day 1)
                                             (if (status :sales-report)
                                               (let [sales-report (status :sales-report)]
                                                 (if (sales-report to)
                                                   (assoc status :sales-report
                                                          (assoc sales-report to
                                                                 (conj (sales-report to) content)))
                                                   (assoc status :sales-report
                                                          (assoc sales-report to [content]))))
                                               (assoc status :sales-report {to [content]}))
                                             status)
           (instance? QueryReport content) (if (> day 1)
                                             (if (status :query-report)
                                               (let [query-report (status :query-report)]
                                                 (if (query-report to)
                                                   (assoc status :query-report
                                                          (assoc query-report to
                                                                 (conj (query-report to) content)))
                                                   (assoc status :query-report
                                                          (assoc query-report to [content]))))
                                               (assoc status :query-report {to [content]}))
                                             status)
           (instance? BankStatus content) (if (> day 0)
                                            (if (status :bank-stat)
                                              (let [bank-stat (status :bank-stat)]
                                                (if (bank-stat to)
                                                  (assoc status :bank-stat
                                                         (assoc bank-stat to
                                                                (conj (bank-stat to) content)))
                                                  (assoc status :bank-stat
                                                         (assoc bank-stat to [content]))))
                                              (assoc status :bank-stat {to [content]}))
                                            status)
           (instance? UserPopulationState content) (if (> day 0)
                                                     (let [retail (seq (status :retail-cat))
                                                           contvec (reduce (fn [coll val]
                                                                             (assoc coll val
                                                                                    (seq (. ^UserPopulationState content (getDistribution ^Product val)))))
                                                                           {} (seq retail))]
                                                       (if (status :user-pop)
                                                         (assoc status :user-pop
                                                                (conj (status :user-pop) contvec))
                                                         (assoc status :user-pop [contvec])))
                                                     status)
           (instance? SlotInfo content) (if (status :slot-info)
                                          status
                                          (assoc status :slot-info content))
           (instance? ReserveInfo content) (if (status :reserve-info)
                                             status
                                             (assoc status :reserve-info content))
           (instance? PublisherInfo content) (if (status :publish-info)
                                               status
                                               (assoc status :publish-info content))
           (instance? RetailCatalog content) (if (status :retail-cat)
                                               status
                                               (let [queryspace (mk-queryspace content)]
                                                 (merge status {:retail-cat content,
                                                                :query-space queryspace,
                                                                :qsf0 #{(new Query)},
                                                                :qsf1 (into #{} (filter (fn [^Query query] (= (.getType query)
                                                                                                             QueryType/FOCUS_LEVEL_ONE)) queryspace))
                                                                :qsf2 (into #{} (filter (fn [^Query query] (= (.getType query)
                                                                                                             QueryType/FOCUS_LEVEL_TWO)) queryspace))})))
           (instance? UserClickModel content) (if (status :user-click)
                                                status
                                                (assoc status :user-click content))
           (instance? AdvertiserInfo content) (if (status :adv-info)
                                                (assoc status :adv-info
                                                       (assoc (status :adv-info) to
                                                              content))
                                                (assoc status :adv-info {to content}))
           :else status))
        (rest messages))
       (check-bid-bundles status))))

(defn reset-parser
  []
  (do
    (reset! messages [])
    (reset! day -1)))

(defn parse-file
  [^String file]
  (do
    (reset-parser)
    (let [reader (new LogReader (new FileInputStream file))
          participants (seq (.getParticipants reader))
          partic-names (reduce (fn [coll ^ParticipantInfo val] (assoc coll
                                               (.getIndex val)
                                               (.getName val)))
                               {} participants)
          is-advertiser (reduce (fn [coll ^ParticipantInfo val] (assoc coll
                                               (.getIndex val)
                                               (= TACAAConstants/ADVERTISER (.getRole val))))
                                {} participants)
          agents (vec (filter identity
                              (map (fn [name is-adv]
                                     (if (peek is-adv)
                                       (peek name)
                                       nil)) partic-names is-advertiser)))
          ^Parser parser (mk-game-parser file reader)]
      (.start parser)
      (let []
        (parse-messages {:agents agents :partic-names partic-names} @messages)))))
