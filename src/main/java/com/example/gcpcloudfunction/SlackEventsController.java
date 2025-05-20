package com.example.gcpcloudfunction;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_servlet.BoltJakartaServletAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SlackEventsController {

    private static final Logger logger = LoggerFactory.getLogger(SlackEventsController.class);

    private final BoltJakartaServletAdapter adapter;

    @Autowired
    public SlackEventsController(App slackApp) {
        this.adapter = new BoltJakartaServletAdapter(slackApp);
    }

    @PostMapping("/slack/events")
    public ResponseEntity<Void> handleSlackEvents(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.info("Received Slack event request");
        // BoltJakartaServletAdapter handles request verification and dispatching
        adapter.service(request, response);
        return ResponseEntity.ok().build();
    }

    // This is a fallback for GCP Cloud Functions if direct servlet integration is tricky
    // GCP might not pass the full HttpServletRequest/Response in a way Bolt expects
    // when not using the spring-cloud-function-adapter-gcp's specific entry points.
    // For HTTP functions, GCP typically gives you HttpRequest and HttpResponse objects.
    // If the adapter.service doesn't work as expected in GCP, we might need to use
    // a more manual approach or ensure the GCP adapter for Spring Cloud Function
    // correctly wraps these into Servlet API compatible objects.

    // The spring-cloud-function-adapter-gcp should ideally handle the translation
    // from GCP's function invocation (e.g., HttpFunction) to a Servlet environment
    // that Spring Boot and Bolt can work with. If that's the case, adapter.service()
    // should work.

    // Let's also consider what happens if we're using Spring Cloud Function's Function<T,R>
    // model directly for the GCP Cloud Function. In that case, the Bolt adapter
    // might not be the right tool, and we'd need to manually verify and process.
    // However, the subtask implies using Spring MVC controller.

}
