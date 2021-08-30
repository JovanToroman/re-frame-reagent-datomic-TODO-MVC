(ns app.queries
    (:require
     [datomic.client.api :as d]))


(defn todo
  "Find a todo based on its id."
  [db todo-id]
  (d/pull db [:*] todo-id))

(defn todos
      ([db]
       (todos db nil))
      ([db {:keys [cond]}]
       (let [q (cond->
                '{:find [(pull ?eid [*])]
                  :where [[?eid :todo/created]]}
                (map? cond) (concat (map (partial cons '?eid) (vec cond))))]
         (map first (d/q q db)))))

#_(defn pull
      ([db id]
       (pull db id '[*]))
      ([db id query]
       (d/pull (d/db db) (or query '[*]) id)))

(defn retract-entity! [conn id]
      (d/transact conn {:tx-data [[:db.fn/retractEntity id]]}))

(defn todo-update! [conn {:keys [db/id todo/completed todo/title]}]
  (d/transact conn {:tx-data [(merge {:db/id id}
                                     (when (boolean? completed)
                                       {:todo/completed completed})
                                     (when title
                                       {:todo/title title}))]}))

(defn add-entity!
  "Transact one item if it is a map, or many items otherwise."
  [conn item]
  (d/transact conn {:tx-data (if (map? item) [item] item)}))

(defn todos-delete-by! [conn cond-map]
      (let [todos (todos conn {:cond cond-map})
            tx-data (map #(vec [:db.fn/retractEntity (:db/id %)]) todos)]
           (d/transact conn tx-data)))

(defn todos-toggle-all! [conn value]
      (let [all-todos (todos conn)]
           (d/transact conn (map (partial merge {:todo/completed value}) all-todos))))

(defn read-tx-data [{:keys [db-after tx-data]}]
      (d/q '[:find ?e ?aname ?v ?added
             :in $ [[?e ?a ?v _ ?added]]
             :where
             [?a :db/ident ?aname]
             (not [?a :db/ident :db/txInstant])]
           db-after
           tx-data))

(defn write-tx-data! [conn tx-data]
      (->> (map (fn [datom]
                    (->> datom
                         (cons (if (last datom)
                                 :db/add
                                 :db/retract))
                         drop-last)) tx-data)
           (d/transact conn)))