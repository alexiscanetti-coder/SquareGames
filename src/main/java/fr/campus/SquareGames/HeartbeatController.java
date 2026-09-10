package fr.campus.SquareGames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeartbeatController {
    @Autowired HeartbeatSensor heartbeatSensor;

    @GetMapping("/heartbeat")
    public int heartbeat() {
        return heartbeatSensor.get();
    }
}
