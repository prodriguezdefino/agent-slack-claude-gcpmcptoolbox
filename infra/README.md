# Terraform Configuration for Spring Boot Cloud Run Deployment

This directory contains Terraform code to provision the necessary Google Cloud infrastructure for deploying the Spring Boot application to Cloud Run. It also includes a mechanism to trigger application builds via Google Cloud Build when local code changes are detected during `terraform apply`.

## Prerequisites (Client-Side)

1.  **Google Cloud SDK:** Install and initialize the [Google Cloud SDK](https://cloud.google.com/sdk/docs/install). Ensure you are authenticated (`gcloud auth login`) and your default project is set (`gcloud config set project YOUR_PROJECT_ID`). The `gcloud` command-line tool must be available in your PATH and configured to use the project where the resources will be deployed.
2.  **Terraform:** Install [Terraform](https://learn.hashicorp.com/tutorials/terraform/install-cli).
3.  **Permissions:** Ensure the user or service account running Terraform (and therefore `gcloud` commands via `local-exec`) has the necessary permissions:
    *   To create/manage the GCP resources defined in `main.tf` (e.g., Project Owner, or a custom role with relevant permissions for Cloud Source Repositories, Artifact Registry, Cloud Run).
    *   To submit builds to Cloud Build (e.g., `roles/cloudbuild.builds.editor`).
    *   To allow the Cloud Build service account to deploy to Cloud Run and access Artifact Registry (these are typically configured by enabling APIs or might need explicit IAM if using fine-grained permissions).

## Setup and Deployment

1.  **Clone the repository:**
    ```bash
    # If you haven't already, clone the main application repository
    git clone <your-repo-url>
    cd <your-repo-name>
    ```

2.  **Configure Terraform Variables:**
    - Navigate to the `infra` directory (this directory):
      ```bash
      cd infra
      ```
    - Create a `terraform.tfvars` file by copying the example:
      ```bash
      cp terraform.tfvars.example terraform.tfvars
      ```
    - Edit `terraform.tfvars` and replace `your-gcp-project-id` with your actual Google Cloud Project ID. You can also change the `region` if needed.
      ```terraform
      project_id = "your-actual-gcp-project-id"
      region     = "us-central1" // Or your preferred region
      // You can also override other variables defined in variables.tf here
      // service_name                       = "my-spring-app"
      // repository_name                    = "my-spring-app-repo" // Still creates CSR, useful for code backup
      // artifact_registry_repository_name  = "my-spring-app-images"
      ```

3.  **Initialize Terraform:**
    From the `infra` directory:
    ```bash
    terraform init
    ```

4.  **Review the Plan and Apply Configuration:**
    From the `infra` directory:
    ```bash
    terraform plan
    terraform apply
    ```
    - Type `yes` when prompted to confirm resource creation/modification.
    - **Build Trigger:** If there are changes to your Java application code in the `../src` directory or to `../pom.xml` (compared to the state when `terraform apply` was last run), Terraform will detect this. The `null_resource` with its `local-exec` provisioner will then trigger a new Google Cloud Build.
    - This build uses `../cloudbuild.yaml` to:
        1.  Build the Docker image (using `../Dockerfile`).
        2.  Push the image to Artifact Registry.
        3.  Deploy the new image to the Cloud Run service.

5.  **Developing and Redeploying the Application:**
    - Make changes to your Java application code in the `../src` directory or to `../pom.xml`.
    - From the `infra` directory, run:
      ```bash
      terraform apply
      ```
        - If infrastructure code in `*.tf` files has changed, Terraform will apply those changes.
        - If your Java application code (in `../src` or `../pom.xml`) has changed, the `local-exec` provisioner will trigger a new Cloud Build and deployment.
        - If neither infrastructure nor relevant application code has changed, Terraform will report no changes and the build will not be triggered.

6.  **Pushing Code (Good Practice):**
    - While `terraform apply` now handles the build trigger based on local code changes, it's still essential to commit and push your code to your Git repository. This includes application code, `Dockerfile`, `cloudbuild.yaml` (from the root project directory), and the `infra` directory changes.
    - The Cloud Source Repository created by Terraform can be used for this, or any other Git remote.
      ```bash
      # Example of adding the CSR as a remote (run from project root):
      # CSR_URL=$(terraform -chdir=infra output -raw cloud_source_repository_url) # Requires project_id to be set for CSR
      # git remote add google $CSR_URL 
      
      # General git commands (run from project root):
      git add .
      git commit -m "Updated application and/or infra"
      # git push google main # Or your preferred remote and branch
      ```
    - This ensures your codebase is versioned, backed up, and available for collaboration.

7.  **Accessing the Application:**
    - After a successful `terraform apply` that includes a build and deployment, the command will output the URL of the deployed Cloud Run service (`cloud_run_service_url`). You can use this URL to access your application.

## CI/CD Flow Summary

-   **Infrastructure:** Managed by Terraform in the `infra` directory. `terraform apply` provisions and updates cloud resources.
-   **Application Build & Deployment Trigger:** Initiated by `terraform apply` (run from the `infra` directory) if changes to Java source files (`../src/**`) or `../pom.xml` are detected since the last apply. This uses `gcloud builds submit .. --config ../cloudbuild.yaml ...` via a `local-exec` provisioner within a `null_resource`.
-   **Build Process:** Defined in `../cloudbuild.yaml` (relative to the `infra` directory). It builds the Docker image (using `../Dockerfile`) and deploys to Cloud Run.
-   **Source Code Management:** Use Git for version control. The Cloud Source Repository created by Terraform is available for use as a remote, but any Git repository can be used.

## File Structure

-   `main.tf`: Defines all the GCP resources.
-   `variables.tf`: Declares the variables used in the configuration.
-   `terraform.tfvars.example`: Example variable definitions. Copy this to `terraform.tfvars` and customize it.
-   `README.md`: This file.

The `Dockerfile` (in the root of the project, i.e., `../Dockerfile` from this directory) is used to containerize the Spring Boot application.
The `cloudbuild.yaml` (in the root of the project, i.e., `../cloudbuild.yaml` from this directory) defines the CI/CD pipeline steps executed by Cloud Build.

## Cleaning Up

To remove all the resources created by this Terraform configuration (run from the `infra` directory):

```bash
terraform destroy
```
Type `yes` when prompted.
```
