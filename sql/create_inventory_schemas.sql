-- IT Asset and Software Exposure Inventory
-- Uses single PostgreSQL database. It creates two equivalent schemas, `dev` and `test`.

--DROP SCHEMA dev CASCADE;
--DROP SCHEMA test CASCADE;

CREATE SCHEMA IF NOT EXISTS dev;
CREATE SCHEMA IF NOT EXISTS test;

CREATE TABLE dev.teams (
    team_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    contact_email VARCHAR(254) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dev_teams_name UNIQUE (name),
    CONSTRAINT uq_dev_teams_contact_email UNIQUE (contact_email)
);

CREATE TABLE test.teams (LIKE dev.teams INCLUDING ALL);

CREATE TABLE dev.computers (
    computer_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    operating_system VARCHAR(120) NOT NULL,
    operating_system_version VARCHAR(120) NOT NULL,
    environment VARCHAR NOT NULL,
    owner_team_id BIGINT NOT NULL REFERENCES dev.teams (team_id),
    internet_facing BOOLEAN NOT NULL DEFAULT FALSE,
    criticality VARCHAR NOT NULL DEFAULT 'MEDIUM',
    last_seen_at TIMESTAMPTZ NOT NULL,
    retired_at TIMESTAMPTZ,
    CONSTRAINT uq_dev_computers_hostname UNIQUE (hostname),
    CONSTRAINT chk_dev_computers_retirement CHECK (retired_at IS NULL OR retired_at >= last_seen_at)
);

CREATE TABLE test.computers (
    computer_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    operating_system VARCHAR(120) NOT NULL,
    operating_system_version VARCHAR(120) NOT NULL,
    environment VARCHAR NOT NULL,
    owner_team_id BIGINT NOT NULL REFERENCES test.teams (team_id),
    internet_facing BOOLEAN NOT NULL DEFAULT FALSE,
    criticality VARCHAR NOT NULL DEFAULT 'MEDIUM',
    last_seen_at TIMESTAMPTZ NOT NULL,
    retired_at TIMESTAMPTZ,
    CONSTRAINT uq_test_computers_hostname UNIQUE (hostname),
    CONSTRAINT chk_test_computers_retirement CHECK (retired_at IS NULL OR retired_at >= last_seen_at)
);

CREATE TABLE dev.applications (
    application_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vendor VARCHAR(160) NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    third_party BOOLEAN NOT NULL,
    owner_team_id BIGINT REFERENCES dev.teams (team_id),
    lifecycle VARCHAR NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dev_applications_vendor_product UNIQUE (vendor, product_name)
);

CREATE TABLE test.applications (
    application_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vendor VARCHAR(160) NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    third_party BOOLEAN NOT NULL,
    owner_team_id BIGINT REFERENCES test.teams (team_id),
    lifecycle VARCHAR NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_test_applications_vendor_product UNIQUE (vendor, product_name)
);

CREATE TABLE dev.application_versions (
    application_version_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES dev.applications (application_id),
    version VARCHAR(100) NOT NULL,
    released_at DATE,
    support_ends_at DATE,
    CONSTRAINT uq_dev_application_versions_application_version UNIQUE (application_id, version),
    CONSTRAINT chk_dev_application_versions_support_date CHECK (support_ends_at IS NULL OR released_at IS NULL OR support_ends_at >= released_at)
);

CREATE TABLE test.application_versions (
    application_version_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES test.applications (application_id),
    version VARCHAR(100) NOT NULL,
    released_at DATE,
    support_ends_at DATE,
    CONSTRAINT uq_test_application_versions_application_version UNIQUE (application_id, version),
    CONSTRAINT chk_test_application_versions_support_date CHECK (support_ends_at IS NULL OR released_at IS NULL OR support_ends_at >= released_at)
);

CREATE TABLE dev.software_installations (
    installation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    computer_id BIGINT NOT NULL REFERENCES dev.computers (computer_id),
    application_version_id BIGINT NOT NULL REFERENCES dev.application_versions (application_version_id),
    status VARCHAR NOT NULL DEFAULT 'INSTALLED',
    discovered_at TIMESTAMPTZ NOT NULL,
    uninstalled_at TIMESTAMPTZ,
    CONSTRAINT uq_dev_software_installations_computer_version UNIQUE (computer_id, application_version_id),
    CONSTRAINT chk_dev_software_installations_dates CHECK (uninstalled_at IS NULL OR uninstalled_at >= discovered_at)
);

CREATE TABLE test.software_installations (
    installation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    computer_id BIGINT NOT NULL REFERENCES test.computers (computer_id),
    application_version_id BIGINT NOT NULL REFERENCES test.application_versions (application_version_id),
    status VARCHAR NOT NULL DEFAULT 'INSTALLED',
    discovered_at TIMESTAMPTZ NOT NULL,
    uninstalled_at TIMESTAMPTZ,
    CONSTRAINT uq_test_software_installations_computer_version UNIQUE (computer_id, application_version_id),
    CONSTRAINT chk_test_software_installations_dates CHECK (uninstalled_at IS NULL OR uninstalled_at >= discovered_at)
);

CREATE TABLE dev.patches (
    patch_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    affected_version_id BIGINT NOT NULL REFERENCES dev.application_versions (application_version_id),
    fixed_version_id BIGINT REFERENCES dev.application_versions (application_version_id),
    advisory_reference VARCHAR(80) NOT NULL,
    severity VARCHAR NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT uq_dev_patches_advisory_affected_version UNIQUE (advisory_reference, affected_version_id)
);

CREATE TABLE test.patches (
    patch_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    affected_version_id BIGINT NOT NULL REFERENCES test.application_versions (application_version_id),
    fixed_version_id BIGINT REFERENCES test.application_versions (application_version_id),
    advisory_reference VARCHAR(80) NOT NULL,
    severity VARCHAR NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT uq_test_patches_advisory_affected_version UNIQUE (advisory_reference, affected_version_id)
);

CREATE TABLE dev.security_observations (
    observation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    computer_id BIGINT NOT NULL REFERENCES dev.computers (computer_id),
    patch_id BIGINT REFERENCES dev.patches (patch_id),
    severity VARCHAR NOT NULL,
    status VARCHAR NOT NULL DEFAULT 'OPEN',
    observed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    details TEXT NOT NULL,
    CONSTRAINT chk_dev_security_observations_dates CHECK (resolved_at IS NULL OR resolved_at >= observed_at)
);

CREATE TABLE test.security_observations (
    observation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    computer_id BIGINT NOT NULL REFERENCES test.computers (computer_id),
    patch_id BIGINT REFERENCES test.patches (patch_id),
    severity VARCHAR NOT NULL,
    status VARCHAR NOT NULL DEFAULT 'OPEN',
    observed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    details TEXT NOT NULL,
    CONSTRAINT chk_test_security_observations_dates CHECK (resolved_at IS NULL OR resolved_at >= observed_at)
);

CREATE INDEX idx_dev_computers_exposure_last_seen
    ON dev.computers (internet_facing, last_seen_at DESC);
CREATE INDEX idx_test_computers_exposure_last_seen
    ON test.computers (internet_facing, last_seen_at DESC);

CREATE INDEX idx_dev_installations_computer_status
    ON dev.software_installations (computer_id, status);
CREATE INDEX idx_test_installations_computer_status
    ON test.software_installations (computer_id, status);

CREATE INDEX idx_dev_patches_severity_available
    ON dev.patches (severity, available_at DESC);
CREATE INDEX idx_test_patches_severity_available
    ON test.patches (severity, available_at DESC);

CREATE INDEX idx_dev_security_observations_open
    ON dev.security_observations (status, severity, observed_at DESC)
    WHERE status IN ('OPEN', 'ACKNOWLEDGED');
CREATE INDEX idx_test_security_observations_open
    ON test.security_observations (status, severity, observed_at DESC)
    WHERE status IN ('OPEN', 'ACKNOWLEDGED');
