package com.api.rizz.portfolio_api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  private static final String BANNER =
      """

           _____           _   __      _ _         ___   ____ ___
          |  __ \\         | | / _|    | (_)       / _ \\ / __ \\_ _|
          | |__) |__  _ __| |_| |_ ___ | |_  ___  | (_| | |  | | |
          |  ___/ _ \\| '__| __|  _/ _ \\| | |/ _ \\  \\__, | |  | | |
          | |  | (_) | |  | |_| || (_) | | | (_) |   / /| |__| | |
          |_|   \\___/|_|   \\__|_| \\___/|_|_|\\___/   /_/  \\____/___|

          Portfolio API is up and running.
          Docs / repo: https://github.com/Rizz404/portfolio-api
          """;

  @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
  public String home() {
    return BANNER;
  }
}
