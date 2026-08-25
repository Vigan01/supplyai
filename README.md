# SupplyAI

[![CI](https://github.com/Vigan01/supplyai/actions/workflows/ci.yml/badge.svg)](https://github.com/Vigan01/supplyai/actions/workflows/ci.yml)

SupplyAI is an early-stage supply-chain planning and inventory management desktop application. The product uses a German user interface while code, package names, commits, and technical documentation remain in English.

## Current scope

- Spring Boot service foundation
- Local German desktop UI with JavaFX
- Spring Boot backend embedded in the desktop app
- German JSON status endpoint
- Product and SKU management
- Inventory movement booking for stock-in, stock-out, and adjustments
- Supplier profiles with lead-time data
- Supplier CRUD workflow in the desktop UI
- Reorder suggestions based on stock, reorder point, and average daily demand
- Purchase order workflow with planned, ordered, delivered, and cancelled statuses
- Dashboard API with inventory, alert, movement, and metric data
- Statistics view with inventory value, coverage, status distribution, category distribution, and supplier risk
- Activity log for product changes, movement bookings, and CSV imports
- Dropdown navigation, filter menus, and product action menus in the desktop UI
- CSV import and export for product master data
- Local H2 persistence for immediate development
- Flyway database migrations with Hibernate schema validation
- Desktop packaging script based on `jpackage`
- Health endpoint via Spring Boot Actuator
- Optional PostgreSQL profile prepared for production-like persistence
- Automated test coverage for application startup and the first web endpoint
- GitHub-ready repository hygiene with CI, issue templates, pull request template, license, and contribution guide

## Technology

- Java 21 LTS
- Spring Boot 3.4
- Maven Wrapper
- JUnit 5 and Spring MockMvc
- Spring Data JPA
- Flyway
- H2 for local persistence
- JavaFX desktop shell
- PostgreSQL through the optional `postgres` Maven profile

## Requirements

- JDK 21 or newer
- No locally installed Maven required; use `./mvnw`

## Run as Desktop App

Start the local desktop application:

```bash
./mvnw -Pdesktop javafx:run
```

This opens SupplyAI as an app window. Spring Boot still runs internally on a random local port, but users do not need to open or know a `localhost` URL.

## Run as Web Development Mode

```bash
./mvnw spring-boot:run
```

Open <http://localhost:8080>. This mode is useful for backend/API development and browser debugging.

Useful endpoints:

- `GET /` opens the local dashboard UI
- `GET /api/status` returns application status JSON
- `GET /api/dashboard` returns dashboard data for the local UI
- `GET /api/products` lists products
- `POST /api/products` creates products
- `PUT /api/products/{id}` updates products
- `DELETE /api/products/{id}` deletes products
- `POST /api/products/{id}/movements` books inventory movements
- `GET /api/products/csv` exports product master data
- `POST /api/products/csv` imports product master data as multipart CSV upload
- `GET /api/suppliers` lists suppliers
- `POST /api/suppliers` creates suppliers
- `PUT /api/suppliers/{id}` updates suppliers
- `DELETE /api/suppliers/{id}` deletes suppliers
- `GET /api/purchase-orders` lists purchase orders
- `POST /api/purchase-orders` creates purchase orders
- `PATCH /api/purchase-orders/{id}/status` updates purchase order status
- `GET /api/statistics` returns operational statistics
- `GET /api/activity` returns the latest activity log entries
- `GET /actuator/health` returns health information
- `GET /h2-console` opens the local H2 database console

## Test and build

Run the test suite:

```bash
./mvnw test
```

Run the full verification used by CI:

```bash
./mvnw verify
```

Create a desktop app image:

```bash
./scripts/package-desktop.sh
```

The generated app image is written to `dist/`. The script requires a full JDK with `jpackage`.

## Local persistence

By default, SupplyAI stores local development data in `./data/supplyai`. The `data/` directory is ignored by Git.

H2 console settings:

- JDBC URL: `jdbc:h2:file:./data/supplyai`
- User: `sa`
- Password: empty

## PostgreSQL profile

The default application starts without a database. This keeps the first setup simple and makes CI fast.

To enable PostgreSQL, create a database named `supplyai`, provide the environment variables from `.env.example`, and run both the Maven and Spring profiles:

```bash
./mvnw -Ppostgres spring-boot:run -Dspring-boot.run.profiles=postgres
```

`ddl-auto` is set to `validate`, so the application will not silently change a production schema. Schema changes are versioned in `src/main/resources/db/migration`.

## Project structure

```text
src/main/java/com/supplyai/
├── SupplyAiApplication.java       # application entry point
├── activity/                      # audit log
├── dashboard/                     # dashboard and status API
├── desktop/                       # JavaFX desktop shell
├── inventory/                     # products, movements, CSV
├── purchase/                      # purchase orders
├── statistics/                    # operational statistics
└── supplier/                      # supplier management

src/main/resources/
├── application.yml                # shared configuration
├── application-postgres.yml       # database profile
├── db/migration/                  # Flyway migrations
└── static/                        # desktop UI assets

src/test/java/com/supplyai/
├── SupplyAiApplicationTests.java
└── dashboard/
    └── HomeControllerTest.java
```

New business capabilities should live in their own package. This package-by-feature approach keeps controllers, services, and domain code belonging to one business capability close together.

## GitHub upload

Initialize the repository and push it to GitHub:

```bash
git init
git add .
git commit -m "Initial SupplyAI project"
git branch -M main
git remote add origin git@github.com:Vigan01/supplyai.git
git push -u origin main
```

After creating the GitHub repository, replace `Vigan01/supplyai` in the CI badge at the top of this README with the real GitHub path.

## IntelliJ IDEA

1. Choose **Open** and select the project directory or its `pom.xml`.
2. Select a JDK 21 or newer when prompted.
3. Allow IntelliJ to import the Maven project.
4. Run `SupplyAiApplication`.

## Roadmap

The core local workflow is now implemented: products, inventory levels, suppliers, movements, warnings, reorder suggestions, purchase orders, CSV, audit logs, statistics, desktop mode, packaging preparation, and migration-based persistence.

Good next features:

- Authentication after the core workflows are stable
- AI/ML forecasting once enough historical movement data exists
- Signed installers for macOS and Windows
- Automated release builds in GitHub Actions

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
