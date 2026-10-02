package model;

/**
 * Observer / Strategy interface for notification delivery.
 * NotificationService holds a list of channels and broadcasts to all of them.
 * Adding a new channel (Push, WhatsApp) requires only a new implementation —
 * NotificationService does not change.
 */
public interface NotificationChannel {
    void send(String recipient, String subject, String body);
}
