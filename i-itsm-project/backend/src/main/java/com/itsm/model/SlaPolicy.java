package com.itsm.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "sla_policies")
public class SlaPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "SLA priority is required.")
    @Pattern(regexp = "P[1-5]", message = "SLA priority must be P1, P2, P3, P4 or P5.")
    private String priority; // P1, P2, P3, P4

    @Min(value = 1, message = "Response minutes must be at least 1.")
    private int responseMinutes;

    @Min(value = 1, message = "Resolution minutes must be at least 1.")
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