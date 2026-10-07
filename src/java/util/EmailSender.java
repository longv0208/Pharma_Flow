package util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * SMTP sender via Gmail — adapted from the reference project's EmailSender,
 * ported to jakarta.mail (Tomcat 10) and PharmaFlow branding.
 *
 * TODO: replace SENDER_EMAIL / SENDER_PASSWORD with the real Gmail address +
 * 16-char App Password (Google Account → Security → 2-Step Verification → App
 * passwords). Never commit real creds.
 */
public final class EmailSender {

    private static final String SENDER_EMAIL = "thomasxh004@gmail.com";
    private static final String SENDER_PASSWORD = "kwys cguq sdzx yxlh";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587"; // STARTTLS

    private EmailSender() {
    }

    /**
     * Verification code sent right after register.
     */
    public static boolean sendVerificationEmail(String recipientEmail, String code, String fullName) {
        String subject = "Verify your email - PharmaFlow";
        String content = wrap(
                "PharmaFlow - Email Verification",
                "Hi <b>" + esc(fullName) + "</b>,",
                "Thanks for creating a PharmaFlow account. Enter the code below to verify your email and activate your account.",
                code,
                "This code expires in 15 minutes. If you did not sign up, you can ignore this email.");
        return send(recipientEmail, subject, content);
    }

    /**
     * OTP code for the forgot-password flow.
     */
    public static boolean sendResetPasswordEmail(String recipientEmail, String code, String fullName) {
        String subject = "Reset your password - PharmaFlow";
        String content = wrap(
                "PharmaFlow - Password Reset",
                "Hi <b>" + esc(fullName) + "</b>,",
                "We received a request to reset the password for your account. Enter the code below to continue.",
                code,
                "This code expires in 15 minutes. If you did not request a reset, you can ignore this email - your password stays unchanged.");
        return send(recipientEmail, subject, content);
    }

    /* ==================== internals ==================== */
    private static boolean send(String to, String subject, String html) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            java.util.logging.Logger.getLogger(EmailSender.class.getName())
                    .log(java.util.logging.Level.SEVERE, "send mail to " + to + " failed", e);
            return false;
        }
    }

    /**
     * Shared HTML shell — white card, sky-blue accent (matches site theme).
     */
    private static String wrap(String heading, String greeting, String body,
            String code, String footer) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;"
                + "padding:24px;border:1px solid #e2e8f0;border-radius:12px'>"
                + "<h2 style='color:#0284c7;text-align:center;margin:0 0 16px'>" + heading + "</h2>"
                + "<p>" + greeting + "</p>"
                + "<p>" + body + "</p>"
                + "<p style='text-align:center;margin:24px 0'>"
                + "<span style='display:inline-block;font-size:28px;font-weight:bold;letter-spacing:6px;"
                + "color:#0c4a6e;background:#f0f9ff;border:1px solid #bae6fd;border-radius:8px;padding:12px 24px'>"
                + code + "</span></p>"
                + "<p style='color:#64748b;font-size:13px'>" + footer + "</p>"
                + "<p>Regards,<br>The PharmaFlow team</p>"
                + "</div>";
    }

    /**
     * Minimal HTML escaping for user-supplied names inside the template.
     */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
