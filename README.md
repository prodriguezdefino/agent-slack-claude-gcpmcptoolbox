# Custom GenAI Agent: Hotel Booking made easy through Slack - Claude - GCP MCPtoolbox Integration

This repository contains a custom GenAI Agent specialized in handling Hotel's booking using a conversational UX. The integrated solution is built using Anthropic's Claude LLM as the Agent's brain, integrating data access services exposing datasources hosted in Google Cloud Platform (GCP) through MCP and the conversational UX is based on a Slack application which serves also as the Agent's memory (based on threaded conversations). The Agent service application is built using Java and Spring Boot AI starters.

All the code included in this project is intended for demonstration purposes only. It is not intended for use in a production environment. Permissions granted to the underlying resources are too broad, so is important to deploy this demo with caution and not use production infrastructure, datasources, etc.

## Example User Interaction

Once all the solution's components are deployed to GCP and Slack application settings are completed and installed on a Slack Workspace, we can start asking questions to our Agent by mentioning it on a channel. But first, lets quickly check the BigQuery table containing the data we will be accessing through our conversational Agent.

![BigQuery table with Hotel's data](/images/bq1.png)

In the table there are a handful of hotel related entries, including the hotel's internal identifier, its name, city location price tier, available checkin and checkout dates and if they are currently booked. We have defined through our MCP Toolbox [configuration](/infra/mcptoolbox/tools.yaml.tpl) the expected access patterns, as SQL statements, for our Agent to use as tools:
* Searches by name or location
* Book a hotels by its ID
* Update checkin and checkout dates by ID and ISO datetime format
* Cancel a booking by ID

Once our Agent configures the LLM with the toolbox, it shares the possible direct interactions the LLM can trigger when provided with the right parameters (ID, names, location, ISO datetimes). But given that an LLM is really good to interpret natural language, recognize patterns and formulate plans of execution, we can stretch the previously defined interactions letting the LLM fit the right requests on the expected tools.

To test that, lets try to request multiple locations at once (even though our interface only expect one) and make multiple cancellation, bookings and checkin/checkout updates all at once:

![Chat interaction](/images/chat1.png)

In this case our Agent correctly identifies the use case and triggers multiple calls to the configured tools to obtain the expected results (3 hotels in Basel and none in Barcelona). Next we can see how the Agent correctly completes our cancellation/booking/update request all in once action as triggered by our user.

We can check then that our requested changes were correctly persisted, after our interactions.

![BigQuery table with Hotel's data](/images/bq2.png)

Our Agent demonstrate how powerful a conversational UX implemented through a specific Agent, MCP and a correct toolset to achieve complex task in a particular domain.

The example data and functionalities are extracted directly from the MCP Toolbox [page](https://googleapis.github.io/genai-toolbox/samples/bigquery/mcp_quickstart/).

## Components

This solution integrates the following components:

* Slack, through the implementation of a Slackbot, using Bolt's SDK for Java.
* GCP CloudRun as the runtime for our services.
* BigQuery as the data repository we want to expose to our Agent.
* Anthropic's Claude LLM, who is in charge of orchestrating which datasources use and trigger needed queries or updates.
* [MCP Toolbox for Databases](https://googleapis.github.io/genai-toolbox/getting-started/introduction/), deployed as a CloudRun service and in charge of exposing the access to GCP databases to LLMs through MCP.
* Spring Boot service exposing the Slackbot functionality.

The code in the repository is arranged in 2 main folders.

- **`src/`**: This directory houses the source code for the Java Spring Boot application. It includes all the modules and functionalities of the Slackbot service.
- **`infra/`**: This directory contains Terraform scripts responsible for defining and deploying the necessary Google Cloud Platform (GCP) infrastructure for the service.

Other key files in the repository.

- **`Dockerfile`**: This file contains the instructions to build a Docker image for the application, enabling containerization and consistent deployment.
- **`cloudbuild.yaml`**: This file defines the CI/CD pipeline configurations for Google Cloud Build, automating the build, test, and deployment processes.
- **`deploy.sh`**: This script automates the deployment of both the application and the underlying GCP infrastructure.
- **`cleanup.sh`**: This script is used to remove all resources deployed by the `deploy.sh` script, ensuring a clean environment post-testing or decommissioning.

## Setup and Deployment

This section guides you through setting up your environment and deploying the Slackbot service on GCP resources.

### Prerequisites

Before you begin, ensure you have the following tools installed and configured:

- **Google Cloud SDK**: Required for interacting with your GCP account and resources.
  - Installation Guide: [https://cloud.google.com/sdk/docs/install](https://cloud.google.com/sdk/docs/install)
- **Terraform**: Used for provisioning and managing the GCP infrastructure as code.
  - Installation Guide: [https://learn.hashicorp.com/tutorials/terraform/install-cli](https://learn.hashicorp.com/tutorials/terraform/install-cli)

#### Slack Application Setup

To integrate the bot with your Slack workspace, you need to create and configure a Slack App.

1.  **Create a Slack App**:
    *   Go to [https://api.slack.com/apps](https://api.slack.com/apps) and click "Create New App".
    *   Choose "From scratch", give your app a name, and select your development workspace.

2.  **Obtain Bot Token and Signing Secret**:
    *   **Bot User OAuth Token (`SLACK_BOT_TOKEN`)**:
        *   Navigate to "OAuth & Permissions" in the sidebar of your Slack app settings.
        *   Find the "Bot User OAuth Token". It usually starts with `xoxb-`.
        *   This token will be used as the value for the `SLACK_BOT_TOKEN` environment variable.
    *   **Signing Secret (`SLACK_SIGNING_SECRET`)**:
        *   Navigate to "Basic Information" in the sidebar.
        *   Scroll down to "App Credentials" and find the "Signing Secret".
        *   This secret will be used as the value for the `SLACK_SIGNING_SECRET` environment variable.

3.  **Enable Socket Mode**:
    *   In your Slack app settings, go to "Settings" > "Socket Mode" in the sidebar.
    *   Enable Socket Mode. You might be prompted to generate an App-Level Token; this token is handled by the Spring Boot Slack SDK and typically doesn't need to be manually configured in the application properties for this project if using the default setup.

4.  **Configure Bot Token Scopes**:
    *   Go back to "OAuth & Permissions".
    *   Scroll down to "Scopes" and under "Bot Token Scopes", add the following:
        *   `app_mentions:read`: Allows the bot to read messages that directly mention it.
        *   `chat:write`: Allows the bot to send messages.
        *   `commands`: Allows the bot to receive and respond to slash commands.

5.  **Subscribe to Bot Events**:
    *   Go to "Event Subscriptions" in the sidebar.
    *   Enable Events, this should be done once the GCP infrastructure is completed, using the Slackbot service URL from Cloud Run.
    *   Under "Subscribe to bot events", add the following:
        *   `app_mention`: To receive an event when your bot is mentioned.
        *   `message.channels`: (Optional, depending on desired functionality) To allow the bot to read messages in public channels it's a member of. If your bot only responds to direct mentions and slash commands, `app_mention` might be sufficient.

6.  **Environment Variable Configuration**:
    *   The `SLACK_BOT_TOKEN` and `SLACK_SIGNING_SECRET` are crucial for the Spring Boot application to communicate with Slack.
    *   These variables need to be set as environment variables for the Cloud Run service where the bot is deployed.
    *   If you are using the provided Terraform scripts (in the `infra/` directory), these variables are typically configured in your Terraform environment (e.g., as OS environment variables where you run `terraform apply`, or within a `terraform.tfvars` file). The Terraform scripts (e.g., `infra/slackapp_service.tf` or a similar resource definition for Cloud Run) are responsible for passing these variables to the Cloud Run instance.

7.  **Prepare application deployment**:
    *   After creating your Slack App and obtaining the `SLACK_BOT_TOKEN` and `SLACK_SIGNING_SECRET`, you will need to ensure these values are available to your application.
    *   If you have configured these as Terraform variables (e.g., in a `*.tfvars` file or as environment variables for the Terraform process), you will need to follow the next steps to deploy the service.

### Deployment Steps

1.  **GCP Project Setup**:
    Ensure you have a Google Cloud Project created with billing enabled. You will also need to enable necessary APIs for the services used by the application (e.g., Cloud Run, Artifact Registry, etc.).

2.  **Terraform Configuration**:
    The GCP infrastructure is managed by Terraform.
    -   Navigate to the `infra/` directory.
    -   Refer to the `infra/README.md` for detailed instructions on:
        -   Configuring necessary Terraform variables (e.g., `project_id`, `region`, Slackbot tokens and Claude's API key).
        -   Understanding the GCP resources that will be created by the Terraform scripts.

3.  **Deploy the Application and Infrastructure**:
    -   The `deploy.sh` script located at the root of the repository automates the entire deployment process.
    -   Execute the script from the root directory:
        ```bash
        sh deploy.sh
        ```
    -   This script performs the following actions:
        -   Initializes Terraform and applies the configuration to provision the required GCP infrastructure (defined in the `infra/` directory).
            - BigQuery dataset and table, with dummy data
            - CloudBuild artifact registry
            - Builds the Docker image for the Spring Boot application (using the `Dockerfile`) and pushes it to Google Artifact Registry.
            - Deploys the MCP Toolbox service into Cloud Run.
            - Deploys the Slackbot service to Google Cloud Run, configuring the MPC Toolbox URL for tools discovery.

    After the script completes successfully, the Slack bot should be deployed and running on Cloud Run.

Once these steps are completed and the application is redeployed with the correct Slack tokens and Claude API key, your bot should be able to connect to your Slack workspace and respond to user mentions. Be in mind that some responses may take longer than others, some roundtrips can take long (Slackbot service - LLM - Slackbot service - MCP interaction - new LLM interaction - response to Slack).

## Cleanup

This section describes how to remove all Google Cloud Platform (GCP) resources that were deployed by this solution.

To remove all the deployed services and infrastructure:

1.  Navigate to the root directory of the repository.
2.  Execute the `cleanup.sh` script:
    ```bash
    sh cleanup.sh
    ```

This script automates the process of tearing down the resources. It primarily uses the `terraform destroy` command within the `infra/` directory to remove all resources managed by Terraform.

**Warning**: Running the `cleanup.sh` script is a destructive action. It will permanently delete all the deployed GCP services and infrastructure associated with this application, including any data stored within them. Ensure you no longer need the deployed instance before running this script.