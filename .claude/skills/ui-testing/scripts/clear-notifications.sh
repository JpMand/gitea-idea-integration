#!/usr/bin/env bash
# Expires every IDE notification balloon, so none covers what the next click or screenshot targets.
"$(dirname "$0")/ui.py" js 'importClass(com.intellij.notification.NotificationsManager); importClass(com.intellij.notification.Notification); var ns = NotificationsManager.getNotificationsManager().getNotificationsOfType(Notification, null); for (var i=0;i<ns.length;i++) ns[i].expire(); "expired " + ns.length'
