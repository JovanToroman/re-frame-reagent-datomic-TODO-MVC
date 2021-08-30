(ns app.fx
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<! timeout]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [re-frame.core :refer [reg-fx dispatch reg-event-db trim-v subscribe]]
   [taoensso.timbre :refer-macros [debugf]]))

(defn api [{:keys [method
                   uri
                   params
                   on-success
                   on-error
                   delay] :as _req}]
  (let [http-fn (case method
                  :get http/get
                  :post http/post
                  :delete http/delete
                  :put http/put
                  http/post)]
    (go
     (when delay (<! (timeout (max delay 0))))
     (let [c (http-fn (str "http://localhost:8080" ; hardcoded, should be moved to shadow-cljs.edn
                           uri)
                      (cond-> {:with-credentials? false
                               :headers           {"Accept" "application/edn"}}
                              (and params (not= method :get)) (assoc :transit-params params)
                              (and params (= method :get)) (assoc :query-params params)))]
       (go
        (let [{:keys [status body success] :as _response} (<! c)]
          (if (= success true)
            (when on-success (dispatch (conj on-success body)))
            (when on-error (dispatch (conj on-error body status))))))))))

(reg-fx ::api api)