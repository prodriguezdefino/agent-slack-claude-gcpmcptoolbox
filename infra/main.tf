terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

resource "google_artifact_registry_repository" "default" {
  location      = var.region
  repository_id = var.artifact_registry_repository_name
  description   = "Docker repository for Spring Boot application"
  format        = "DOCKER"
}

resource "google_cloud_run_v2_service" "default" {
  name     = var.service_name
  location = var.region

  template {
    # Add annotations to force a new revision when code changes
    annotations = {
      "app-code-version-src" = null_resource.cloud_build_on_change.triggers.app_src_hash
      "app-code-version-pom" = null_resource.cloud_build_on_change.triggers.pom_xml_hash
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.default.repository_id}/${var.service_name}:latest" # Placeholder, will be updated by Cloud Build
      resources {
        limits = {
          memory = "1024Mi"
          cpu    = "2"
        }
      }
      ports {
        container_port = 8080
      }
      env {
        name  = "CLAUDE_API_KEY"
        value = var.claude_apikey
      }
      env {
        name  = "SLACK_BOT_TOKEN"
        value = var.slackbot_workspace_token
      }
      env {
        name  = "SLACK_SIGNING_SECRET"
        value = var.slackapp_signing_secret
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [google_artifact_registry_repository.default]
}

# Allow unauthenticated invocations for the Cloud Run service
resource "google_cloud_run_service_iam_member" "allow_unauthenticated" {
  location = google_cloud_run_v2_service.default.location
  project  = google_cloud_run_v2_service.default.project
  service  = google_cloud_run_v2_service.default.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

data "archive_file" "app_src_archive" {
  type        = "zip"
  source_dir  = "../src"
  output_path = ".terraform/temp/app_src.zip"
}

data "local_file" "pom_xml" {
  filename = "../pom.xml"
}

resource "null_resource" "cloud_build_on_change" {
  triggers = {
    # Trigger based on the hash of the archived src directory
    app_src_hash = data.archive_file.app_src_archive.output_sha,
    # Trigger based on the hash of pom.xml
    pom_xml_hash = filesha256("../pom.xml")
  }

  provisioner "local-exec" {
    # Command to execute. Note the '..' to go to the parent directory (project root)
    # where cloudbuild.yaml and the source code are located.
    command = <<EOT
      echo "Code changes detected, triggering Cloud Build..."
      gcloud builds submit . --project ${var.project_id} --config cloudbuild.yaml --substitutions=_SERVICE_NAME=${var.service_name},_REGION=${var.region},_ARTIFACT_REGISTRY_REPO_NAME=${google_artifact_registry_repository.default.repository_id} --quiet
    EOT
    # Set working_directory if needed, but gcloud submit with '..' should handle paths correctly
    # from the infra directory.
    working_dir = "../"
  }

  # This part is to ensure the Artifact Registry repository is created before this tries to use its name.
  # And that the Cloud Source Repo exists if any local tooling expects it (though not strictly needed for this trigger).
  depends_on = [
    google_artifact_registry_repository.default,
  ]
}

output "cloud_run_service_url" {
  description = "The URL of the deployed Cloud Run service."
  value       = google_cloud_run_v2_service.default.uri
}

output "artifact_registry_repository_url" {
  description = "The URL of the Artifact Registry repository."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.default.repository_id}"
}
