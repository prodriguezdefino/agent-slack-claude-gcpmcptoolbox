# Slack Claude GCP MCPtoolbox Integration

This repository contains a integration example for a Slack application, implemented with Slackbot service, which integrates chat-like communication with Anthropic's Claude LLM and can discover Google Cloud Platform (GCP) datasources through an MCP based server. The core application is built using Java and Spring Boot.

## Components

- **`src/`**: This directory houses the source code for the Java Spring Boot application. It includes all the modules and functionalities of the Slackbot service.
- **`infra/`**: This directory contains Terraform scripts responsible for defining and deploying the necessary Google Cloud Platform (GCP) infrastructure for the service.

### Key Files at Root Level

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

## Example User Interaction

Once all the components are deployed and configured correctly, and the Slack app was added to a workspace we can ask a question to the bot by mentioning it on a channel. But first lets check the BigQuery table containing the data we will be trying to query.

![BigQuery table with Hotel's data](/images/bq1.png)

We can then ask the bot to retrieve hotels based on their location:

![Chat interaction](/images/chat1.png)

After the Slackbot completes the interaction we can inspect the BigQuery table to see the changes propagated.

![BigQuery table with Hotel's data](/images/bq2.png)

The example data and functionalities are extracted directly from the MCP Toolbox [page](https://googleapis.github.io/genai-toolbox/samples/bigquery/mcp_quickstart/). In there you can see what other interactions can be used right away while accessing and updating data on BigQuery, and also how to add other GCP databases to the MCP service configured.

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