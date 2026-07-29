import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple console-based AI chatbot using the Google Gemini API.
 *
 * Setup:
 *   1. Get an API key from https://aistudio.google.com/ (click the key icon)
 *   2. Set it as an environment variable named GEMINI_API_KEY
 *      (see README.md for how to do this on your OS)
 *
 * Run:
 *   javac ChatbotApp.java
 *   java ChatbotApp
 */
public class ChatbotApp {

    private static final String MODEL = "gemini-flash-latest"; // fast + free-tier friendly
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

    public static void main(String[] args) {
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("ERROR: No API key found.");
            System.out.println("Please set the GEMINI_API_KEY environment variable before running.");
            System.out.println("See README.md for instructions.");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================");
        System.out.println("   Java AI Chatbot (type 'exit' to quit)");
        System.out.println("=================================");

        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (userInput.isBlank()) {
                continue;
            }

            try {
                String reply = getAIResponse(client, apiKey, userInput);
                System.out.println("Bot: " + reply);
            } catch (Exception e) {
                System.out.println("Something went wrong talking to the AI: " + e.getMessage());
            }
        }

        scanner.close();
    }

    /**
     * Sends the user's message to the Gemini API and returns the AI's reply text.
     */
    private static String getAIResponse(HttpClient client, String apiKey, String userMessage)
            throws IOException, InterruptedException {

        String escapedMessage = escapeJson(userMessage);

        String systemInstruction =  systemInstruction =
                "You are a helpful AI assistant. Answer user questions clearly, accurately, and politely.";
        String requestBody = "{"
                + "\"system_instruction\": {\"parts\": [{\"text\": \"" + escapeJson(systemInstruction) + "\"}]},"
                + "\"contents\": [{\"parts\": [{\"text\": \"" + escapedMessage + "\"}]}]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return "API error (status " + response.statusCode() + "): " + response.body();
        }

        return extractContent(response.body());
    }

    /**
     * Extracts the reply text from the Gemini JSON response without
     * needing an external JSON library. Good enough for a learning project.
     */
    private static String extractContent(String json) {
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)(?<!\\\\)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return "(Could not parse a response. Raw JSON: " + json + ")";
    }

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String text) {
        return text
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}

