# Schema Diff POC

This proof of concept compares PostgreSQL schema definitions for an IT asset and software exposure inventory.

## Create the two baseline schemas

1. Create a PostgreSQL data source for the local database.
2. Open a query console for that data source.
3. Run [`sql/create_inventory_schemas.sql`](sql/create_inventory_schemas.sql).

The script creates equivalent `dev` and `test` schemas. It is intended for a fresh database; it does not delete or replace existing objects.

4. Run [`sql/seed_inventory_data.sql`](sql/seed_inventory_data.sql) to add realistic endpoint, software, patch, and security-observation data to both schemas.

## Java schema-diff tool

Credentials are supplied as environment variables and/or config/connection.properties

For a shell run, for example:

```bash
export SOURCE_DB_URL='jdbc:postgresql://localhost:5432/postgres'
export SOURCE_DB_USER='postgres'
export SOURCE_DB_PASSWORD='password'
export TARGET_DB_URL='jdbc:postgresql://localhost:5432/postgres'
export TARGET_DB_USER='postgres'
export TARGET_DB_PASSWORD="password"
```

Compare the `dev` reference schema with the `test` target schema:

```bash
mvn compile exec:java -Dexec.args="--source=db"
```

To use PostgreSQL DDL as the reference instead, set `source.file.path` and `source.file.schema`, then run:

```bash
mvn compile exec:java -Dexec.args="--source=file"
```

To pass params via commadline params:
```bash
mvn compile exec:java \
  -Dexec.args="--source=db \
  --source-db-url=jdbc:postgresql://localhost:5432/inventory_poc \
  --source-db-user=postgres \
  --source-db-password=password \
  --target-db-url=jdbc:postgresql://localhost:5432/inventory_poc \
  --target-db-user=postgres \
  --target-db-password=password \
  --groq.api.key=api_key"
```

The report is written to `reports/schema-diff.md` by default. The tool compares tables, columns, primary keys, imported foreign keys, and indexes. The DDL reader supports the PostgreSQL `CREATE TABLE` and `CREATE INDEX` subset used by this POC.
