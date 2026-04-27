package io.github.georgecodes.betsettler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Main entry point for the bet settler application. */
@SpringBootApplication
public class BetSettlerApplication {

  /** Prevents instantiation of the application entry-point type. */
  private BetSettlerApplication() {}

  /**
   * Starts the bet settler application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(BetSettlerApplication.class, args);
  }
}
