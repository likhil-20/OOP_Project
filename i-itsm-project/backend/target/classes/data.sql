INSERT INTO it_services (id, service_name, business_criticality, description) VALUES 
(1, 'Payment Application', 'HIGH', 'Core transaction processing service'),
(2, 'Authentication Service', 'HIGH', 'OAuth and SSO provider'),
(3, 'Internal HR Portal', 'LOW', 'Employee records')
ON CONFLICT (id) DO NOTHING;
