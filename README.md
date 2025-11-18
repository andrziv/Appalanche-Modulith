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
            copy templates\.env.template .env
            ```
        *   On macOS/Linux:
            ```bash
            cp templates/.env.template .env
            ```
    *   **Create your Docker override file:** Copy the example file from the `templates` directory to the project root.
        *   On Windows (PowerShell):
            ```powershell
            copy templates\compose.override.yaml.template compose.override.yaml
            ```
        *   On macOS/Linux:
            ```bash
            cp templates/compose.override.yaml.template compose.override.yaml
            ```

3.  **Update your secrets:** Open the newly created `.env` file in the project root and fill in the local environment variables.

4. **(Optional?) Update IDE Environment Settings**
    * **IntelliJ**
        1) Click on the Run Configuration near top right and press "Edit Configuration" in the dropdown.
            * Clicking on the three dot menu near the same area and pressing "Edit" in the dropdown also works.
        2) Select the Spring Boot application configuration.
        3) Find the environment variables field and either paste in the path to the `.env` file, or press the folder icon and click on the `.env` file to add it.
        4) Apply & Ok. Done!
   * **Visual Studio Code** (untested)
     1. Open the `.vscode/launch.json` file.
     2. Add the `envFile` attribute to the launch configuration:
     ```json
     {
         "type": "java",
         "name": "Launch YourApplication",
         "request": "launch",
         "mainClass": "com.yourpackage.YourApplication",
         "projectName": "your-project-name",
         "envFile": "${workspaceFolder}/.env"
     }
     ```
   * Alternatively: set your environment variables wherever you normally configure run-target settings in your IDE.

5. **Clean build using Maven:**
    * Open a terminal in the project root and run:
    ```bash
    ./mvnw spring-boot:build-image -Dspring-boot.build-image.cleanCache=true
    ```
   * Alternatively, open the Maven terminal in IntelliJ and run:
   ```
   mvn spring-boot:build-image -Dspring-boot.build-image.cleanCache=true
   ```

6. **Run the application:** You can now run the main Spring Boot application from your IDE. Spring will also start the docker container as well.