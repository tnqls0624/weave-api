package com.weave;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class WeaveApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(WeaveApiApplication.class, args);
    log.info("weave api start success");
  }
}
