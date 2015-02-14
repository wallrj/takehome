(ns takehome.integration
  (:require [takehome.test-common :refer :all]
            [clj-http.client :as http]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(fact-group
 :integration

 (fact "Ping resource returns 200 HTTP response"
       (let [response (http/get (url+ "/ping")  {:throw-exceptions false})]
         response => (contains {:status 200})))

 (fact "Healthcheck resource returns 200 HTTP response"
       (let [response (http/get (url+ "/healthcheck") {:throw-exceptions false})]
         response => (contains {:status 200})))

 (fact "Hello resource returns 200 HTTP response"
       (let [response (http/get (url+ "/hellos?nickname=world") {:throw-exceptions false})]
         response => (contains {:status 200})))

 (fact "Subscribe to topic returns 200 HTTP response"
       (let [response (http/post (url+ "/kittens_and_puppies/alice") {:throw-exceptions false})]
         response => (contains {:status 200})))

 (fact "Unsubscribe from topic returns 200 HTTP response"
       (let [response (http/delete (url+ "/kittens_and_puppies/alice") {:throw-exceptions false})]
         response => (contains {:status 200})))

 (fact "Unsubscribe from unknown topic returns 404 HTTP response"
       (let [response (http/delete (url+ "/unknown_topic/alice") {:throw-exceptions false})]
         response => (contains {:status 404})))

 (fact "Publish a message to topic returns 200 HTTP response"
       (let [response (http/post (url+ "/kittens_and_puppies") {:throw-exceptions false})]
         response => (contains {:status 200})))

 (fact "Retrieve message from topic returns 200 HTTP response"
       (let [response (http/get (url+ "/kittens_and_puppies/alice") {:throw-exceptions false})]
         response => (contains {:status 200})))

 (fact "Retrieve message from empty topic returns 204 HTTP response"
       (let [response (http/get (url+ "/kittens_and_puppies/alice") {:throw-exceptions false})]
         response => (contains {:status 204})))

 (fact "Retrieve message from unsubscribed topic returns 404 HTTP response"
       (let [response (http/get (url+ "/kittens_and_puppies/carol") {:throw-exceptions false})]
         response => (contains {:status 404})))

)
