(ns utsusu.transfer
  (:require
    [clojure.string :refer [trim]]
    [me.raynes.conch :refer [programs]]
    [tentacles
     [users :as u]
     [repos :as r]
     [events :as e]
     [core :as tc]
     [data :as data]
     [orgs :as o]]))

(programs git du rm)
(def temp-dir "/tmp/utsusu")
(.mkdir (java.io.File. temp-dir))


(def ^:dynamic *config*)
(def ^:dynamic org)
(def ^:dynamic auth)
(def ^:dynamic endpoint)

(defmacro with-conf [prefix & body]
  `(binding [org (*config* (keyword (str (name ~prefix) "-org")))
             auth (mk-auth (*config* (keyword (str (name ~prefix) "-token"))))
             endpoint (endpoint-for (*config* (keyword (str (name ~prefix) "-domain"))))]
     (binding [tc/url endpoint]
       ~@body)))

(defn- endpoint-for [domain]
  (if (= domain "github.com")
    "https://api.github.com/"
    (str "https://" domain "/api/v3/")))

(defn mk-auth [token] {:oauth-token token})

(defn repos []
  (r/org-repos org (merge auth {:all-pages true})))

(defn path-for-repo [{:keys [name]}]
  (str temp-dir "/" name))

(defn create-repo [{:keys [private name description homepage] :as repo}]
  (println "Create repo" name)
  (let [new-repo (r/create-org-repo
                   org name
                   (merge auth
                          {:description description
                           :homepage homepage
                           :public (not private)}))
        new-repo (if (= (:status new-repo) 422)
                   (do
                     (println "Error creating repo:" (-> new-repo :body :errors first :message))
                     (println "Making sure it exists:")
                     (let [res (r/specific-repo org name auth)]
                       (println (:url res))
                       res))
                   new-repo)]
    (merge repo {:new-repo new-repo})))

(defn clone-repo [{:keys [name ssh_url] :as repo}]
  (let [path (path-for-repo repo)]
    (println "Cloning" ssh_url "into" path)
    (let [clone (git "clone" "--mirror" ssh_url path {:verbose true})]
      (when-not (= 0 @(:exit-code clone))
        (throw (Exception. (str "failed clone: " (:stderr clone))))))
    (println (trim (du "-hs" :dir path))))
  repo)

(defn push-repo [{:keys [new-repo name] :as repo}]
  (let [path (str temp-dir "/" name)
        dest-ssh-url (:ssh_url new-repo)]
    (println "Push" name "to" dest-ssh-url)
    (let [push (git "push" "--mirror" dest-ssh-url :dir path {:verbose true})]
      (println (trim ((:juxt :stdout :stderr) push)))))
  repo)

(defn cleanup-repo [{:keys [new-repo name] :as repo}]
  (let [path (path-for-repo repo)]
    (println "Removing" path)
    (println)
    (rm "-rf" path))
  repo)

(defn specific-org [] (o/specific-org org auth))

(defn ensure-connectivity []
  (doall
    (for [[prefix fullname] {:source "source" :dest "destination"}]
      (do
        (println "Ensuring" fullname "org connectivity:")
        (let [o (with-conf prefix (specific-org))
              url (:html_url o)]
          (if (nil? url)
            (throw (Exception. (str "Could not connect to " fullname)))
            (println "✓ Connected to" fullname "at" url)))))))

(defn transfer
  "Transfer repos from source to destination, one at a time:
   1. Get a list of source repos
   2. Create repos on destination with same name, description and homepage
   3. Clone each source repo using `git clone --mirror origin-url`
   4. Push each source repo to destination using `git push --mirror destination-url`"
  [config]
  (println "Created" temp-dir)
  (binding [*config* config]
    (ensure-connectivity)
    (let [source-repos (with-conf :source (repos))]
      (println)
      (println "Found" (count source-repos) "repos on source")
      (println "For each repo, I will now:")
      (println " - create a repo of the same name on" (:dest-org config))
      (println " - git clone --mirror the repo into" temp-dir)
      (println " - git push --mirror to the new repo on" (:dest-org config))
      (println " - rm -rf the local repo")
      (println "For the sake of log readability, this is all done in series rather than in parallel.")
      (println)
      (println "BEGIN TRANSFER")
      (println)
      (let [repo-fn (if (:dry-run config)
                      #(println "Would transfer" (:ssh_url %))
                      (fn [repo] (-> repo create-repo clone-repo push-repo cleanup-repo)))]
        (with-conf :dest (dorun (map repo-fn source-repos)))
        (when-not (:dry-run config)
          (println "✓ Transfered" (count source-repos) "repos"))))
    (rm "-rf" temp-dir)
    (println "Removed" temp-dir)
    (println)
    (println "GREAT SUCCESS")))
