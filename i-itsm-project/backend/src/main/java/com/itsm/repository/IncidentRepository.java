package com.itsm.repository;
import com.itsm.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByReporterId(Long reporterId);
    List<Incident> findByAssignedTechnicianId(Long technicianId);
    List<Incident> findByStatusNot(String status);
}
