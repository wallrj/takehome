(ns takehome.db)

(require '[clojure.java.jdbc.deprecated :as sql])

(def db-config
    {:classname "org.h2.Driver"
     :subprotocol "h2"
     :subname "/tmp/takehome"})

(defn drop_messages []
  (sql/with-connection db-config
    (try (sql/drop-table :message)
         (catch org.h2.jdbc.JdbcBatchUpdateException ex nil))))

(defn create_messages []
  (sql/with-connection db-config
    (sql/create-table :message
                      [:id "bigint" "primary key" "auto_increment"]
                      [:user "varchar(256)"]
                      [:topic "varchar(256)"]
                      [:message "varchar(256)"])))

(defn empty_messages []
  (drop_messages)
  (create_messages))

(defn subscribe [topic user]
  (sql/with-connection db-config
    (sql/insert-records
     :message
     {:topic topic :user user :message "SUBSCRIBE"})))

(defn subscribed?
  "Return true if user is subscribed to topic."
  [topic user]
  (sql/with-connection db-config
    (sql/with-query-results results
     ["select * from message where message = ? and topic = ? and user = ? order by id desc limit 1" "SUBSCRIBE" topic user]
     (not (empty? (do results))))))

(defn unsubscribe [topic user]
  (sql/with-connection db-config
    (first (sql/delete-rows
            :message
            ["topic = ? and user = ?" topic user]))))

(defn topics
  "Return topics to which username is subscribed."
  [user]
  (sql/with-connection db-config
    (sql/with-query-results results
     ["select topic from message where user = ?" user]
     (do results))))

(defn message_push [topic user message]
  (sql/with-connection db-config
    (sql/insert-records
     :message
     {:topic topic :user user :message message})))

(defn message_pop
  "Return next message for username in topic."
  [topic user]
  (sql/with-connection db-config
    (sql/with-query-results results
     ["select * from message where message != ? and user = ? and topic = ? order by id desc limit 1" "SUBSCRIBE" user topic]
     (let [result (first (do results))]
       (sql/delete-rows "message" ["id = ?" (:id result)])
       result))))
