
public class Main {
    public static void main(String[] args) {
        System.out.println("Learner Tool - Server Starting...");
        
        // Start a simple HTTP server for the web admin
        try {
            SimpleHttpServer server = new SimpleHttpServer();
            server.start(8080);
            System.out.println("Web admin server running on http://0.0.0.0:8080");
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
