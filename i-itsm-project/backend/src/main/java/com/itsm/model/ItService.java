package com.itsm.model;
import jakarta.persistence.*;

@Entity
@Table(name = "it_services")
public class ItService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String businessCriticality; // HIGH, MEDIUM, LOW

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
