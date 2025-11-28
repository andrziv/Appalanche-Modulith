# JobHunt Modulith Project
This project serves as the backend of the JobHunt system. This is my first attempt at a "Secure" Spring server, with a 
reliance on it being a Modulith because I think that having the modules/components be "microservice-like" and standalone
is worth doing, not only because it's interesting, but it also keeps your mind sharper (monoliths are pretty easy to 
build in comparison). It's a pretty simple CRUD server, but it was a nice first "non-tutorial"/copy-paste/cookie cutter 
attempt at something secure, while using Spring's configuration, data (Hibernate), and security frameworks while also
(integration) testing the functionality and implementing a full CI/CD support backbone. 

## API
### Authentication Endpoints
  - `POST /authenticate/signup`
    - Requires the following fields:
        - firstname (`String`)
        - surname (`String`)
        - email (`String`)
        - password (`String`)
    - Returns:
      - `201: CREATED`, no body content
  - `POST /authenticate/login`, which requires the following fields:
    - Requires the following fields:
      - email (`String`)
      - password (`String`)
    - Returns:
        - `200: OK`, Returns the JWT needed for other endpoint access, and the lifespan of the token in milliseconds
### Application Endpoints 
- **You require a valid Bearer JWT in `Authorization` header to access:**
  - `GET /application`
  - `GET /application/{id}`
      - Requires the following path field:
          - ID (`long`)
      - Returns:
          - `200: OK`, with a return of the application data in the body:
            - id (`long`)
            - requisitionId (`String`)
            - ownerEmail (`String`)
            - title (`String`)
            - company (`String`)
            - interest (`integer`, between 1 and 10 inclusive)
            - status (`Status` object)
              - id (`long`)
              - code (`String`)
              - label (`String`)
              - round (`integer`)
              - colour (`String`, HEX code for a colour)
              - textColour (`String`, HEX code for a colour)
            - appliedDate (`Date`)
            - responseDate (`Date`)
            - createdAt (`Date`)
  - `POST /application`
      - Requires the following fields:
        - requisitionId (`String`)
        - ownerEmail (`String`)
        - title (`String`)
        - company (`String`)
        - interest (`integer`)
        - status (`String`, only allowing the codes seen in the [Status Codes](#status-codes) section below)
        - appliedDate (`Date`)
        - responseDate (`Date`)
      - Returns:
          - `201: CREATED` with...
            - a return of the newly created application ID in the body:
                - id (`long`)
            - the URI in the `Location` header
  - `PATCH /application/{id}`
      - Requires the following path field:
          - ID (`long`)
    - Returns:
        - `204: NO CONTENT`
  - `DELETE /application/{id}`
      - Requires the following path field:
          - ID (`long`)
    - Returns:
        - `204: NO CONTENT`
      
### Status Codes
  - `APPLIED`
  - `ONLINE_ASSESSMENT`
  - `PHONE_INTERVIEW_{0}`
    - Note: {0} can be any integer between 1 and 10, inclusive
  - `INTERVIEW_{0}`
      - Note: {0} can be any integer between 1 and 10, inclusive
  - `TECHNICAL_INTERVIEW_{0}`
      - Note: {0} can be any integer between 1 and 10, inclusive
  - `OFFER_RECEIVED`
  - `REJECTED`
  - `WITHDREW`

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

4. (Optional) **Update IDE Environment Settings**
    * **IntelliJ**
      * Assuming that the `main` project run configuration is not already in the IntelliJ project, you can create your own by:
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

## Future Goals
1) Add whatever features I deem interesting or useful to me, the only user (lol)
   - Latex resume storage + Compiler? and to allow for Resume modification all on the site
   - Crawler functionality?
     - Crawling the web to look for available jobs and recommend certain ones to users
     - Crawling a provided link to automatically fill other fields in
2) Security
   - Look into CSRF protection (it's currently disabled and I believe it requires a little extra work to get it going on the frontend, so this will happen after I'm satisfied with the state of the frontend)
   - Currently only JWTs are in-play, but I'd like to add a stored opaque token that allows for JWTs to be automatically provided if the opaque token is present.
     - This will make sessions last longer while also avoiding the downsides of JWTs not being able to be revoked...
   - OAUTH and SSO to be added too because I think implementation could be interesting
3) Exposing the Status Data?
    - A little extra work but having the data be "get-able" would allow for less hardcoding on the Frontend part
4) Porting over to the latest and greatest
   - The new Spring framework versions have just recently come out and I think I'll wait for a few minor versions to go by before dipping myself into using them.