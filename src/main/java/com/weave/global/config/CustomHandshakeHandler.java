package com.weave.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {
  @Override
  protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) {
    Object principal = attributes.get("principal");
    if (principal instanceof Principal p) {
      log.info("Assigning Principal from handshake attributes: {}", p.getName());
      return p;
    }
    // fallback to default behavior
    Principal fallback = super.determineUser(request, wsHandler, attributes);
    if (fallback != null) {
      log.info("Assigning fallback Principal: {}", fallback.getName());
    } else {
      log.info("No Principal provided, using null (anonymous)");
    }
    return fallback;
  }
}
