package com.itsm.model;
import jakarta.persistence.*;

@Entity
@Table(name = "system_configurations")
public class SystemConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String configKey; // ASSIGNMENT_STRATEGY (SKILL_BASED or LOAD_BASED), etc.
    private String configValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
}
