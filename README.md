# Java Mail Service

A small Spring Boot REST API for persisting email requests and attachment metadata, searching stored emails, and downloading stored attachments.

> The current `send` operations are intentionally transport stubs. The application persists email requests and models the send workflow, but it does not connect to an SMTP provider or deliver real email.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- Jakarta Validation
- H2 in-memory database
- Maven
- JUnit 5 / MockMvc / Mockito
- Postman functional tests

## Run locally

Prerequisites:

- JDK 21
- Maven

```powershell
mvn test
mvn spring-boot:run
```

The API starts on:

```text
http://localhost:8080/api/emails
```

The H2 console is available at:

```text
http://localhost:8080/h2
```

Default database connection:

```text
JDBC URL: jdbc:h2:mem:memDB
User: sa
Password: <empty>
```

Attachments are stored under `./attachments` by default. Override the location with:

```powershell
$env:MAIL_SERVICE_ATTACHMENT_PATH = "C:\path\to\attachments"
mvn spring-boot:run
```

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/emails/send` | Persist one email request, optionally with attachments |
| POST | `/api/emails/bulk` | Persist multiple email requests with UUID-mapped attachments |
| GET | `/api/emails/{id}` | Get one stored email |
| GET | `/api/emails/search` | Search by optional sender, recipient, and subject filters |
| GET | `/api/emails/sent` | List all stored emails |
| GET | `/api/emails/attachments/{attachmentId}/download` | Download a stored attachment |

### Single email

`POST /api/emails/send` consumes `multipart/form-data`.

Text parts:

- `sender`
- `recipient`
- `subject`
- `body`

Optional file parts:

- one or more `attachments`

### Bulk email

`POST /api/emails/bulk` also consumes `multipart/form-data`.

The JSON part must be named `emailRequests`. Each request may reference attachment keys through `attachmentIds`.

Example JSON part:

```json
[
  {
    "sender": "sender1@example.com",
    "recipient": "recipient1@example.com",
    "subject": "Subject 1",
    "body": "Body 1",
    "attachmentIds": ["ddbeceac-78a9-4ada-a047-8be226406124"]
  },
  {
    "sender": "sender2@example.com",
    "recipient": "recipient2@example.com",
    "subject": "Subject 2",
    "body": "Body 2",
    "attachmentIds": [
      "eba625c6-ae6e-4ecb-8e2a-17fa93f28ffc",
      "ddbeceac-78a9-4ada-a047-8be226406124"
    ]
  }
]
```

Each uploaded file part is named with the UUID referenced by `attachmentIds`.

## Automated tests

### Maven test suite

```powershell
mvn test
```

The repository includes unit and Spring MVC integration tests.

### Postman functional suite

Import:

```text
postman/MailService.postman_collection.json
```

Set the Postman working directory to the repository's `postman` directory so these fixture paths resolve:

```text
test-files/attachment-a.txt
test-files/attachment-b.txt
```

Run the collection in its existing order. It covers 11 functional scenarios, including:

- email creation with zero, one, and multiple attachments
- bulk creation with a shared attachment
- retrieval and search
- attachment download
- not-found responses

## Persistence behavior

The database is in-memory. Restarting the application resets email and attachment database records.

Attachment files are written to disk and are not automatically deleted when the application stops.

## Security notes

- Uploaded files are stored under a configured base directory.
- Attachment retrieval normalizes and constrains paths to that directory.
- Request bodies and email contents are not written to application logs.
- This sample has no authentication or authorization layer and should not be exposed to the public internet as-is.

## CI

GitHub Actions runs the Maven test suite for pushes and pull requests targeting `main`.
