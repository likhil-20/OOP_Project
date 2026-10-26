-- Run AFTER the backend has started once (it seeds users, services, categories, SLA, config).

-- Extra user waiting for admin approval
INSERT INTO users (name, email, password, role, skills, current_load, approved)
VALUES ('Carol Pending', 'carol@gmail.com', 'carol123', 'EMPLOYEE', NULL, 0, FALSE)
ON CONFLICT (email) DO NOTHING;

-- Incidents
INSERT INTO incidents
 (reporter_id, category, description, affected_service_id, impact, urgency, priority, status,
  assigned_technician_id, sla_response_deadline, sla_resolution_deadline,
  resolution_details, feedback_rating, feedback_comments, is_duplicate, created_at)
VALUES
-- P1, in progress
((SELECT id FROM users WHERE email='employee@gmail.com'), 'APPLICATION',
 'Payment page returns HTTP 500 during checkout',
 (SELECT id FROM it_services WHERE service_name='Payment Application'),
 'HIGH', 'HIGH', 'P1', 'IN_PROGRESS',
 (SELECT id FROM users WHERE email='tech.alice@gmail.com'),
 NOW() + INTERVAL '5 minutes', NOW() + INTERVAL '30 minutes',
 NULL, NULL, NULL, FALSE, NOW()),

-- P2, resolved and verified with feedback
((SELECT id FROM users WHERE email='employee@gmail.com'), 'AUTHENTICATION',
 'Cannot sign in using SSO since morning',
 (SELECT id FROM it_services WHERE service_name='Authentication Service'),
 'HIGH', 'MEDIUM', 'P2', 'VERIFIED',
 (SELECT id FROM users WHERE email='tech.alice@gmail.com'),
 NOW() - INTERVAL '3 hours', NOW() - INTERVAL '1 hour',
 'Expired OAuth signing certificate was renewed and the service restarted.',
 5, 'Quick fix, thanks!', FALSE, NOW() - INTERVAL '4 hours'),

-- P4, newly assigned
((SELECT id FROM users WHERE email='employee@gmail.com'), 'SOFTWARE',
 'HR portal dashboard loads slowly',
 (SELECT id FROM it_services WHERE service_name='Internal HR Portal'),
 'LOW', 'LOW', 'P4', 'ASSIGNED',
 (SELECT id FROM users WHERE email='tech.bob@gmail.com'),
 NOW() + INTERVAL '4 hours', NOW() + INTERVAL '24 hours',
 NULL, NULL, NULL, FALSE, NOW());

-- Audit trail
INSERT INTO audit_logs (incident_id, user_email, action_performed, previous_state, new_state, description, "timestamp")
VALUES
((SELECT id FROM incidents WHERE description='Payment page returns HTTP 500 during checkout'),
 'employee@gmail.com', 'INCIDENT_CREATED', 'NEW', 'ASSIGNED', 'Created incident with priority P1', NOW()),
((SELECT id FROM incidents WHERE description='Payment page returns HTTP 500 during checkout'),
 'tech.alice@gmail.com', 'STATUS_UPDATE', 'ASSIGNED', 'IN_PROGRESS', 'Updated status to IN_PROGRESS', NOW()),
((SELECT id FROM incidents WHERE description='Cannot sign in using SSO since morning'),
 'tech.alice@gmail.com', 'STATUS_UPDATE', 'IN_PROGRESS', 'RESOLVED', 'Updated status to RESOLVED', NOW() - INTERVAL '2 hours'),
((SELECT id FROM incidents WHERE description='Cannot sign in using SSO since morning'),
 'employee@gmail.com', 'STATUS_UPDATE', 'RESOLVED', 'VERIFIED', 'Updated status to VERIFIED', NOW() - INTERVAL '1 hour');

-- Notifications
INSERT INTO notifications (recipient_email, message, is_read, created_at)
VALUES
('employee@gmail.com',   'Incident #1 created (P1)', FALSE, NOW()),
('tech.alice@gmail.com', 'Assigned Incident #1 (P1)', FALSE, NOW()),
('employee@gmail.com',   'Incident #2 status: VERIFIED', TRUE, NOW() - INTERVAL '1 hour');