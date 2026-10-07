package learnmarket.templates;

import learnmarket.infrai.InfraiEmailTemplates;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarketplaceLifecycleTemplates {
    public enum Stage { SELLER_ASSET_READY, BUYER_UPDATE, ORDER_HANDOFF }

    public record OrderEvent(boolean sellerAssetsReady, boolean buyerPaymentConfirmed, boolean accessGranted) {}
    public record TemplateDraft(Stage stage, String suffix, String subject, String html) {}

    private final InfraiEmailTemplates templates;
    private final String namespace;

    public MarketplaceLifecycleTemplates(InfraiEmailTemplates templates, String namespace) {
        this.templates = templates;
        this.namespace = namespace;
    }

    public Stage nextStage(OrderEvent event) {
        if (!event.sellerAssetsReady()) return Stage.SELLER_ASSET_READY;
        if (!event.buyerPaymentConfirmed()) return Stage.BUYER_UPDATE;
        return Stage.ORDER_HANDOFF;
    }

    public Map<Stage, String> publishCatalog() {
        Map<Stage, String> ids = new LinkedHashMap<>();
        try {
            for (TemplateDraft draft : drafts()) {
                String name = namespace + "-" + draft.suffix();
                Map<String, Object> data = templates.create(
                        name, draft.subject(), draft.html(), "template-" + digest(name + draft.html()));
                ids.put(draft.stage(), String.valueOf(data.get("template_id")));
            }
            return ids;
        } catch (RuntimeException failure) {
            deleteCatalog(ids, failure);
            throw failure;
        }
    }

    public void deleteCatalog(Map<Stage, String> ids) {
        deleteCatalog(ids, null);
    }

    private void deleteCatalog(Map<Stage, String> ids, RuntimeException failure) {
        RuntimeException firstFailure = failure;
        for (String templateId : ids.values()) {
            try {
                templates.delete(templateId);
            } catch (RuntimeException cleanupFailure) {
                if (firstFailure == null) firstFailure = cleanupFailure;
                else firstFailure.addSuppressed(cleanupFailure);
            }
        }
        if (failure == null && firstFailure != null) throw firstFailure;
    }

    public List<TemplateDraft> drafts() {
        return List.of(
                new TemplateDraft(Stage.SELLER_ASSET_READY, "seller-assets",
                        "Prepare the learning assets for {{course_title}}",
                        "<h1>{{course_title}}</h1><p>Upload the workbook and lesson links for order {{order_id}}.</p>"),
                new TemplateDraft(Stage.BUYER_UPDATE, "buyer-update",
                        "Your order for {{course_title}} is confirmed",
                        "<h1>You're enrolled</h1><p>We are preparing access for order {{order_id}}.</p>"),
                new TemplateDraft(Stage.ORDER_HANDOFF, "order-handoff",
                        "Your {{course_title}} materials are ready",
                        "<h1>Start learning</h1><p>Open {{access_link}} to begin order {{order_id}}.</p>")
        );
    }

    private static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < 12; i++) out.append(String.format("%02x", bytes[i]));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
