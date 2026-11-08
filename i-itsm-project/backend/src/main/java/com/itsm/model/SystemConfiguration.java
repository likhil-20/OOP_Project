package com.itsm.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "system_configurations")
public class SystemConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Config key is required.")
    @Size(max = 100, message = "Config key must be at most 100 characters.")
    private String configKey; // ASSIGNMENT_STRATEGY (SKILL_BASED or LOAD_BASED), etc.

    @NotBlank(message = "Config value is required.")
    @Size(max = 255, message = "Config value must be at most 255 characters.")
    private String configValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
}