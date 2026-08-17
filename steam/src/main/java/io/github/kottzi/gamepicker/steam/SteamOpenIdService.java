package io.github.kottzi.gamepicker.steam;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SteamOpenIdService {

    private static final String STEAM_OPENID_ENDPOINT = "https://steamcommunity.com/openid/login";
    private static final Pattern CLAIMED_ID_PATTERN = Pattern.compile("^https://steamcommunity\\.com/openid/id/(\\d+)$");

    private final RestClient restClient;

    public SteamOpenIdService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public String buildLoginRedirectUrl(String returnToUrl, String realm) {
        return UriComponentsBuilder.fromUriString(STEAM_OPENID_ENDPOINT)
                .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
                .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
                .queryParam("openid.return_to", returnToUrl)
                .queryParam("openid.realm", realm)
                .build()
                .toUriString();
    }

    public Optional<String> verifyAndExtractSteamId(Map<String, String> callbackParams) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        callbackParams.forEach(body::add);
        body.set("openid.mode", "check_authentication");

        String response = restClient.post()
                .uri(STEAM_OPENID_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(String.class);

        if (response == null || !response.contains("is_valid:true")) {
            return Optional.empty();
        }
        String claimedId = callbackParams.get("openid.claimed_id");
        if (claimedId == null) {
            return Optional.empty();
        }
        Matcher matcher = CLAIMED_ID_PATTERN.matcher(claimedId);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
