package learnmarket.templates;

import learnmarket.templates.MarketplaceLifecycleTemplates.OrderEvent;
import learnmarket.templates.MarketplaceLifecycleTemplates.Stage;

public final class MarketplaceLifecycleTemplatesTest {
    public static void main(String[] args) {
        MarketplaceLifecycleTemplates service = new MarketplaceLifecycleTemplates(null, "test");
        expect(Stage.SELLER_ASSET_READY, service.nextStage(new OrderEvent(false, false, false)));
        expect(Stage.BUYER_UPDATE, service.nextStage(new OrderEvent(true, false, false)));
        expect(Stage.ORDER_HANDOFF, service.nextStage(new OrderEvent(true, true, false)));
        expect(Stage.ORDER_HANDOFF, service.nextStage(new OrderEvent(true, true, true)));
        System.out.println("Lifecycle decision test passed");
    }

    private static void expect(Stage expected, Stage actual) {
        if (expected != actual) throw new AssertionError("expected " + expected + " but got " + actual);
    }
}
