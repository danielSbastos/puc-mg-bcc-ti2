package service;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import spark.Request;
import spark.Response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuthService {
    private static final String STATE_KEY = "spotify_auth_state";

    private static final String CLIENT_ID = "3a382d598de845c1a8db261c24be5d63";
    private static final String CLIENT_SECRET = "269ad429b52b47be94781ee6d1949f56";

    private static final String REDIRECT_URL = "http://localhost:8888/callback";
    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
    private static final String ACCOUNT_URL = "https://api.spotify.com/v1/me";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    public Object login(Request request, Response response) {
         String state = "skdhfsidufhsfhuegfuywgefwe";

         String scope = "user-read-private user-read-email";
         String query = "?client_id=" + CLIENT_ID + "&scope=" + scope + "&redirect_uri=" + REDIRECT_URL + "&state=" + state + "&response_type=code";

         response.cookie("/", STATE_KEY, state, 5000, false);
         response.redirect(AUTHORIZE_URL + query);

         return "success";
    }

    public Object callback(Request request, Response response) {
        HttpClient client = HttpClient.newHttpClient();

        String code = request.queryMap("code").value();
        HttpRequest tokenRequest = tokenRequest(code);
        try {
            HttpResponse<String> tokenResponse = client.send(
                tokenRequest,
                HttpResponse.BodyHandlers.ofString()
            );
            if (tokenResponse.statusCode() == 200) {
                Map<String, Object> tokenBody = responseMapBody(tokenResponse.body());

                HttpRequest accountRequest = accountRequest((String) tokenBody.get("access_token"));
                HttpResponse<String> accountResponse = client.send(accountRequest, HttpResponse.BodyHandlers.ofString());

                if (accountResponse.statusCode() == 200) {
                    Map<String, Object> accountBody = responseMapBody(accountResponse.body());

                    response.cookie("user_id", (String) accountBody.get("id"));
                    response.cookie("access_token", (String) tokenBody.get("access_token"), Math.toIntExact((Long) tokenBody.get("expires_in")));
                    response.cookie("refresh_token", (String) tokenBody.get("refresh_token"));
                } else {
                    return "error";
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
            return "error";
        }

        return "ok";
    }

    private HttpRequest accountRequest(String accessToken) {
        return HttpRequest.newBuilder()
                .uri(URI.create(ACCOUNT_URL))
                .headers("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
    }

    private HttpRequest tokenRequest(String code) {
        String rawAuthHeader = CLIENT_ID + ':' + CLIENT_SECRET;
        String encodedString = Base64.getEncoder().encodeToString(rawAuthHeader.getBytes());

         return HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .headers("Authorization", "Basic " + encodedString, "Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("code="+ code + "&redirect_uri=" + REDIRECT_URL + "&grant_type=authorization_code"))
                .build();
    }

    private Map<String, Object> responseMapBody(String body) {
        Map <String, Object> hm = new HashMap<>();

        Object obj = JSONValue.parse(body);
        JSONObject jsonObject = (JSONObject) obj;

        for (Object o : jsonObject.keySet()) {
            String key = (String) o;
            hm.put(key, jsonObject.get(key));
        }

        return hm;
    }
}
