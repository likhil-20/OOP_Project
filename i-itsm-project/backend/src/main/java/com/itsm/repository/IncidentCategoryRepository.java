package com.itsm.repository;
import com.itsm.model.IncidentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {
    boolean existsByName(String name);
}
