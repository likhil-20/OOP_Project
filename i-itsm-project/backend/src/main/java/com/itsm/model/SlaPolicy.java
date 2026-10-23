package com.itsm.model;
import jakarta.persistence.*;

@Entity
@Table(name = "sla_policies")
public class SlaPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String priority; // P1, P2, P3, P4

    private int responseMinutes;
    private int resolutionMinutes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public int getResponseMinutes() { return responseMinutes; }
    public void setResponseMinutes(int responseMinutes) { this.responseMinutes = responseMinutes; }
    public int getResolutionMinutes() { return resolutionMinutes; }
    public void setResolutionMinutes(int resolutionMinutes) { this.resolutionMinutes = resolutionMinutes; }
}
