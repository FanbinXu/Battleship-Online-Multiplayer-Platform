package app.battleship;

import app.battleship.config.BattleshipProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BattleshipProperties.class)
public class BattleshipApplication {
  public static void main(String[] args) {
    SpringApplication.run(BattleshipApplication.class, args);
  }
}
