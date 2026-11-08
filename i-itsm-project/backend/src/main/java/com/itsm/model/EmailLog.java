package com.itsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs", indexes = @Index(name = "idx_email_recipient", columnList = "recipientEmail"))
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipientEmail;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private LocalDateTime sentAt = LocalDateTime.now();
    private Boolean opened = false;

    public Long getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public LocalDateTime getSentAt() { return sentAt; }
    public Boolean getOpened() { return opened; }
    public void setOpened(Boolean opened) { this.opened = opened; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}