# JobHunt Modulith Project

This project is a Spring Boot application using the Modulith pattern.

## Local Development Setup

To run this project locally, you will need Java, Maven, and Docker installed.

1.  **Clone the repository:**
    ```bash
    git clone ...
    ```

2.  **Configure your local environment:**
    This project uses template files to help you set up your local secrets and Docker configuration. These are located in the `/templates` directory.

    *   **Create your `.env` file:** Copy the example file from the `templates` directory to the project root.
        *   On Windows (PowerShell):
            ```powershell
            copy templates\.env.example .env
            ```
        *   On macOS/Linux:
            ```bash
            cp templates/.env.example .env
            ```
    *   **Create your Docker override file:** Copy the example file from the `templates` directory to the project root.
        *   On Windows (PowerShell):
            ```powershell
            copy templates\compose.override.yaml.example compose.override.yaml
            ```
        *   On macOS/Linux:
            ```bash
            cp templates/compose.override.yaml.example compose.override.yaml
            ```

3.  **Update your secrets:**
    Open the newly created `.env` file in the project root and fill in your local `PG_PASSWORD`.

4.  **Start the database:**
    Open a terminal in the project root and run:
    ```bash
    docker compose up -d
    ```

5.  **Run the application:**
    You can now run the main Spring Boot application from your IDE.