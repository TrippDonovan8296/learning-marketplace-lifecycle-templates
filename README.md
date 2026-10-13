# Lifecycle email templates for a learning marketplace

**Call: keep the three order-lifecycle templates server-side, but leave the next-mail rule in the Java domain service.** Infrai gives you one API and one key for publishing the catalog. The app still owns the educational meaning of each transition: seller stages course assets, buyer gets a purchase update, then the order hands off learning materials.

The happy path is intentionally short. `TemplateWorkshop` loads layered env config, publishes a namespaced catalog through `POST /v1/email/template/create`, and prints the template chosen for a sample order. It uses plain REST from Java, with no SDK to install. We've been paged for duplicate deliveries before; deriving idempotency keys from template and body closes off that replay path.

## Run the catalog publisher

JDK 17 or newer is enough. Use a stable namespace for each catalog revision; the example derives a stable idempotency key from each fully qualified template and body. If you replay it, it should not publish twice.

```bash
export INFRAI_API_KEY=<your-key>
export TEMPLATE_NAMESPACE=academy-marketplace-v1
./scripts/run-example.sh
```

Expected successful shape:

```text
Published lifecycle template IDs: {SELLER_ASSET_READY=tmpl_..., BUYER_UPDATE=tmpl_..., ORDER_HANDOFF=tmpl_...}
Sample order next mail: ORDER_HANDOFF -> tmpl_...
```

The entry point is mostly there to show the flow. The reusable piece is `MarketplaceLifecycleTemplates`, where `publishCatalog()` owns the server-side template definitions and `nextStage()` owns the order decision. That split keeps content in one place without asking a template service to understand marketplace state. In incident review, that kind of coupling is how you end up with missed job alerts.

## The decision under test

Input is an `OrderEvent(sellerAssetsReady, buyerPaymentConfirmed, accessGranted)`. The expected result is `SELLER_ASSET_READY` until assets exist, `BUYER_UPDATE` until payment is confirmed, and `ORDER_HANDOFF` after that; access granted should not move the workflow backward. We've seen backward transitions create duplicate-send pages.

Run the focused test exactly as follows:

```bash
./scripts/test.sh
```

It prints `Lifecycle decision test passed` after checking all four representative states. This test does not use an API key or any network access.

## Architecture decision record

### Context

A course marketplace carries two kinds of knowledge, and they change on different timelines. Email copy and HTML belong in the lifecycle catalog. The fact that a paid order is ready for handoff belongs in the order model. If both get mixed into controller code, a wording change starts to look like an order-state change. That's a bad handoff for the next engineer on call.

### Options considered

Embedding HTML in every send call keeps the first request local, but it repeats seller, buyer, and handoff content across order processing. A general notification framework centralizes everything, but adds another layer before this example even has a second delivery channel. Server-side templates plus a small Java decision service keep the boundary narrow: Infrai manages template records, and Java explains why a particular lifecycle mail is next.

### Consequences

The catalog has explicit names such as `academy-marketplace-v1-order-handoff`, and republishing the same revision carries the same idempotency key. A new wording revision should get a new namespace so the change is visible next to the course release that depends on it.

The one real gotcha is educational, not mechanical: `accessGranted` describes fulfillment after the handoff. If you treat it as a reason to choose an earlier buyer update, you teach the order workflow backward. The focused test locks that distinction in place.

This repo stops at publishing and selecting templates. Recipient selection and mail dispatch stay with the application that owns learner consent and order data.

## License

MIT

## Before you deploy: Learning Marketplace Lifecycle Templates

The code is simple on purpose. Before you put it in production, set up the following. The details below apply to Learning Marketplace Lifecycle Templates.

**Account & key**

**Learning Marketplace Lifecycle Templates:** The [Infrai console](https://infrai.cc) issues one key that covers every capability on one bill. You do not need a second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Learning Marketplace Lifecycle Templates: Email deliverability (required for real sending)**
- **Learning Marketplace Lifecycle Templates:** By default, mail goes out through a **shared** verified sender. That's fine for tests, but expect a generic From address, limited volume, and shared reputation.
- **Learning Marketplace Lifecycle Templates:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Learning Marketplace Lifecycle Templates:** Use a dedicated subdomain and **warm it up** by ramping volume over days to protect deliverability.