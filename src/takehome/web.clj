(ns takehome.web
  (:require [compojure
             [core :refer [defroutes context GET PUT POST DELETE]]
             [route :as route]]
            [metrics.ring
             [expose :refer [expose-metrics-as-json]]
             [instrument :refer [instrument]]]
            [radix
             [error :refer [wrap-error-handling error-response]]
             [ignore-trailing-slash :refer [wrap-ignore-trailing-slash]]
             [setup :as setup]
             [reload :refer [wrap-reload]]]
            [ring.middleware
             [format-params :refer [wrap-json-kw-params]]
             [format-response :refer [wrap-json-response]]
             [params :refer [wrap-params]]]))

(require '[clojure.java.jdbc.deprecated :as sql])

(def db-config
    {:classname "org.h2.Driver"
     :subprotocol "h2"
     :subname "/tmp/takehome"})


(sql/with-connection db-config
  (try (sql/drop-table :message)
       (catch org.h2.jdbc.JdbcBatchUpdateException ex nil))

  (sql/create-table :message
    [:user "varchar(256) primary key"]
    [:topic "varchar(256)"]
    [:message "varchar(256)"]))

(def version
  (setup/version "takehome"))

(defn healthcheck
  []
  (let [body {:name "takehome"
              :version version
              :success true
              :dependencies []}]
    {:headers {"content-type" "application/json"}
     :status (if (:success body) 200 500)
     :body body}))

(defn greet
  "Says hello!"
  [nickname]
  {:status 200 :body (format "Hello %s!\n" nickname)})


(defn subscribe
  "Subscribe username to topic"
  [topic username]
  (sql/with-connection db-config
    (sql/insert-records
     :message
     {:topic topic :user username :message "SUBSCRIBED"})

  {:status 200 :body (format "SUBSCRIBED\n")}))


(defn unsubscribe
  "Unsubscribe username from topic"
  [topic username]
  {:status 200 :body (format "UNSUBSCRIBED\n")})


(defroutes routes

  (GET "/healthcheck"
       [] (healthcheck))

  (GET "/ping"
       [] "pong")

  (GET "/hello"
       [nickname] (greet nickname))

  (POST "/:topic/:username"
       [topic username] (subscribe topic username))

  (DELETE "/:topic/:username"
       [topic username] (unsubscribe topic username))

  (route/not-found (error-response "Resource not found" 404)))

(def app
  (-> routes
      (wrap-reload)
      (instrument)
      (wrap-error-handling)
      (wrap-ignore-trailing-slash)
      (wrap-json-response)
      (wrap-json-kw-params)
      (wrap-params)
      (expose-metrics-as-json)))
