variable "project_id" {
  description = "The Google Cloud project ID."
  type        = string
}

variable "region" {
  description = "The Google Cloud region."
  type        = string
  default     = "us-central1"
}

variable "service_name" {
  description = "The name of the Cloud Run service."
  type        = string
  default     = "spring-boot-app"
}

variable "repository_name" {
  description = "The name of the Cloud Source Repository."
  type        = string
  default     = "spring-boot-app-repo"
}

variable "artifact_registry_repository_name" {
  description = "The name of the Artifact Registry repository."
  type        = string
  default     = "spring-boot-app-images"
}
