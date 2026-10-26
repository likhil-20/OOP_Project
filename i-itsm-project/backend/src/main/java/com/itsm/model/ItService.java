package com.itsm.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "it_services")
public class ItService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Service name is required.")
    @Size(max = 100, message = "Service name must be at most 100 characters.")
    private String serviceName;

    @Column(nullable = false)
    @NotBlank(message = "Business criticality is required.")
    @Pattern(regexp = "HIGH|MEDIUM|LOW", message = "Business criticality must be HIGH, MEDIUM or LOW.")
    private String businessCriticality; // HIGH, MEDIUM, LOW

    @Size(max = 255, message = "Service description must be at most 255 characters.")
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getBusinessCriticality() { return businessCriticality; }
    public void setBusinessCriticality(String businessCriticality) { this.businessCriticality = businessCriticality; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}