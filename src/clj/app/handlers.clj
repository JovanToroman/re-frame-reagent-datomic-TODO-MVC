(ns app.handlers
  (:require [app.queries :as q]
            [app.client :as client]
            [ring.util.http-response :as r]
            [taoensso.timbre :refer [infof]]))

(def conn (client/get-conn))

(defn get-items
  "This handler returns all of the todos."
  [{:keys [_params]}]
  (let [todos (q/todos (client/db))]
    (if (not-empty todos)
      (r/ok todos)
      (r/bad-request {:status :error}))))

(defn create-todo
  "Creates a todo note."
  [{:keys [parameters]}]
  (infof "Parameters: %s" parameters)
  (let [title       (get-in parameters [:body :todo/title])
        entity      {:todo/title     title
                     :todo/completed false
                     :todo/created   (java.util.Date.)}
        result      (q/add-entity! conn
                                   {:todo/title     title
                                    :todo/completed false
                                    :todo/created   (java.util.Date.)})
        new-note-id (-> result :tempids vals first)
        db-after    (:db-after result)
        new-todo    (q/todo db-after new-note-id)]
    (if (:db-after result)
      (r/ok new-todo)
      (r/bad-request))))

(defn update-todo
  "Updates a todo note."
  [{:keys [parameters]}]
  (let [data         (get-in parameters [:body :data])
        result       (q/todo-update! conn data)
        updated-todo (q/todo (client/db) (:db/id data))]
    (if (:db-after result)
      (r/ok updated-todo)
      (r/bad-request))))

(defn delete-todo
  "Retracts a todo note."
  [{:keys [parameters]}]
  (let [id     (get-in parameters [:body :db/id])
        result (q/retract-entity! conn id)]
    (if (:db-after result)
      (r/ok {:db/id id})
      (r/bad-request))))

(defn delete-todo-by
  "Retracts a todo note based on a condition."
  [{:keys [params]}]
  (let [cond-map (get-in params [:body :cond])
        result   (q/todos-delete-by! conn cond-map)]
    (r/ok result)))

(defn toggle-all
  "Toggles all notes to a completed or uncompleted state."
  [{:keys [params]}]
  (let [value  (get-in params [:body :value])
        result (q/todos-toggle-all! conn value)]
    (r/ok result)))
