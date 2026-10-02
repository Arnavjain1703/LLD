package model;

public class EmailChannel implements NotificationChannel {

    @Override
    public void send(String recipient, String subject, String body) {
        System.out.println("[EMAIL] To: " + recipient
                         + " | Subject: " + subject
                         + "\n        " + body);
    }
}
