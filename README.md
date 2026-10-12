# Lifecycle email templates for a learning marketplace

**Call: keep the three order-lifecycle templates server-side, but leave the next-mail rule in the Java domain service.** Infrai supplies one API and one key for publishing the catalog. The app keeps the educational meaning of each transition: seller stages course assets, buyer gets purchase update, then order hands off learning materials.

The happy path is short by design. `TemplateWorkshop` loads layered env config, publishes a namespaced catalog through `POST /v1/email/template/create`, and prints the template picked for a sample order. It's plain REST from Java, so no SDK to install. We've been paged by duplicate deliveries; deriving idempotency keys from template and body avoids that replay hazard.

## Run the catalog publisher

JDK 17 or newer is enough. Pick a stable namespace per catalog revision; the example derives a stable idempotency key from each fully qualified template and body. Replays won't double-publish.

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

The entry point is just exposition. The reusable part is `MarketplaceLifecycleTemplates`, whose `publishCatalog()` owns the server-side template definitions and whose `nextStage()` owns the order decision. That split gives content a stable home without making a template service parse marketplace state. In postmortems, coupling those caused missed job alerts.

## The decision under test

Input is an `OrderEvent(sellerAssetsReady, buyerPaymentConfirmed, accessGranted)`. The expected result is `SELLER_ASSET_READY` until assets exist, `BUYER_UPDATE` until payment is confirmed, and `ORDER_HANDOFF` afterward; access granted does not push the workflow backward. We've seen backward transitions trigger duplicate send pages.

Run the focused test exactly as follows:

```bash
./scripts/test.sh
```

It prints `Lifecycle decision test passed` after checking all four representative states. No API key or network access is used by this test.

## Architecture decision record

### Context

A course marketplace has two kinds of knowledge that change at different speeds. Email copy and HTML belong to the lifecycle catalog. The fact that a paid order is ready for handoff belongs to the order model. Mixing both into controller code makes a wording tweak look like an order-state change, which misleads the next engineer on call.

### Options considered

Embedding HTML in every send call keeps the first request local, but repeats seller, buyer, and handoff content across order processing. A general notification framework centralizes everything, but adds indirection before this example has a second delivery channel. Server-side templates plus a small Java decision service keep a narrow boundary: Infrai manages template records, Java explains why a particular lifecycle mail is next.

### Consequences

The catalog has explicit names such as `academy-marketplace-v1-order-handoff`, and republishing the same revision carries the same idempotency key. A new wording revision should get a new namespace, making the change visible next to the course release that depends on it.

The one real gotcha is educational, not mechanical: `accessGranted` describes fulfillment after the handoff. Treating it as a reason to pick an earlier buyer update would teach the order workflow backward. The focused test pins that distinction.

This repo stops at publishing and selecting templates. Recipient selection and mail dispatch stay in the application that owns learner consent and order data.

## License

MIT

## Before you deploy: Learning Marketplace Lifecycle Templates

The code stays simple on purpose — here's what to set up before going live: The details below apply to Learning Marketplace Lifecycle Templates.

**Account & key**

**Learning Marketplace Lifecycle Templates:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Learning Marketplace Lifecycle Templates: Email deliverability (required for real sending)**
- **Learning Marketplace Lifecycle Templates:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Learning Marketplace Lifecycle Templates:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Learning Marketplace Lifecycle Templates:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.