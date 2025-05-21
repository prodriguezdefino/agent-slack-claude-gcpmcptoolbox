/*
 * Copyright (C) 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.example.gcp.slack.claude.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAiToolboxConfig {

  private static final Logger logger = LoggerFactory.getLogger(GenAiToolboxConfig.class);

  @Value("${genai.toolbox.service.url}")
  private String toolboxServiceUrl;

  /* @Bean
  public WebClient toolboxWebClient() {
      if (toolboxServiceUrl == null || toolboxServiceUrl.isBlank()) {
          logger.warn("genai.toolbox.service.url is not configured. ToolboxWebClient will not be functional.");
          // Return a non-functional client or throw an exception, depending on desired behavior
          // For now, let's return a client that will likely fail if used, to highlight the misconfiguration.
          return WebClient.builder().baseUrl("http://localhost:INVALID_PORT").build();
      }

      // Note on Authentication:
      // For a production setup where the Cloud Function calls a Cloud Run service (genai-toolbox),
      // authentication should be handled using GCP's standard mechanisms.
      // This typically involves:
      // 1. Ensuring the Cloud Function's service account has permission to invoke the Cloud Run service.
      // 2. Modifying this WebClient configuration to:
      //    a. Obtain an OIDC identity token for the Cloud Function's service account.
      //       The audience for this token would be the URL of the `genai-toolbox` service.
      //    b. Attach this token as a "Authorization: Bearer <TOKEN>" header to requests.
      //
      // Example using GoogleCredentials (would require com.google.auth:google-auth-library-oauth2-http dependency):
      //
      // import com.google.auth.oauth2.GoogleCredentials;
      // import com.google.auth.oauth2.IdTokenCredentials;
      // import com.google.auth.oauth2.IdTokenProvider;
      //
      // GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
      // if (credentials instanceof IdTokenProvider) {
      //     IdTokenProvider idTokenProvider = (IdTokenProvider) credentials;
      //     IdTokenCredentials idTokenCredentials = IdTokenCredentials.newBuilder()
      //         .setIdTokenProvider(idTokenProvider)
      //         .setTargetAudience(toolboxServiceUrl) // The URL of the target Cloud Run service
      //         .build();
      //     idTokenCredentials.refreshIfExpired(); // Ensure token is fresh
      //     String idToken = idTokenCredentials.idToken();
      //     // Add "Authorization: Bearer " + idToken to defaultHeaders
      // }
      //
      // For now, we are setting up the WebClient without explicit auth headers.

      return WebClient.builder()
              .baseUrl(toolboxServiceUrl)
              .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .build();
  } */
}
