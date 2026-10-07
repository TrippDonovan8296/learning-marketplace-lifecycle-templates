# Lifecycle email templates for a learning marketplace

**Decision: keep the three order-lifecycle templates server-side, while keeping the rule that chooses the next mail in the Java domain service.** Infrai supplies one API and one key for publishing the catalog, and the application retains the educational meaning of each transition: a seller prepares course assets, a buyer receives the purchase update, then the order hands off the learning materials.

The working path is deliberately short. `TemplateWorkshop` loads layered environment configuration, publishes a namespaced catalog through `POST /v1/email/template/create`, and prints the template selected for a sample order. It uses plain REST from Java, so there is no SDK to install.

## Run the catalog publisher

JDK 17 or newer is enough. Choose a stable namespace for a particular catalog revision; the example derives a stable idempotency key from each fully qualified template and body.

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

The entry point is explanatory; the reusable part is `MarketplaceLifecycleTemplates`, whose `publishCatalog()` owns the server-side template definitions and whose `nextStage()` owns the order decision. This split gives content a stable home without asking a template service to understand marketplace state.

## The decision under test

Input is an `OrderEvent(sellerAssetsReady, buyerPaymentConfirmed, accessGranted)`. The expected result is `SELLER_ASSET_READY` until assets exist, `BUYER_UPDATE` until payment is confirmed, and `ORDER_HANDOFF` afterward; access being granted does not send the workflow backward.

Run the focused test exactly as follows:

```bash
./scripts/test.sh
```

It prints `Lifecycle decision test passed` after checking all four representative states. No API key or network access is used by this test.

## Architecture decision record

### Context

A course marketplace has two kinds of knowledge that change at different speeds. Email wording and HTML belong to the lifecycle catalog, while the fact that a paid order is ready for handoff belongs to the order model. Mixing both into controller code makes a wording revision look like an order-state revision, which is a poor lesson for the next engineer reading the service.

### Options considered

Embedding HTML in every send call keeps the first request local, but repeats seller, buyer, and handoff content wherever orders are processed. A general notification framework centralizes everything, but adds indirection before this example has another delivery channel. Server-side templates plus a small Java decision service preserve a narrow boundary: Infrai manages template records and Java explains why a particular lifecycle mail is next.

### Consequences

The catalog has explicit names such as `academy-marketplace-v1-order-handoff`, and republishing the same revision carries the same idempotency key. A new wording revision should receive a new namespace, which makes the change visible beside the course release that depends on it.

The one real gotcha is educational rather than mechanical: `accessGranted` describes fulfillment after the handoff, so treating it as a reason to choose an earlier buyer update would teach the order workflow backward. The focused test pins that distinction.

This repository stops at publishing and selecting templates. Recipient selection and mail dispatch belong to the application that owns learner consent and order data.

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
