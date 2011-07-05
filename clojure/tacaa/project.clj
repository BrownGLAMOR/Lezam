(defproject tacaa "0.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 ;[incanter "1.2.2"]
                 [org.clojars.jberg/tasim "0.8.0.4.2"]
                 [org.clojars.jberg/aa-common "0.9.6.2"]
                 [org.clojars.jberg/aa-logging "0.9.6.2"]
		 [org.clojars.jberg//aa-agent "0.9.6.2"]]
  :dev-dependencies [[swank-clojure "1.3.0"]
                     [criterium "0.1.0"]]  
  :disable-deps-clean true
  :main tacaa.javasim
  :jvm-opts ["-server" "-Xmx1500m"])
