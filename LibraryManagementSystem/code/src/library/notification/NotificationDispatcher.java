package library.notification;

import library.model.Member;

/**
 * Minimal dispatcher for now — prints to console.
 * Step 7 replaces the body with channel-based routing (EMAIL, SMS).
 */
public class NotificationDispatcher {

    public void dispatch(Member member, String message) {
        System.out.println("[NOTIFY → " + member.getEmail() + "]: " + message);
    }
}
