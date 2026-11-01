package com.itsm.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "affected_service_id")
    private ItService affectedService;

    @Column(nullable = false)
    private String impact; // HIGH, MEDIUM, LOW

    @Column(nullable = false)
    private String urgency; // HIGH, MEDIUM, LOW

    private String priority; // P1, P2, P3, P4
    private String status = "NEW"; // NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED, ESCALATED

    @ManyToOne
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician;

    private LocalDateTime slaResponseDeadline;
    private LocalDateTime slaResolutionDeadline;

    @Column(columnDefinition = "TEXT")
    private String investigationNotes;

    @Column(columnDefinition = "TEXT")
    private String resolutionDetails;

    @Column(columnDefinition = "TEXT")
    private String additionalInfo; // Employee additional info / responses to tech

    @Column(columnDefinition = "TEXT")
    private String techRequestInfo; // Tech requesting more info from employee

    private Integer feedbackRating; // 1 to 5 stars
    @Column(columnDefinition = "TEXT")
    private String feedbackComments;

    private boolean isDuplicate = false;
    private Long duplicateOfIncidentId;
    private Boolean escalated = false;
    private LocalDateTime escalatedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ItService getAffectedService() { return affectedService; }
    public void setAffectedService(ItService affectedService) { this.affectedService = affectedService; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public User getAssignedTechnician() { return assignedTechnician; }
    public void setAssignedTechnician(User assignedTechnician) { this.assignedTechnician = assignedTechnician; }
    public LocalDateTime getSlaResponseDeadline() { return slaResponseDeadline; }
    public void setSlaResponseDeadline(LocalDateTime slaResponseDeadline) { this.slaResponseDeadline = slaResponseDeadline; }
    public LocalDateTime getSlaResolutionDeadline() { return slaResolutionDeadline; }
    public void setSlaResolutionDeadline(LocalDateTime slaResolutionDeadline) { this.slaResolutionDeadline = slaResolutionDeadline; }
    public String getInvestigationNotes() { return investigationNotes; }
    public void setInvestigationNotes(String investigationNotes) { this.investigationNotes = investigationNotes; }
    public String getResolutionDetails() { return resolutionDetails; }
    public void setResolutionDetails(String resolutionDetails) { this.resolutionDetails = resolutionDetails; }
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    public String getTechRequestInfo() { return techRequestInfo; }
    public void setTechRequestInfo(String techRequestInfo) { this.techRequestInfo = techRequestInfo; }
    public Integer getFeedbackRating() { return feedbackRating; }
    public void setFeedbackRating(Integer feedbackRating) { this.feedbackRating = feedbackRating; }
    public String getFeedbackComments() { return feedbackComments; }
    public void setFeedbackComments(String feedbackComments) { this.feedbackComments = feedbackComments; }
    public boolean isDuplicate() { return isDuplicate; }
    public void setDuplicate(boolean duplicate) { isDuplicate = duplicate; }
    public Long getDuplicateOfIncidentId() { return duplicateOfIncidentId; }
    public void setDuplicateOfIncidentId(Long duplicateOfIncidentId) { this.duplicateOfIncidentId = duplicateOfIncidentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getEscalated() { return escalated; }
    public void setEscalated(Boolean escalated) { this.escalated = escalated; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
}
