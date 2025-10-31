public class Main {

    public static void main(String[] args) {
        var console = System.console();
        if (console == null) {
            System.err.println("No console available");
            System.exit(1);
        }
        System.out.println("Oracle Cloud Infrastructure AI Database...");
        String password = new String(console.readPassword("Admin Password: "));
        String startingUrl = console.readLine("Enter a starting URL from Wikipedia: ");
        String targetUrl = console.readLine("Enter the end-goal URL from Wikipedia: ");
        try {
            String token = SemanticAPIClient.getToken(password);
            password = ""; // Clear the password if successful after token creation
            var crawler = new SemanticCrawler(startingUrl, targetUrl, token);
            crawler.beginCrawling();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            password = ""; // Clear the password if error at token creation
        }
    }
}
