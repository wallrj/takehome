(ns takehome.unit.web
  (:require [takehome.web :refer :all]
            [takehome.db :as db]
            [cheshire.core :as json]
            [midje.sweet :refer :all])
  (:import [java.io ByteArrayInputStream InputStream]))

(defn- json-response?
  [res]
  (when-let [content-type (get-in res [:headers "Content-Type"])]
    (re-find #"^application/(..+)?json.+" content-type)))

(defn request
  "Creates a compojure request map and applies it to our routes.
   Accepts method, resource and optionally an extended map"
  [method resource & [{:keys [params body content-type headers]
                       :or {params {}
                            headers {}}}]]
  (let [{:keys [body] :as res}
        (app (merge {:request-method method
                     :uri resource
                     :params params
                     :headers headers}
                    (when body {:body (-> body json/generate-string .getBytes ByteArrayInputStream.)})
                    (when content-type {:content-type content-type})))]
    (cond-> res
            (instance? InputStream body)
            (update-in [:body] slurp)

            (json-response? res)
            (update-in [:body] #(json/parse-string % true)))))

(fact-group
 :unit

 (fact "Ping returns a pong"
       (:body (request :get "/ping"))  => "pong")

 (fact "Healthcheck returns OK"
       (let [resp (request :get "/healthcheck")]
         (:status resp) => 200
         (get-in resp [:body :name]) => "takehome"))

 (fact "Hello returns OK"
       (let [resp (request :get "/hello" {:params {:nickname "world"}})]
         (:status resp) => 200
         (get-in resp [:body]) => "Hello world!\n"))

 (fact "Subscribe to topic returns OK"
       (let [resp (request :post "/kittens_and_puppies/alice")]
         (:status resp) => 200))

 (fact "Unsubscribe from topic returns OK"
       (db/empty_messages)
       (db/subscribe "kittens_and_puppies" "alice")
       (let [resp (request :delete "/kittens_and_puppies/alice")]
         (:status resp) => 200))

 (fact "Unsubscribe from unsubscribed topic returns NOT_FOUND"
       (db/empty_messages)
       (let [resp (request :delete "/kittens_and_puppies/bob")]
         (:status resp) => 404))

 (fact "Publish to topic returns OK"
       (let [resp (request :post "/kittens_and_puppies" {:params {:message "http://cuteoverload.files.wordpress.com/2014/10/unnamed23.jpg?w=750&h=1000"}})]
         (:status resp) => 404))

 (fact "Retrieve the next message from a topic"
       (db/empty_messages)
       (db/subscribe "kittens_and_puppies" "alice")
       (db/message_push "kittens_and_puppies" "alice" "first message")
       (let [resp (request :get "/kittens_and_puppies/alice")]
         (:status resp) => 200
         (get-in resp [:body]) => "first message"))

 (fact "Retrieve the next message from empty topic"
       (db/empty_messages)
       (db/subscribe "kittens_and_puppies" "alice")
       (let [resp (request :get "/kittens_and_puppies/alice")]
         (:status resp) => 204
         (get-in resp [:body]) => "")))
