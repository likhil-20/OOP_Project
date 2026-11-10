package com.itsm.repository;
import com.itsm.model.SlaViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SlaViolationRepository extends JpaRepository<SlaViolation, Long> {
    List<SlaViolation> findAllByOrderByDetectedAtDesc();
}
