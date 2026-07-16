package com.sda.mapper.ui.component;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton notification center that stores application notifications.
 * Accessible from any Vaadin UI view to add and retrieve notifications.
 */
@UIScope
@SpringComponent
public class NotificationCenter {

    private final List<NotificationItem> notifications = new ArrayList<>();

    /**
     * Adds a notification to the center.
     *
     * @param title   the notification title
     * @param message the notification message body
     * @param type    the notification severity type
     */
    public void addNotification(String title, String message, NotificationType type) {
        notifications.add(new NotificationItem(title, message, type, LocalDateTime.now()));
    }

    /**
     * Returns an unmodifiable view of all notifications, most recent last.
     *
     * @return list of notification items
     */
    public List<NotificationItem> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }

    /**
     * Returns the count of notifications.
     *
     * @return notification count
     */
    public int getCount() {
        return notifications.size();
    }

    /**
     * Notification severity type.
     */
    public enum NotificationType {
        SUCCESS, INFO, WARNING, ERROR
    }

    /**
     * A single notification item.
     *
     * @param title     the notification title
     * @param message   the notification message body
     * @param type      the notification severity type
     * @param timestamp when the notification was created
     */
    public record NotificationItem(String title, String message,
                                    NotificationType type, LocalDateTime timestamp) {
    }
}
