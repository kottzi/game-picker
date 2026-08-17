package io.github.kottzi.gamepicker;

import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
public class VersionController {

    private final BuildProperties buildProperties;

    public VersionController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/api/version")
    public Map<String, String> version() {
        return Map.of("version", Objects.requireNonNull(buildProperties.getVersion()));
    }
}