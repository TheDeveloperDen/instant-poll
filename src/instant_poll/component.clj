(ns instant-poll.component
  (:require [clojure.string :as string]
            [instant-poll.poll :as polls]
            [instant-poll.interactions :refer [ephemeral-response normal-response update-message-response]]
            [instant-poll.state :refer [discord-conn config]]
            [discljord.messaging :as discord]
            [discljord.util :refer [parse-if-str]]
            [discljord.formatting :as discord-fmt]
            [discljord.permissions :as discord-perms]))

(def option-id-separator " ")
(def option-id-separator-pattern (re-pattern option-id-separator))
(def close-prefix "$$CLOSE$$")

(defn parse-custom-id [custom-id]
  (if (string/starts-with? custom-id close-prefix)
    [(subs custom-id (count close-prefix)) :close]
    (string/split custom-id option-id-separator-pattern 2)))

(defn make-components [{:keys [options id] :as poll}]
  [{:type 1
    :components
    (for [[key _] options]
      {:type 2
       :style 1
       :label key
       :custom_id (str id option-id-separator key)})}
   {:type 1
    :components
    [{:type 2
      :style 4
      :label "Close Poll"
      :custom_id (str close-prefix id)}]}])

(defn handle-button-press
  [{{{user-id :id} :user :keys [permissions]} :member {:keys [custom-id]} :data}]
  (let [[poll-id option] (parse-custom-id custom-id)]
    (if-let [{:keys [application-id interaction-token creator-id] :as poll} (polls/find-poll poll-id)]
      (if (= option :close)
        (if (or (= user-id creator-id) (discord-perms/has-permission-flag? :manage-messages (parse-if-str permissions)))
          (let [poll (polls/close-poll! poll-id)]
            (update-message-response
             {:content (str (polls/render-poll poll (:bar-length config)) \newline
                            (discord-fmt/mention-user user-id) " closed this poll.")
              :components []}))
          (ephemeral-response {:content "You do not have permission to close this poll."}))
        (let [updated-poll (polls/toggle-vote! poll-id user-id option)]
          (update-message-response {:content (polls/render-poll updated-poll (:bar-length config))})))
      (ephemeral-response {:content "This poll isn't active anymore."}))))
