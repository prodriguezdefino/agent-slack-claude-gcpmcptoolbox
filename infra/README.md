# Terraform Configuration for Spring Boot Cloud Run Deployment

This directory contains Terraform code to provision the necessary Google Cloud infrastructure for deploying the Spring Boot application to Cloud Run. Builds are managed by Cloud Build, and deployments are managed by Terraform.

## Prerequisites

1.  **Google Cloud SDK:** Install and initialize the [Google Cloud SDK](https://cloud.google.com/sdk/docs/install). Ensure you are authenticated (`gcloud auth login`) and your default project is set (`gcloud config set project YOUR_PROJECT_ID`).
2.  **Terraform:** Install [Terraform](https://learn.hashicorp.com/tutorials/terraform/install-cli).
3.  **Permissions:** Ensure the user or service account running Terraform has the necessary permissions to create the resources defined (e.g., Project Owner, or a custom role with relevant permissions for Cloud Source Repositories, Artifact Registry, Cloud Build, Cloud Run, and to submit builds).

## Setup and Deployment

1.  **Clone the repository:**
    ```bash
    # If you haven't already, clone the main application repository
    git clone <your-repo-url>
    cd <your-repo-name>
    ```

2.  **Configure Terraform Variables:**
    - Navigate to the `infra` directory:
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
      ```

3.  **Initialize Terraform:**
    ```bash
    terraform init
    ```

4.  **Apply Configuration & Trigger Build:**
    ```bash
    terraform plan
    terraform apply
    ```
    - Type `yes` when prompted to confirm resource creation/modification.
    - If there are changes to your Java application code in the `../src` directory or to `../pom.xml`, running `terraform apply` will also trigger a Google Cloud Build (via a local execution). This build will containerize your application and push the image to Artifact Registry.
    - Terraform will then ensure the Cloud Run service is deployed with the specified image (e.g., the one tagged 'latest').

5.  **Developing and Redeploying:**
    - Make changes to your Java application code in the `../src` directory or to `../pom.xml`.
    - Run `terraform apply` again from the `infra` directory.
        - This will trigger a new image build and push it to Artifact Registry.
        - Terraform will then update the Cloud Run service to use the new image.

6.  **Pushing Code (Good Practice):**
    - It's essential to commit and push your code (including application code, `Dockerfile`, `cloudbuild.yaml`, and `infra` directory changes) to your Git repository.
      ```bash
      git add .
      git commit -m "Updated application and/or infra"
      # Example: git push origin main 
      ```

7.  **Accessing the Application:**
    - The `terraform apply` command will output the URL of the deployed Cloud Run service (`cloud_run_service_url`).

## File Structure

-   `infra/main.tf`: Defines all the GCP resources.
-   `infra/variables.tf`: Declares the variables used in the configuration.
-   `infra/terraform.tfvars.example`: Example variable definitions.
-   `infra/README.md`: This file.
-   `Dockerfile` (root): Used to containerize the Spring Boot application.
-   `cloudbuild.yaml` (root): Defines the CI/CD pipeline steps executed by Cloud Build (build and push image).

## Cleaning Up

To remove all the resources created by this Terraform configuration:

```bash
terraform destroy
```
Type `yes` when prompted.
```
