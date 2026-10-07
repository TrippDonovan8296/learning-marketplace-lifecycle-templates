package learnmarket;

import learnmarket.config.EmailTemplateConfig;
import learnmarket.infrai.InfraiEmailTemplates;
import learnmarket.templates.MarketplaceLifecycleTemplates;
import learnmarket.templates.MarketplaceLifecycleTemplates.OrderEvent;

import java.util.Map;

public final class TemplateWorkshop {
    private TemplateWorkshop() {}

    public static void main(String[] args) {
        EmailTemplateConfig config = EmailTemplateConfig.fromEnvironment();
        MarketplaceLifecycleTemplates catalog = new MarketplaceLifecycleTemplates(
                new InfraiEmailTemplates(config.apiKey()), config.namespace());

        Map<MarketplaceLifecycleTemplates.Stage, String> templateIds = catalog.publishCatalog();
        try {
            var sampleOrder = new OrderEvent(true, true, false);
            var next = catalog.nextStage(sampleOrder);

            System.out.println("Published lifecycle template IDs: " + templateIds);
            System.out.println("Sample order next mail: " + next + " -> " + templateIds.get(next));
        } finally {
            catalog.deleteCatalog(templateIds);
        }
    }
}
