package com.itsm.observer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IncidentEventPublisher {

    private final List<IncidentObserver> observers = new ArrayList<>();

    @Autowired
    public IncidentEventPublisher(List<IncidentObserver> found) {
        observers.addAll(found);
    }

    public void subscribe(IncidentObserver o) { observers.add(o); }
    public void unsubscribe(IncidentObserver o) { observers.remove(o); }

    public void publish(IncidentEvent event) {
        for (IncidentObserver o : observers) {
            o.update(event);
        }
    }
}