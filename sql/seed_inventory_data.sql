-- Sample data for the IT Asset and Software Exposure Inventory.
-- The inserts are safe to re-run.

INSERT INTO dev.teams (name, contact_email) VALUES
    ('Platform Engineering', 'platform@example.edu'),
    ('Endpoint Operations', 'endpoints@example.edu'),
    ('Security Operations', 'security@example.edu'),
    ('Finance Systems', 'finance-systems@example.edu')
ON CONFLICT (name) DO UPDATE SET contact_email = EXCLUDED.contact_email;

INSERT INTO dev.applications (vendor, product_name, third_party, owner_team_id, lifecycle)
SELECT seed.vendor, seed.product_name, seed.third_party, team.team_id, seed.lifecycle
FROM (VALUES
    ('Mozilla', 'Firefox', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('Google', 'Chrome', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('Microsoft', 'Visual Studio Code', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('Docker', 'Docker Desktop', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('Docker', 'Docker Engine', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('Eclipse Adoptium', 'Temurin JDK', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('PostgreSQL Global Development Group', 'PostgreSQL Client', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('Python Software Foundation', 'Python', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('OpenJS Foundation', 'Node.js', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('OpenBSD', 'OpenSSH', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('The Git Project', 'Git', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('curl Project', 'curl', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('The Document Foundation', 'LibreOffice', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('Microsoft', 'Microsoft Edge', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('Microsoft', 'Microsoft 365 Apps', TRUE, 'Finance Systems', 'ACTIVE'),
    ('7-Zip', '7-Zip', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('Simon Tatham', 'PuTTY', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('Apple', 'Xcode Command Line Tools', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('Homebrew', 'Homebrew', TRUE, 'Platform Engineering', 'ACTIVE'),
    ('George Nachman', 'iTerm2', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('Salesforce', 'Slack', TRUE, 'Endpoint Operations', 'ACTIVE'),
    ('Postman', 'Postman', TRUE, 'Platform Engineering', 'ACTIVE')
) AS seed(vendor, product_name, third_party, team_name, lifecycle)
JOIN dev.teams team ON team.name = seed.team_name
ON CONFLICT (vendor, product_name) DO NOTHING;

INSERT INTO dev.application_versions (application_id, version, released_at, support_ends_at)
SELECT application.application_id, seed.version, seed.released_at::date, seed.support_ends_at::date
FROM (VALUES
    ('Mozilla', 'Firefox', '126.0', '2024-06-11', NULL),
    ('Google', 'Chrome', '125.0.6422.142', '2024-05-21', NULL),
    ('Microsoft', 'Visual Studio Code', '1.90.1', '2024-06-06', NULL),
    ('Docker', 'Docker Desktop', '4.31.1', '2024-06-06', NULL),
    ('Docker', 'Docker Engine', '26.1.3', '2024-05-16', NULL),
    ('Eclipse Adoptium', 'Temurin JDK', '21.0.3+9', '2024-04-16', '2031-09-30'),
    ('PostgreSQL Global Development Group', 'PostgreSQL Client', '16.3', '2024-05-09', '2028-11-09'),
    ('Python Software Foundation', 'Python', '3.12.3', '2024-04-09', '2028-10-31'),
    ('OpenJS Foundation', 'Node.js', '20.14.0', '2024-05-28', '2026-04-30'),
    ('OpenBSD', 'OpenSSH', '9.7p1', '2024-03-11', NULL),
    ('The Git Project', 'Git', '2.45.1', '2024-05-14', NULL),
    ('curl Project', 'curl', '8.7.1', '2024-05-22', NULL),
    ('The Document Foundation', 'LibreOffice', '24.2.4', '2024-06-06', NULL),
    ('Microsoft', 'Microsoft Edge', '125.0.2535.92', '2024-05-23', NULL),
    ('Microsoft', 'Microsoft 365 Apps', '2405', '2024-05-28', NULL),
    ('7-Zip', '7-Zip', '24.07', '2024-06-19', NULL),
    ('Simon Tatham', 'PuTTY', '0.81', '2024-04-15', NULL),
    ('Apple', 'Xcode Command Line Tools', '15.4', '2024-05-13', NULL),
    ('Homebrew', 'Homebrew', '4.3.5', '2024-06-10', NULL),
    ('George Nachman', 'iTerm2', '3.5.1', '2024-05-28', NULL),
    ('Salesforce', 'Slack', '4.39.95', '2024-06-04', NULL),
    ('Postman', 'Postman', '11.1.14', '2024-05-30', NULL)
) AS seed(vendor, product_name, version, released_at, support_ends_at)
JOIN dev.applications application
  ON application.vendor = seed.vendor AND application.product_name = seed.product_name
ON CONFLICT (application_id, version) DO NOTHING;

INSERT INTO dev.computers (
    hostname, operating_system, operating_system_version, environment, owner_team_id,
    internet_facing, criticality, last_seen_at
)
SELECT seed.hostname, seed.operating_system, seed.operating_system_version,
       seed.environment, team.team_id, seed.internet_facing,
       seed.criticality, CURRENT_TIMESTAMP - seed.last_seen_offset
FROM (VALUES
    ('linux-build-01', 'Linux', 'Ubuntu 24.04 LTS', 'DEVELOPMENT', 'Platform Engineering', FALSE, 'HIGH', INTERVAL '3 minutes'),
    ('linux-api-01', 'Linux', 'Ubuntu 22.04 LTS', 'TEST', 'Platform Engineering', TRUE, 'CRITICAL', INTERVAL '7 minutes'),
    ('linux-monitor-01', 'Linux', 'Rocky Linux 9.4', 'PRODUCTION', 'Security Operations', TRUE, 'HIGH', INTERVAL '18 minutes'),
    ('win-finance-01', 'Windows', 'Windows 11 Pro 23H2', 'PRODUCTION', 'Finance Systems', FALSE, 'HIGH', INTERVAL '11 minutes'),
    ('win-helpdesk-01', 'Windows', 'Windows 11 Pro 23H2', 'TEST', 'Endpoint Operations', FALSE, 'MEDIUM', INTERVAL '26 minutes'),
    ('win-web-01', 'Windows', 'Windows Server 2022', 'STAGING', 'Platform Engineering', TRUE, 'CRITICAL', INTERVAL '5 minutes'),
    ('mac-design-01', 'macOS', 'macOS 14.5', 'DEVELOPMENT', 'Endpoint Operations', FALSE, 'MEDIUM', INTERVAL '16 minutes'),
    ('mac-platform-01', 'macOS', 'macOS 14.5', 'DEVELOPMENT', 'Platform Engineering', FALSE, 'HIGH', INTERVAL '9 minutes'),
    ('mac-executive-01', 'macOS', 'macOS 14.4', 'PRODUCTION', 'Finance Systems', FALSE, 'HIGH', INTERVAL '42 minutes')
) AS seed(hostname, operating_system, operating_system_version, environment, team_name, internet_facing, criticality, last_seen_offset)
JOIN dev.teams team ON team.name = seed.team_name
ON CONFLICT (hostname) DO NOTHING;

-- Each endpoint receives twelve familiar applications appropriate to its operating system.
INSERT INTO dev.software_installations (computer_id, application_version_id, status, discovered_at)
SELECT computer.computer_id, version.application_version_id, 'INSTALLED',
       CURRENT_TIMESTAMP - plan.discovery_offset
FROM (VALUES
    ('Linux', 'Mozilla', 'Firefox', '126.0', INTERVAL '2 days'),
    ('Linux', 'Google', 'Chrome', '125.0.6422.142', INTERVAL '3 days'),
    ('Linux', 'Microsoft', 'Visual Studio Code', '1.90.1', INTERVAL '3 days'),
    ('Linux', 'Docker', 'Docker Engine', '26.1.3', INTERVAL '7 days'),
    ('Linux', 'Eclipse Adoptium', 'Temurin JDK', '21.0.3+9', INTERVAL '7 days'),
    ('Linux', 'PostgreSQL Global Development Group', 'PostgreSQL Client', '16.3', INTERVAL '5 days'),
    ('Linux', 'Python Software Foundation', 'Python', '3.12.3', INTERVAL '3 days'),
    ('Linux', 'OpenJS Foundation', 'Node.js', '20.14.0', INTERVAL '4 days'),
    ('Linux', 'OpenBSD', 'OpenSSH', '9.7p1', INTERVAL '14 days'),
    ('Linux', 'The Git Project', 'Git', '2.45.1', INTERVAL '9 days'),
    ('Linux', 'curl Project', 'curl', '8.7.1', INTERVAL '14 days'),
    ('Linux', 'The Document Foundation', 'LibreOffice', '24.2.4', INTERVAL '30 days'),
    ('Windows', 'Mozilla', 'Firefox', '126.0', INTERVAL '2 days'),
    ('Windows', 'Google', 'Chrome', '125.0.6422.142', INTERVAL '2 days'),
    ('Windows', 'Microsoft', 'Visual Studio Code', '1.90.1', INTERVAL '4 days'),
    ('Windows', 'Docker', 'Docker Desktop', '4.31.1', INTERVAL '8 days'),
    ('Windows', 'Eclipse Adoptium', 'Temurin JDK', '21.0.3+9', INTERVAL '8 days'),
    ('Windows', 'Python Software Foundation', 'Python', '3.12.3', INTERVAL '4 days'),
    ('Windows', 'OpenJS Foundation', 'Node.js', '20.14.0', INTERVAL '4 days'),
    ('Windows', 'The Git Project', 'Git', '2.45.1', INTERVAL '10 days'),
    ('Windows', 'Microsoft', 'Microsoft Edge', '125.0.2535.92', INTERVAL '1 day'),
    ('Windows', '7-Zip', '7-Zip', '24.07', INTERVAL '8 days'),
    ('Windows', 'Microsoft', 'Microsoft 365 Apps', '2405', INTERVAL '12 days'),
    ('Windows', 'Simon Tatham', 'PuTTY', '0.81', INTERVAL '15 days'),
    ('macOS', 'Mozilla', 'Firefox', '126.0', INTERVAL '2 days'),
    ('macOS', 'Google', 'Chrome', '125.0.6422.142', INTERVAL '3 days'),
    ('macOS', 'Microsoft', 'Visual Studio Code', '1.90.1', INTERVAL '2 days'),
    ('macOS', 'Docker', 'Docker Desktop', '4.31.1', INTERVAL '7 days'),
    ('macOS', 'Eclipse Adoptium', 'Temurin JDK', '21.0.3+9', INTERVAL '7 days'),
    ('macOS', 'Python Software Foundation', 'Python', '3.12.3', INTERVAL '3 days'),
    ('macOS', 'OpenJS Foundation', 'Node.js', '20.14.0', INTERVAL '3 days'),
    ('macOS', 'The Git Project', 'Git', '2.45.1', INTERVAL '5 days'),
    ('macOS', 'Apple', 'Xcode Command Line Tools', '15.4', INTERVAL '10 days'),
    ('macOS', 'Homebrew', 'Homebrew', '4.3.5', INTERVAL '1 day'),
    ('macOS', 'George Nachman', 'iTerm2', '3.5.1', INTERVAL '14 days'),
    ('macOS', 'Salesforce', 'Slack', '4.39.95', INTERVAL '2 days')
) AS plan(operating_system, vendor, product_name, version, discovery_offset)
JOIN dev.computers computer ON computer.operating_system = plan.operating_system
JOIN dev.applications application
  ON application.vendor = plan.vendor AND application.product_name = plan.product_name
JOIN dev.application_versions version
  ON version.application_id = application.application_id AND version.version = plan.version
ON CONFLICT (computer_id, application_version_id) DO NOTHING;

INSERT INTO dev.patches (
    affected_version_id, fixed_version_id, advisory_reference, severity, available_at, description
)
SELECT affected.application_version_id, fixed.application_version_id, seed.advisory_reference,
       seed.severity, seed.available_at::timestamptz, seed.description
FROM (VALUES
    ('Mozilla', 'Firefox', '126.0', 'Mozilla', 'Firefox', '126.0', 'ADV-2026-001', 'HIGH', '2026-08-10 09:00:00+00', 'Browser security update pending verification.'),
    ('OpenBSD', 'OpenSSH', '9.7p1', 'OpenBSD', 'OpenSSH', '9.7p1', 'ADV-2026-002', 'CRITICAL', '2026-08-21 12:00:00+00', 'Network-facing SSH service requires urgent patch review.'),
    ('Docker', 'Docker Engine', '26.1.3', 'Docker', 'Docker Engine', '26.1.3', 'ADV-2026-003', 'HIGH', '2026-08-25 15:30:00+00', 'Container runtime update is available.'),
    ('Microsoft', 'Microsoft Edge', '125.0.2535.92', 'Microsoft', 'Microsoft Edge', '125.0.2535.92', 'ADV-2026-004', 'MEDIUM', '2026-08-28 08:00:00+00', 'Browser update is available through managed endpoint policy.'),
    ('Eclipse Adoptium', 'Temurin JDK', '21.0.3+9', 'Eclipse Adoptium', 'Temurin JDK', '21.0.3+9', 'ADV-2026-005', 'HIGH', '2026-09-01 10:15:00+00', 'Java runtime update requires application compatibility validation.')
) AS seed(affected_vendor, affected_product, affected_version, fixed_vendor, fixed_product, fixed_version, advisory_reference, severity, available_at, description)
JOIN dev.applications affected_application ON affected_application.vendor = seed.affected_vendor AND affected_application.product_name = seed.affected_product
JOIN dev.application_versions affected ON affected.application_id = affected_application.application_id AND affected.version = seed.affected_version
JOIN dev.applications fixed_application ON fixed_application.vendor = seed.fixed_vendor AND fixed_application.product_name = seed.fixed_product
JOIN dev.application_versions fixed ON fixed.application_id = fixed_application.application_id AND fixed.version = seed.fixed_version
ON CONFLICT (advisory_reference, affected_version_id) DO NOTHING;

INSERT INTO dev.security_observations (computer_id, patch_id, severity, status, observed_at, details)
SELECT computer.computer_id, patch.patch_id, seed.severity, seed.status,
       seed.observed_at::timestamptz, seed.details
FROM (VALUES
    ('linux-api-01', 'ADV-2026-002', 'CRITICAL', 'OPEN', '2026-09-06 08:30:00+00', 'Internet-facing API host has an outstanding OpenSSH advisory.'),
    ('linux-api-01', 'ADV-2026-003', 'HIGH', 'ACKNOWLEDGED', '2026-09-06 09:00:00+00', 'Docker Engine update scheduled for the next maintenance window.'),
    ('win-web-01', 'ADV-2026-005', 'HIGH', 'OPEN', '2026-09-06 11:00:00+00', 'Staging web host requires Java runtime validation before patching.'),
    ('win-finance-01', 'ADV-2026-004', 'MEDIUM', 'OPEN', '2026-09-05 16:00:00+00', 'Managed Microsoft Edge update has not yet been reported by the endpoint.'),
    ('mac-platform-01', 'ADV-2026-005', 'HIGH', 'ACCEPTED_RISK', '2026-09-04 13:15:00+00', 'Developer workstation excluded until the current sprint demonstration completes.')
) AS seed(hostname, advisory_reference, severity, status, observed_at, details)
JOIN dev.computers computer ON computer.hostname = seed.hostname
JOIN dev.patches patch ON patch.advisory_reference = seed.advisory_reference
WHERE NOT EXISTS (
    SELECT 1 FROM dev.security_observations existing
    WHERE existing.computer_id = computer.computer_id
      AND existing.patch_id = patch.patch_id
      AND existing.details = seed.details
);

-- Copy the baseline data to `test`, mapping all foreign keys by stable natural keys.
INSERT INTO test.teams (name, contact_email)
SELECT name, contact_email FROM dev.teams
ON CONFLICT (name) DO UPDATE SET contact_email = EXCLUDED.contact_email;

INSERT INTO test.applications (vendor, product_name, third_party, owner_team_id, lifecycle, created_at)
SELECT application.vendor, application.product_name, application.third_party, test_team.team_id,
       application.lifecycle, application.created_at
FROM dev.applications application
LEFT JOIN dev.teams dev_team ON dev_team.team_id = application.owner_team_id
LEFT JOIN test.teams test_team ON test_team.name = dev_team.name
ON CONFLICT (vendor, product_name) DO NOTHING;

INSERT INTO test.application_versions (application_id, version, released_at, support_ends_at)
SELECT test_application.application_id, version.version, version.released_at, version.support_ends_at
FROM dev.application_versions version
JOIN dev.applications application ON application.application_id = version.application_id
JOIN test.applications test_application ON test_application.vendor = application.vendor AND test_application.product_name = application.product_name
ON CONFLICT (application_id, version) DO NOTHING;

INSERT INTO test.computers (
    hostname, operating_system, operating_system_version, environment, owner_team_id,
    internet_facing, criticality, last_seen_at, retired_at
)
SELECT computer.hostname, computer.operating_system, computer.operating_system_version,
       computer.environment, test_team.team_id,
       computer.internet_facing, computer.criticality,
       computer.last_seen_at, computer.retired_at
FROM dev.computers computer
JOIN dev.teams dev_team ON dev_team.team_id = computer.owner_team_id
JOIN test.teams test_team ON test_team.name = dev_team.name
ON CONFLICT (hostname) DO NOTHING;

INSERT INTO test.software_installations (computer_id, application_version_id, status, discovered_at, uninstalled_at)
SELECT test_computer.computer_id, test_version.application_version_id,
       installation.status, installation.discovered_at, installation.uninstalled_at
FROM dev.software_installations installation
JOIN dev.computers computer ON computer.computer_id = installation.computer_id
JOIN test.computers test_computer ON test_computer.hostname = computer.hostname
JOIN dev.application_versions version ON version.application_version_id = installation.application_version_id
JOIN dev.applications application ON application.application_id = version.application_id
JOIN test.applications test_application ON test_application.vendor = application.vendor AND test_application.product_name = application.product_name
JOIN test.application_versions test_version ON test_version.application_id = test_application.application_id AND test_version.version = version.version
ON CONFLICT (computer_id, application_version_id) DO NOTHING;

INSERT INTO test.patches (affected_version_id, fixed_version_id, advisory_reference, severity, available_at, description)
SELECT test_affected_version.application_version_id, test_fixed_version.application_version_id,
       patch.advisory_reference, patch.severity, patch.available_at, patch.description
FROM dev.patches patch
JOIN dev.application_versions affected_version ON affected_version.application_version_id = patch.affected_version_id
JOIN dev.applications affected_application ON affected_application.application_id = affected_version.application_id
JOIN test.applications test_affected_application ON test_affected_application.vendor = affected_application.vendor AND test_affected_application.product_name = affected_application.product_name
JOIN test.application_versions test_affected_version ON test_affected_version.application_id = test_affected_application.application_id AND test_affected_version.version = affected_version.version
LEFT JOIN dev.application_versions fixed_version ON fixed_version.application_version_id = patch.fixed_version_id
LEFT JOIN dev.applications fixed_application ON fixed_application.application_id = fixed_version.application_id
LEFT JOIN test.applications test_fixed_application ON test_fixed_application.vendor = fixed_application.vendor AND test_fixed_application.product_name = fixed_application.product_name
LEFT JOIN test.application_versions test_fixed_version ON test_fixed_version.application_id = test_fixed_application.application_id AND test_fixed_version.version = fixed_version.version
ON CONFLICT (advisory_reference, affected_version_id) DO NOTHING;

INSERT INTO test.security_observations (computer_id, patch_id, severity, status, observed_at, resolved_at, details)
SELECT test_computer.computer_id, test_patch.patch_id, observation.severity,
       observation.status, observation.observed_at, observation.resolved_at, observation.details
FROM dev.security_observations observation
JOIN dev.computers computer ON computer.computer_id = observation.computer_id
JOIN test.computers test_computer ON test_computer.hostname = computer.hostname
LEFT JOIN dev.patches patch ON patch.patch_id = observation.patch_id
LEFT JOIN test.patches test_patch ON test_patch.advisory_reference = patch.advisory_reference
WHERE NOT EXISTS (
    SELECT 1 FROM test.security_observations existing
    WHERE existing.computer_id = test_computer.computer_id
      AND existing.patch_id IS NOT DISTINCT FROM test_patch.patch_id
      AND existing.details = observation.details
);

-- Expected baseline counts: 4 teams, 22 applications, 22 versions, 9 computers,
-- 108 software installations, 5 patches, and 5 security observations in each schema.
