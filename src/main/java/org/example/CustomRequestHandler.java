package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class CustomRequestHandler extends CefRequestHandlerAdapter {

    @Override
    public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {

        String url = request.getURL();
        System.out.println("Navigating to: " + url);
        // Check for specific query parameter
        if (containsQueryParameter(url, "id_token").length() > 0) {
            String idToken = extractIdTokenFromUrl(url);
            String sessionId = getSessionId(idToken);
            writeAccountsToTxt(sessionId);
            System.out.println("The URL contains the specified id_token query parameter.");
            KillJavaProcess.kill();
        } else if (containsQueryParameter(url, "code").length() > 0) {
            String code = extractCodeFromUrl(url);
            getToken(code);
            System.out.println("The URL contains the specified code query parameter.");
        }
        {
            System.out.println("The URL does not contain the specified query parameter.");
        }

        return super.onBeforeBrowse(browser, frame, request, user_gesture, is_redirect);
    }

    private void getToken(String code) {
        try {
            // Prepare the post data
            String postData = "grant_type=authorization_code"
                    + "&client_id=com_jagex_auth_desktop_launcher"
                    + "&code=" + code
                    + "&code_verifier=" + Main.codeVerifier
                    + "&redirect_uri=https://secure.runescape.com/m=weblogin/launcher-redirect";

            // Create a URL object
            URL obj = new URL("https://account.jagex.com/oauth2/token");

            // Open a connection
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // Set the request method to POST
            con.setRequestMethod("POST");

            // Set the Content-Type header
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Enable writing output to the connection
            con.setDoOutput(true);

            // Write the post data to the output stream
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = con.getResponseCode();
            System.out.println("POST Response Code :: " + responseCode);

            // If the response code is 200, read the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response (you can implement a method to do so)
                String response = readResponse(con);
                System.out.println("Response: " + response);
                getIdToken(response);
            } else {
                System.out.println("POST request failed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getIdToken(String response) {
        JSONObject jsonResponse = new JSONObject(response);
        String jwt = jsonResponse.getString("id_token");

        // Generate a random nonce
        String nonce = generateNonce(48);

        // Prepare the state parameter (you need to define how to get or generate this)

        // Construct the URL
        String url = "https://account.jagex.com/oauth2/auth?id_token_hint=" + jwt
                + "&nonce=" + nonce
                + "&prompt=consent"
                + "&redirect_uri=http%3A%2F%2Flocalhost"
                + "&response_type=id_token+code"
                + "&state=" + Main.state
                + "&client_id=1fddee4e-b100-4f4e-b2b0-097f9088f9d2"
                + "&scope=openid+offline";

        // Print instructions to the user
        System.out.println("Go to this URL, let it do its thing, and bring me back your 'id_token'\n");
        System.out.println(url + "\n");

        MainFrame.openNewTab(url);
    }

    public String getSessionId(String idToken) {
        try {
            // Prepare the POST data as a JSON object
            String postData = "{\"idToken\":\"" + idToken + "\"}";

            // Create a URL object for the endpoint
            URL url = new URL("https://auth.jagex.com/game-session/v1/sessions");

            // Open a connection
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            con.setRequestMethod("POST");

            // Set the Content-Type header to indicate JSON data
            con.setRequestProperty("Content-Type", "application/json");

            // Enable output for the connection
            con.setDoOutput(true);

            // Write the JSON data to the output stream
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = postData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code to check if the request was successful
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response (similar to the readResponse method)
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Convert response to a string
                JSONObject sessionIdJson = new JSONObject(response.toString());

                System.out.println("Session ID fetched succesfully.");

                return sessionIdJson.getString("sessionId");

            } else {
                System.out.println("POST request fetching sessionId failed with response code: " + responseCode);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void writeAccountsToTxt(String sessionId) {
        try {
            // Step 1: Make the GET request with a Bearer token
            String apiUrl = "https://auth.jagex.com/game-session/v1/accounts";
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + sessionId);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Step 2: Parse the JSON response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse the JSON array
                Gson gson = new Gson();
                List<Account> newAccounts = gson.fromJson(response.toString(), new TypeToken<List<Account>>() {
                }.getType());

                for (Account account : newAccounts) {
                    account.sessionId = sessionId;
                    account.createdOn = new Date();
                }
                String userHome = System.getProperty("user.home");
                Path directoryPath = Paths.get(userHome, ".microbot");
                Path filePath = Paths.get(directoryPath.toString(), "accounts.json");

                // Step 3: Read existing accounts.json if it exists
                File file = new File(filePath.toString());
                file.createNewFile(); // Ensures the file exists, though unnecessary if you're checking existence below
                List<Account> existingAccounts = new ArrayList<>();

                if (file.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        existingAccounts = gson.fromJson(reader, new TypeToken<List<Account>>() {
                        }.getType());
                    }
                }

                if (existingAccounts == null) {
                    existingAccounts = new ArrayList<>();
                }

                // Set to keep track of existing account IDs
                Set<String> existingAccountIds = existingAccounts.stream()
                        .map(Account::getAccountId)
                        .collect(Collectors.toSet());

                // Filter new accounts and only add those with non-duplicate account IDs
                List<Account> nonDuplicateNewAccounts = newAccounts.stream()
                        .filter(account -> !existingAccountIds.contains(account.getAccountId()))
                        .collect(Collectors.toList());

                // Combine the filtered new accounts with the existing accounts
                existingAccounts.addAll(nonDuplicateNewAccounts);

                // Step 4: Write the combined data back to accounts.json
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toString()))) {
                    gson.toJson(existingAccounts, writer);
                } catch (IOException e) {
                    e.printStackTrace(); // Log the exception for debugging
                }

                System.out.println("Accounts data written to accounts.json");
            } else {
                System.out.println("GET request failed with response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String generateNonce(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder nonce = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            nonce.append(characters.charAt(random.nextInt(characters.length())));
        }
        return nonce.toString();
    }

    public static String extractIdTokenFromUrl(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            System.out.println("Failed to parse URI");
        }
        String fragment = uri.getFragment(); // Get the fragment part of the URL (after #)

        if (fragment != null) {
            Map<String, String> params = splitQuery(fragment);
            return params.get("id_token");
        }
        return null;
    }

    private static Map<String, String> splitQuery(String query) {
        Map<String, String> queryPairs = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return queryPairs;
    }

    public static String extractCodeFromUrl(String url) {
        // Split the URL by comma since the parameters are separated by commas
        String[] parameters = url.split(",");

        // Loop through the parameters to find the one that starts with the desired key
        for (String parameter : parameters) {
            if (parameter.startsWith("jagex:code=")) {
                // Return the value after the '=' sign
                return parameter.substring(parameter.indexOf('=') + 1);
            }
        }

        // If the parameter is not found, return null
        return null;
    }

    private String containsQueryParameter(String urlString, String queryKey) {
        if (urlString.contains(queryKey + "=")) {
            return urlString;
        }
        return "";
    }

    public static String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();

        // Try with resources to automatically close the BufferedReader
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {

            String inputLine;

            // Read the input line by line
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        // Return the response as a string
        return response.toString();
    }

    static class Account {
        private String sessionId;
        private String accountId;
        private String displayName;
        private String userHash;
        private Date createdOn;

        public String getSessionId() {
            return sessionId;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getUserHash() {
            return userHash;
        }
    }
}
