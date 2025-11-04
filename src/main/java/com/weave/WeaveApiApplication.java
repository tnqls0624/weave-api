package com.weave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WeaveApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(WeaveApiApplication.class, args);
    System.out.println("weave api start success");
  }

}
