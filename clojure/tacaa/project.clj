(defproject tacaa "0.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [criterium "0.1.0"]
		 [incanter "1.2.2"]]
  :dev-dependencies [[swank-clojure "1.3.0"]]  
  :disable-deps-clean true
  :main tacaa.core
  :jvm-opts ["-server" "-Xmx1500m"])
