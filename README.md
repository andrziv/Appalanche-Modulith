# Appalanche Modulith Project

This project serves as the backend of the Appalanche system. This is my first attempt at a "Secure" Spring server, with
a reliance on it being a Modulith because I think that having the modules/components be "microservice-like" and
standalone is worth doing, because it's interesting (monoliths are pretty easy to
build in comparison). It's a pretty simple CRUD server, but it was a nice first attempt at something secure, while using 
Spring's configuration, data (Hibernate), and security frameworks while also (integration) testing the functionality and 
implementing a full CI/CD support backbone.

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
        - `200: OK`
            - Important cookies (`Strict`, `HttpOnly`):
                - `accessToken`, short-lived JWT for accessing important endpoints
                - `refreshToken`, opaque token for refreshing the `accessToken` when calling
                  `POST /authenticate/refresh`
            - Body Content with the following info:
                - accountId (`UUID`)
                - email (`String`)
                - accessExpiryMilliseconds (`long`)

- `GET /authenticate`, **requires a valid `accessToken` JWT cookie to access**
    - Returns:
        - `200: OK`, Returns content with the following info related to the account attached to the JWT:
            - Body Content:
                - accountId (`UUID`)
                - email (`String`)
                - accessExpiryMilliseconds (`long`)

- `POST /authenticate/refresh`, **requires a valid `refreshToken` cookie to successfully use**
    - Returns:
        - `200: OK`, Returns refreshed `accessToken` and `refreshToken` Strict httpOnly cookies.
            - Body Content:
                - accountId (`UUID`)
                - email (`String`)
                - accessExpiryMilliseconds (`long`)

- `POST /authenticate/logout`, **requires a valid `accessToken` JWT cookie to access**
    - Returns:
        - `200: OK`, Returns a dead/outdated JWT as a Strict httpOnly cookie called `accessToken`.

### Account Endpoints

- **You require a valid `accessToken` JWT cookie to access:**
    - `PATCH /account/change-password`
        - Required request body fields:
            - oldPassword (`String`)
            - newPassword (`String`)
        - Returns:
            - `200: OK`, Returns refreshed `accessToken` and `refreshToken` Strict httpOnly cookies.
                - Body Content:
                    - accountId (`UUID`)
                    - email (`String`)
                    - accessExpiryMilliseconds (`long`)
    - `PATCH /account/change-email`
        - Required request body fields:
            - currentPassword (`String`)
            - newEmail (`String`)
        - Returns:
            - `200: OK`, Returns refreshed `accessToken` and `refreshToken` Strict httpOnly cookies.
                - Body Content:
                    - accountId (`UUID`)
                    - email (`String`)
                    - accessExpiryMilliseconds (`long`)

### Profile Endpoints

- **You require a valid `accessToken` JWT cookie to access:**
    - `GET /profile`
        - Returns:
            - `200: OK`, with a return of the profile data in the body:
                - accountId (`UUID`)
                - firstname (`String`)
                - surname (`String`)
                - linkedInProfile (`String`, URL)
                - gitHubProfile (`String`, URL)
                - portfolioSite (`String`, URL)
                - jobSites (`JobSite`)
                    - siteId (`UUID`)
                    - url (`String`)
                    - name (`String`)

    - `PATCH /profile`
        - Optional request body fields:
            - firstname (`String`, if the field is present, it must have at least one non-whitespace char)
            - surname (`String`, if the field is present, it must have at least one non-whitespace char)
            - linkedInProfile (`String`, URL)
            - gitHubProfile (`String`, URL)
            - portfolioSite (`String`, URL)
        - Notes on request body fields:
            - To null out the object fields, you must set the related request fields to '' or " ". Having them be null
              or missing will NOT null the related object fields.
        - Returns:
            - `204: NO CONTENT`

    - `POST /profile/job-sites` (adds a job site to a user)
        - Required request body fields:
            - url (`String`, doesn't even need to be a URL, can just be a domain)
        - Returns:
            - `201: CREATED`

    - `POST /profile/job-sites/{id}`
        - Required path field:
            - id (`UUID`)
        - Returns:
            - `204: NO CONTENT`

### Application Endpoints

- Note that any Status or Experience Level related `String` inputs require the strings to be exactly one of the
  listed Code entries in the [Status Codes](#status-codes) or [Experience Codes](#experience-codes) sections below,
  respectively, unless otherwise specified.

- **You require a valid `accessToken` JWT cookie to access:**
    - `GET /application`
        - Optional path search params:
            - search (`String`)
            - statusCodes (List of `String`)
            - experienceLevelCodes (List of `String`)
            - interestCriteria (List of `String`)
                - Format should be as following: `KEYWORD:NUMBER` or `KEYWORD:NUMBER1:NUMBER2`, see the following:
                    - `gt:NUMBER`, find Applications with interest greater than `NUMBER`
                    - `gte:NUMBER`, find Applications with interest greater or equal to `NUMBER`
                    - `lt:NUMBER`, find Applications with interest less than `NUMBER`
                    - `lte:NUMBER`, find Applications with interest less than or equal to `NUMBER`
                    - `eq:NUMBER`, find Applications with interest equal to `NUMBER`
                    - `between:NUMBER1:NUMBER2`, find Applications with interest between `NUMBER1` and `NUMBER2`,
                      inclusive
            - appliedAfter (ISO 8601 `Date`, format: `yyyy-MM-dd`)
            - appliedBefore (ISO 8601 `Date`, format: `yyyy-MM-dd`)
            - responseAfter (ISO 8601 `Date`, format: `yyyy-MM-dd`)
            - responseBefore (ISO 8601 `Date`, format: `yyyy-MM-dd`)
            - timezone (`String`, TZ Identifier (e.g. `America/Toronto`))
        - Notes on request body fields:
            - The statusCodes field could be fed with code-fragments to return every application with that status,
              regardless of the round-count. For example, "statusCodes=INTERVIEW" will return applications with "
              INTERVIEW_1" and "INTERVIEW_10".
            - The after/before dates are inclusive. An application with a responseDate of 2025-12-25T20:00:30Z will
              be caught by a "before" query at 2025-12-25.
        - Returns (paged according to HATEOAS spec):
            - jobApplicationModelList (List of `JobApplicationModel`, see `GET` endpoint below for the details of the
              `JobApplicationModel` fields)
                - NOTE: The `description` field will ALWAYS be `null` when calling with the search endpoint.
                  The real description can only be obtained through the ID `GET` endpoint below.

    - `GET /application/{id}`
        - Requires the following path field:
            - ID (`UUID`)
        - Returns:
            - `200: OK`, with a return of the application data in the body (`JobApplicationModel`):
                - applicationId (`UUID`)
                - requisitionId (`String`)
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
                - experience (`Experience` object)
                    - id (`long`)
                    - code (`String`)
                    - label (`String`)
                    - description (`String`)
                - jobPostingLink (`URL`)
                - description (`String`)
                - appliedDate (ISO 8601 `Date & Time`, format: `yyyy-MM-ddThh:mm:ssZ` e.g. `2025-12-27T18:50:00Z`)
                - responseDate (ISO 8601 `Date & Time`, format: `yyyy-MM-ddThh:mm:ssZ` e.g. `2025-12-27T18:50:00Z`)

    - `POST /application`
        - Requires the following fields:
            - requisitionId (`String`)
            - title (`String`)
            - company (`String`)
            - interest (`integer`, between 1 and 10 inclusive)
            - statusCode (`String`)
            - experienceLevelCode (`String`)
            - jobPostingLink (`URL`, HTTPS required)
            - description (`String`)
            - appliedDate (ISO 8601 `Date & Time`, format: `yyyy-MM-ddThh:mm:ssZ` e.g. `2025-12-27T18:50:00Z`)
            - responseDate (ISO 8601 `Date & Time`, format: `yyyy-MM-ddThh:mm:ssZ` e.g. `2025-12-27T18:50:00Z`)
        - Returns:
            - `201: CREATED` with...
                - a return of the newly created application ID in the body:
                    - applicationId (`UUID`)
                - the URI in the `Location` header

    - `PATCH /application/{id}`
        - Requires the following path field:
            - ID (`UUID`)
        - Optional request body fields:
            - requisitionId (`String`)
            - title (`String`)
            - company (`String`)
            - interest (`integer`, between 1 and 10 inclusive)
            - statusCode (`String`)
            - experienceLevelCode (`String`)
            - jobPostingLink (`URL`, HTTPS required)
            - description (`String`)
            - appliedDate (ISO 8601 `Date & Time`, format: `yyyy-MM-ddThh:mm:ssZ` e.g. `2025-12-27T18:50:00Z`)
            - responseDate (ISO 8601 `Date & Time`, format: `yyyy-MM-ddThh:mm:ssZ` e.g. `2025-12-27T18:50:00Z`)
        - Notes:
            - To null out the description field, you must set the related request fields to '' or " ". Having them be
              null or missing will NOT null the related object fields.
        - Returns:
            - `204: NO CONTENT`

    - `DELETE /application/{id}`
        - Requires the following path field:
            - ID (`UUID`)
        - Returns:
            - `204: NO CONTENT`

#### Static Application Data

- **You require a valid `accessToken` JWT cookie to access:**
    - `GET /application/static/statuses`
        - Returns:
            - `200: OK`, with a list of `Status` that each have the following fields:
                - id (`long`)
                - code (`String`)
                - label (`String`)
                - round (`integer`)
                - colour (`String`, HEX code for a colour)
                - textColour (`String`, HEX code for a colour)

    - `GET /application/static/experiences`
        - Returns:
            - `200: OK`, with a list of `Experience` that each have the following fields:
                - id (`long`)
                - code (`String`)
                - label (`String`)
                - description (`String`)

    - `GET /application/static/metadata/statuses`
        - Returns:
            - `200: OK`, with a list of `Status Metadata` that each have the following fields:
                - codeFragment (`String`)
                - maxRounds (`integer`)

### Company Logo Endpoints

- **You require a valid `accessToken` JWT cookie to access:**
    - `GET /logo/{brand}/{tld}`
        - Requires the following path field:
            - brand (`String`)
            - tld (`String`)
                - Note: TLD = Top Level Domain (e.g. `.com`, `.ca`, `.org`, `.net`, `.dev`, etc.)
        - Returns:
            - `image/jpeg`
    - `GET /logo/{brand}`
        - Requires the following path field:
            - brand (`String`)
        - Returns:
            - `image/jpeg`

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

### Experience Codes

- `INTERN`
- `ENTRY_LEVEL`
- `JUNIOR`
- `INTERMEDIATE`
- `SENIOR`
- `STAFF`
- `PRINCIPAL`
- `MANAGER`

## Local Development Setup

To run this project locally, you will need Java, Maven, and Docker installed.

1. **Clone the repository:**
   ```bash
   git clone ...
   ```

2. **Configure your local environment:**
   This project uses template files to help you set up your local secrets and Docker configuration. These are located in
   the `/templates` directory.

    * **Create your `.env` file:** Copy the example file from the `templates` directory to the project root.
        * On Windows (PowerShell):
          ```powershell
          copy templates\.env.template .env
          ```
        * On macOS/Linux:
          ```bash
          cp templates/.env.template .env
          ```
    * **Create your Docker override file:** Copy the example file from the `templates` directory to the project root.
        * On Windows (PowerShell):
          ```powershell
          copy templates\compose.override.yaml.template compose.override.yaml
          ```
        * On macOS/Linux:
          ```bash
          cp templates/compose.override.yaml.template compose.override.yaml
          ```

3. **Update your secrets:** Open the newly created `.env` file in the project root and fill in the local environment
   variables.

4. (Optional) **Update IDE Environment Settings**
    * **IntelliJ**
        * Assuming that the `main` project run configuration is not already in the IntelliJ project, you can create your
          own by:
            1) Click on the Run Configuration near top right and press "Edit Configuration" in the dropdown.
                * Clicking on the three dot menu near the same area and pressing "Edit" in the dropdown also works.
            2) Select the Spring Boot application configuration.
            3) Find the environment variables field and either paste in the path to the `.env` file, or press the folder
               icon and click on the `.env` file to add it.
            4) Apply & Ok. Done!
    * **Visual Studio Code** (untested)
        1. Open the `.vscode/launch.json` file.
        2. Add the `envFile` attribute to the launch configuration:
      ```json
      {
          "type": "java",
          "name": "Launch Appalanche",
          "request": "launch",
          "mainClass": "com.appalanche.backend.AppalancheModulithApplication",
          "projectName": "appalanche-modulith",
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

6. **Run the application:** You can now run the main Spring Boot application from your IDE. Spring will also start the
   docker container as well.

## Future Goals

1) Add whatever features I deem interesting or useful to me, the only user (lol)
    - Statistics provider
      - Generic status breakdown, status breakdown for a company/experience level/resume/date range, or a combination?
    - Email-reading service to automatically update the status of an application
    - Latex resume storage + Compiler? and to allow for Resume modification all on the site
        - Tie it into the application portion so that the user can specify which resume (and/or) cover letter they used
          for an application
    - Crawler functionality?
        - Crawling the web to look for available jobs and recommend certain ones to users
        - Crawling a provided link to automatically fill other fields in
2) Security
    - OAUTH and SSO to be added too because I think implementation could be interesting
3) Porting over to the latest and greatest
    - The new Spring framework versions have just recently come out and I think I'll wait for a few minor versions to go
      by before dipping myself into using them.