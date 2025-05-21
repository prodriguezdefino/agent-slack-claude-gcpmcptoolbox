variable "project_id" {
  description = "The Google Cloud project ID."
  type        = string
}

variable "claude_apikey" {
  description = "The API KEY used for Claude interactions."
  type        = string
}

variable "slackbot_workspace_token" {
  description = "The workspace token created when adding the slackbot."
  type        = string
}

variable "slackapp_signing_secret" {
  description = "The signing secret for the slack app."
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
  default     = "slack-claude-mcptoolbox-integration"
}

variable "artifact_registry_repository_name" {
  description = "The name of the Artifact Registry repository."
  type        = string
  default     = "slack-claude-mcptoolbox-integration-images"
}