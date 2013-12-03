(ns utsusu.transfer
  (:require
    [me.raynes.conch :refer [programs]]
    [tentacles
     [users :as u]
     [repos :as r]
     [events :as e]
     [data :as data]
     [orgs :as o]]))

(def temp-dir "/tmp/utsusu")
(.mkdir (java.io.File. temp-dir))
(programs git cd ls)

(def ^:dynamic *config*)
(def ^:dynamic org)
(def ^:dynamic auth)
(def ^:dynamic endpoint)

(defmacro with-conf [prefix & body]
  `(binding [org (*config* (keyword (str ~(name prefix) "-org")))
             auth (mk-auth (*config* (keyword (str (name ~prefix) "-token"))))
             endpoint (endpoint-for (*config* (keyword (str (name ~prefix) "-domain"))))]
     ~@body))

(defn- endpoint-for [domain]
  (if (= domain "github.com")
    "https://api.github.com"
    (str "https://" domain "/api/v3")))

(defn mk-auth [token] {:oauth-token token})

(defn repos []
  (r/org-repos org (merge auth {:all-pages true})))

(defn create-repo [{:keys [private name description homepage git_url] :as repo}]
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

(defn clone-repo [{:keys [name git_url] :as repo}]
  (let [path (str temp-dir "/" name)]
    (println "Cloning" git_url "into" path)
    (git "clone" "--mirror" git_url path))
  repo)

(defn push-repo [{:keys [new-repo name] :as repo}]
  (let [path (str temp-dir "/" name)
        dest-ssh-url (:ssh_url new-repo)]
    (println "Push" name "to" dest-ssh-url)
    (let [push (git "push" "--mirror" dest-ssh-url :dir path {:verbose true})]
      (println ((:juxt :stdout :stderr) push))))
  (println)
  repo)

(defn transfer
  "Transfer repos from source to destination, one at a time:
   1. Get a list of source repos
   2. Create repos on destination with same name
   3. Clone each source repo using `git clone --mirror origin-url`
   4. Push each source repo to destination using `git push --mirror destination-url`"
  [config]
  (binding [*config* config]
    (let [source-repos (with-conf :source (repos))]
      (println "Found" (count source-repos) "repos on source")
      (println)
      (dorun
        (map (fn [repo]
               (with-conf :dest
                          (-> repo
                              create-repo
                              clone-repo
                              push-repo))) source-repos))
      (println "☑ DONE: transfered" (count source-repos) "repos")))
  (println "Config was:" config))
