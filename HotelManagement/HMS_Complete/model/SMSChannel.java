package model;

public class SMSChannel implements NotificationChannel {

    @Override
    public void send(String recipient, String subject, String body) {
        // SMS: subject is ignored, body is truncated
        String sms = body.length() > 80 ? body.substring(0, 80) + "..." : body;
        System.out.println("[SMS]   To: " + recipient + " | " + sms);
    }
}
