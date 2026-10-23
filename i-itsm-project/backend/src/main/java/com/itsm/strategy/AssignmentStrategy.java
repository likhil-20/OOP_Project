package com.itsm.strategy;
import com.itsm.model.Incident;
import com.itsm.model.User;
import java.util.List;

public interface AssignmentStrategy {
    User assignTechnician(Incident incident, List<User> technicians);
}
