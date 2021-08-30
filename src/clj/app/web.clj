(ns app.web
  (:require [clojure.spec.alpha :as s]
            [muuntaja.core :as m]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.cors :refer [wrap-cors]]
            [taoensso.timbre :refer [debug spy]]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.cors :refer [wrap-cors]]
            [app.handlers :as h]))

(def app
  (ring/ring-handler
   (ring/router
    ["" {:swagger    {:consumes ["application/edn" "application/transit+json"]
                      :produces ["application/edn" "application/transit+json"]}}
     ["/list" {:get {:handler h/get-items}}]
     ["/create" {:put {:handler h/create-todo
                       :parameters {:body {:todo/title string?}}}}]
     ["/update" {:post {:handler h/update-todo
                        :parameters {:body {:data map?}}}}]
     ["/delete" {:delete {:handler h/delete-todo
                          :parameters {:body {:db/id number?}}}}]
     ["/delete-by" {:delete {:handler h/delete-todo-by}}]
     ["/toggle" {:post {:handler h/toggle-all}}]]
    {:conflicts identity
     :data      {:coercion   reitit.coercion.spec/coercion
                 :muuntaja   m/instance
                 :middleware [parameters/parameters-middleware
                              muuntaja/format-negotiate-middleware
                              muuntaja/format-response-middleware
                              muuntaja/format-request-middleware
                              coercion/coerce-response-middleware
                              coercion/coerce-request-middleware
                              [wrap-cors :access-control-allow-origin #".*"
                               :access-control-allow-methods [:get :put :post :delete]]]}})
   (ring/create-default-handler)))