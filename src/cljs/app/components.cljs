(ns ^{:figwheel-hooks true} app.components
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [app.memory :as mem]
            [app.utils :as utils]
            [app.session :as session]))


(defn selected-class [display-type todos-display-type]
  (if (= display-type
         todos-display-type)
    "selected" ""))

(defn filters []
  [:ul.filters
   [:li [:a {:class (selected-class :all @session/todos-display-type) :href "#/"} "All"]]
   [:li [:a {:class (selected-class :active @session/todos-display-type) :href "#/active"} "Active"]]
   [:li [:a {:class (selected-class :completed @session/todos-display-type) :href "#/completed"} "Completed"]]])

(defn items-count
  "A counter showing hom many todo items there are."
  []
  (let [active-count (count (utils/todos-active @session/todos))]
    [:span.todo-count
     [:strong active-count]
     (str (if (= 1 active-count) " item " " items ")
          "left")]))

(defn clear-button
  "Button to clear all completed todos."
  []
  [:button.clear-completed {:on-click #(mem/clear-completed-todos @session/todos)
                            :style    {:display (utils/display-elem (utils/todos-any-completed?
                                                                     @session/todos))}}
   "Clear completed"])

(defn create
  "Create a todo."
  []
  (let [title   (r/atom "")]
    (r/create-class
     {:reagent-render       (fn [] ; mopzda ovde i let treba u render
                              [:input.new-todo {:type        "text"
                                                :value       @title
                                                :placeholder "What needs to be done?"
                                                :on-change   #(reset! title (-> % .-target .-value))
                                                :on-key-down #(mem/on-key-down % title)}])
      :component-did-update #(.focus (rd/dom-node %))})))

(defn edit
  "Edit a todo."
  [{:keys [db/id todo/title]} editing]
  (let [default    (or title "")
        edit-title (r/atom default)]
    (r/create-class
     {:reagent-render       (fn [] ; mopzda ovde i let treba u render
                              [:input.edit {:type        "text"
                                            :style       {:display (utils/display-elem @editing)}
                                            :value       @edit-title
                                            :on-change   #(reset! edit-title (-> % .-target .-value))
                                            :on-blur     #(mem/update-todo id edit-title editing)
                                            :on-key-down #(mem/on-key-down % id edit-title default editing)}])
      :component-did-update #(.focus (rd/dom-node %))})))

(defn item
  "A single todo item."
  [_todo]
  (let [editing (r/atom false)]
    (fn [{:keys [db/id todo/title todo/completed] :as todo}]
      [:li {:class (str (when completed "completed ")
                        (when @editing "editing"))
            :style {:display (utils/display-item
                              (utils/todo-display-filter completed @session/todos-display-type))}}
       [:div.view
        [:input.toggle {:type      "checkbox"
                        :checked   completed
                        :on-change #(rf/dispatch [::mem/toggle id completed])}]
        [:label {:on-double-click #(reset! editing true)} title]
        [:button.destroy {:on-click #(rf/dispatch [::mem/delete id])}]]
       [edit todo editing]])))

(defn todos-list
  "List of all todo items."
  [todos]
  [:ul.todo-list
   (for [todo todos]
     ^{:key (str (:todo/created todo))}
     [item todo])])

(defn todos-toggle
  "Marks all todos as completed."
  []
  [:span
   [:input#toggle-all.toggle-all {:type      "checkbox"
                                  :checked   (utils/todos-all-completed? @session/todos)
                                  :on-change #(mem/toggle-all-todos
                                               (utils/todos-all-completed? @session/todos))}]
   [:label {:for "toggle-all"} "Mark all as complete"]])

(defn content
  "Main content of the TODO MVC view."
  []
  (let [todos @(rf/subscribe [::mem/todos])]
    [:div
     [:section.todoapp
      [:header.header
       [:h1 "TODO MVC"]
       [create]]
      [:div {:style
             {:display (utils/display-elem (utils/todos-any? todos))}}
       [:section.main

        [todos-toggle]
        [todos-list todos #_(utils/todos-all )]]
       [:footer.footer
        [items-count]
        [filters]
        [clear-button]
        ]]]]))

; render the app
(defn mount-root []
  (rf/clear-subscription-cache!)
  (rd/render [content]
            (js/document.getElementById "app")))

(defn ^:export re-render
  "Re-render the app when figwheel reloads"
  []
  (mount-root))

(defn ^:export run []
  (rf/dispatch-sync [::mem/boot])
  #_(rf/configure! [mem/chain-api-config])
  (rf/dispatch [::mem/fetch-todos])
  (mount-root))