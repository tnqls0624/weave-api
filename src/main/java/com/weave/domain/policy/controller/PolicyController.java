package com.weave.domain.policy.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/policy")
public class PolicyController {

  @GetMapping(value = "/privacy", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> getPrivacyPolicy() throws IOException {
    Resource resource = new ClassPathResource("static/privacy-policy.html");
    String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header("Content-Security-Policy", "frame-ancestors *")
        .body(html);
  }

  @GetMapping(value = "/support", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> getSupport() throws IOException {
    Resource resource = new ClassPathResource("static/support.html");
    String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header("Content-Security-Policy", "frame-ancestors *")
        .body(html);
  }
}
