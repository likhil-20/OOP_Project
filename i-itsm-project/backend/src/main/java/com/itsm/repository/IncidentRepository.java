package com.itsm.repository;
import com.itsm.model.Incident;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // The entity graph loads reporter / technician / service in ONE query instead of many
    @Override
    @EntityGraph(attributePaths = {"reporter", "assignedTechnician", "affectedService"})
    List<Incident> findAll();

    @EntityGraph(attributePaths = {"reporter", "assignedTechnician", "affectedService"})
    List<Incident> findByReporterId(Long reporterId);

    @EntityGraph(attributePaths = {"reporter", "assignedTechnician", "affectedService"})
    List<Incident> findByAssignedTechnicianId(Long technicianId);

    List<Incident> findByStatusNot(String status);

    // ----- counts (no need to load every incident) -----
    long countByStatusNot(String status);
    long countByPriority(String priority);
    long countByStatusIn(Collection<String> statuses);

    @Query("select count(i) from Incident i where i.status not in :finished "
         + "and i.slaResolutionDeadline is not null and i.slaResolutionDeadline < :limit")
    long countOpenWithDeadlineBefore(@Param("finished") Collection<String> finished,
                                     @Param("limit") LocalDateTime limit);

    @Query("select i.assignedTechnician.id, count(i) from Incident i "
         + "where i.assignedTechnician is not null and i.status not in :finished "
         + "group by i.assignedTechnician.id")
    List<Object[]> countOpenByTechnician(@Param("finished") Collection<String> finished);

    @Query("select i.affectedService.id, count(i) from Incident i "
         + "where i.affectedService is not null group by i.affectedService.id")
    List<Object[]> countByService();

    // ----- targeted lists -----
    @EntityGraph(attributePaths = {"reporter", "assignedTechnician", "affectedService"})
    @Query("select i from Incident i where i.status not in :finished "
         + "and i.slaResolutionDeadline is not null and i.slaResolutionDeadline < :limit")
    List<Incident> findOpenWithDeadlineBefore(@Param("finished") Collection<String> finished,
                                              @Param("limit") LocalDateTime limit);

    @EntityGraph(attributePaths = {"reporter", "assignedTechnician", "affectedService"})
    @Query("select i from Incident i where i.status not in :finished "
         + "and (i.escalated is null or i.escalated = false)")
    List<Incident> findEscalationCandidates(@Param("finished") Collection<String> finished);

    @EntityGraph(attributePaths = {"affectedService"})
    @Query("select i from Incident i where i.status <> 'CLOSED' "
         + "and upper(i.category) = upper(:category) and i.priority = :priority "
         + "and i.affectedService.id = :serviceId")
    List<Incident> findDuplicateCandidates(@Param("category") String category,
                                           @Param("priority") String priority,
                                           @Param("serviceId") Long serviceId);

    List<Incident> findTop200ByResolutionDetailsIsNotNullOrderByIdDesc();
}