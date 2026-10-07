package learnmarket.config;

public record EmailTemplateConfig(String apiKey, String namespace) {
    public static EmailTemplateConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        String namespace = System.getenv().getOrDefault("TEMPLATE_NAMESPACE", "learning-marketplace-v1");
        return new EmailTemplateConfig(key, namespace);
    }
}
