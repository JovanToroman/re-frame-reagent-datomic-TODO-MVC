(ns app.memory
  (:require [app.utils :as utils]
            [app.session :as session]
            [app.fx :as fx]
            [cljs-time.core :as time]
            [re-frame.core :refer [trim-v reg-event-fx reg-event-db reg-sub dispatch]]))

(defn save-todo [title]
  (let [trimmed-title (utils/trim-title @title)]
    (if-not (empty? trimmed-title)
      (dispatch [::create @title]))))

(defn update-todo [id title editing]
  (let [trimmed-title (utils/trim-title @title)]
    (if-not (empty? trimmed-title)
      (dispatch [::update-title id @title]))
    (reset! editing false)))

(defn on-key-down
  "Handles key-pressed event. Initiates save note for enter key and resets edit value for the escape key."
  ([k id title default editing]
   (let [key-pressed (.-which k)]
     (condp = key-pressed
       utils/enter-key (update-todo id title editing)
       utils/escape-key (do (reset! title default)
                            (reset! editing false))
       nil)))
  ([k title]
   (let [key-pressed (.-which k)]
     (condp = key-pressed
       utils/enter-key (save-todo title)
       nil))))

(defn toggle-todo [id]
  (swap! session/todos update-in [id :completed] not))

(defn toggle-all-todos [bool]
  (doseq [todo (utils/todos-all @session/todos)]
    (swap! session/todos assoc-in [(:id todo) :completed] (not bool))))

(defn clear-completed-todos [todos]
  ;TODO: implement
  )

(def chain-api-config ; TODO: maybe remove
  {:effect-present? :app.fx/api
   :get-dispatch    #(get-in % [:app.fx/api :on-success])
   :set-dispatch    (fn [effects dispatch] (assoc-in effects [:app.fx/api :on-success] dispatch))})

;; re-frame

; setup app
(reg-event-db
 ::set-now
 (fn [db]
   (assoc db :now (time/now))))

(defn boot [_]
  {:db {:name "todos-db"}
   #_#_:dispatch-interval {:dispatch [::set-now]
                           :id       :now-interval
                           :ms       5000}})

(reg-event-fx ::boot trim-v boot)

; business logic
(defn create
  "Persists a todo to the server."
  [_cofx [title]]
  {::fx/api {:uri        "/create"
             :method     :put
             :params     {:todo/title title}
             :on-success [::create-success]}})

(reg-event-fx ::create trim-v create)

(defn create-success
  "Dispatched on successful note creation."
  [db [new-note]]
  (update db :todos conj new-note))

(reg-event-db ::create-success trim-v create-success)

(defn update-title
  "Update a todo on the server."
  [_cofx [id title]]
  {::fx/api {:uri        "/update"
             :method     :post
             :params     {:data {:db/id      id
                                 :todo/title title}}
             :on-success [::update-title-success id]}})

(reg-event-fx ::update-title trim-v update-title)

(defn update-title-success
  "Dispatched on successful update of a note's title."
  [db [id updated-note]]
  (-> db
      (update :todos #(remove (comp #{id} :db/id) %))
      (update :todos conj updated-note)))

(reg-event-db ::update-title-success trim-v update-title-success)

(defn delete
  "Delete a todo."
  [_ [id]]
  {::fx/api {:uri        "/delete"
             :method     :delete
             :params     {:db/id id}
             :on-success [::delete-success id]}})

(reg-event-fx ::delete trim-v delete)

(defn delete-success
  "Removes note from app db."
  [db [id]]
  (update db :todos #(remove (comp #{id} :db/id) %)))

(reg-event-db ::delete-success trim-v delete-success)

(defn toggle
  "Toggle a todo's status."
  [_ [id completed]]
  {::fx/api {:uri        "/update"
             :method     :post
             :params     {:data {:db/id          id
                                 :todo/completed (not completed)}}
             :on-success [::toggle-success id]}})

(reg-event-fx ::toggle trim-v toggle)

(defn toggle-success
  "Updates note's status in the app db."
  [db [id updated-note]]
  (-> db
      (update :todos #(remove (comp #{id} :db/id) %))
      (update :todos conj updated-note)))

(reg-event-db ::toggle-success trim-v toggle-success)

(defn fetch-todos
  "Fetches all of the todo notes from the server."
  [_]
  {::fx/api {:uri        "/list"
             :method     :get
             :on-success [::fetch-todos-success]}})

(reg-event-fx ::fetch-todos fetch-todos)

(defn fetch-todos-success
  "Stores fetched todos in the app db."
  [db [todos]]
  (assoc db :todos todos))

(reg-event-db ::fetch-todos-success trim-v fetch-todos-success)


;; subscriptions


(reg-sub ::todos (fn [db _] (get db :todos)))