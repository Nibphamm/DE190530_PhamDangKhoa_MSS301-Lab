"""Generate the portable Postman collection used by Desktop and Newman."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HTTP = 'pm.test("HTTP 200", () => pm.response.to.have.status(200));\n'


def request(name, method, url, body=None, test="", pre=""):
    item = {"name": name, "request": {"method": method, "header": [], "url": url}, "event": []}
    if body is not None:
        item["request"]["header"] = [{"key": "Content-Type", "value": "application/json"}]
        item["request"]["body"] = {"mode": "raw", "raw": json.dumps(body), "options": {"raw": {"language": "json"}}}
    for kind, code in [("prerequest", pre), ("test", test)]:
        if code:
            item["event"].append({"listen": kind, "script": {"type": "text/javascript", "exec": code.splitlines()}})
    return item


SNAPSHOT = HTTP + '''
pm.response.json().forEach(p => pm.collectionVariables.set("stock_" + p.id, p.stock));
pm.sendRequest(pm.collectionVariables.get("paymentUrl") + "/api/payments/wallets", (err, res) => {
  pm.test("Wallet snapshot request succeeds", () => { pm.expect(err).to.be.null; pm.expect(res.code).to.eql(200); });
  if (!err && res.code === 200) res.json().forEach(w => pm.collectionVariables.set("bal_" + w.customerId, w.balance));
});
'''


def verify(stock=None, balance=None):
    return HTTP + 'const stockDelta = ' + json.dumps(stock or {}) + ';\nconst balanceDelta = ' + json.dumps(balance or {}) + ''';
const products = pm.response.json();
pm.test("Both products exist", () => pm.expect(products.map(p => p.id).sort()).to.eql(["P1", "P2"]));
products.forEach(p => pm.test("Stock delta " + p.id, () => pm.expect(p.stock).to.eql(Number(pm.collectionVariables.get("stock_" + p.id)) + (stockDelta[p.id] || 0))));
pm.sendRequest(pm.collectionVariables.get("paymentUrl") + "/api/payments/wallets", (err, res) => {
  pm.test("Wallet verification succeeds", () => { pm.expect(err).to.be.null; pm.expect(res.code).to.eql(200); });
  if (!err && res.code === 200) res.json().forEach(w => pm.test("Balance delta " + w.customerId, () => pm.expect(w.balance).to.eql(Number(pm.collectionVariables.get("bal_" + w.customerId)) + (balanceDelta[w.customerId] || 0))));
});
'''


def scenario(kb, title, body, error=None, stock=None, balance=None, manual=False):
    status, saga = ("CONFIRMED", "COMPLETED") if error is None else ("CANCELLED", "COMPENSATED")
    code = HTTP + f'''
const o = pm.response.json();
pm.collectionVariables.set("kb{kb}OrderId", o.id);
pm.test("Order status {status}", () => pm.expect(o.status).to.eql("{status}"));
pm.test("Saga status {saga}", () => pm.expect(o.sagaStatus).to.eql("{saga}"));
'''
    code += 'pm.test("No failure reason", () => pm.expect(o.failureReason).to.be.null);' if error is None else f'pm.test("Failure reason", () => pm.expect(o.failureReason).to.match(/^{error}/));'
    if error is None:
        steps = ["CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "CONFIRM_ORDER"]
    elif kb in (3, 4):
        steps = ["CREATE_ORDER", "SAGA", "CANCEL_ORDER"]
    else:
        steps = ["CREATE_ORDER", "RESERVE_INVENTORY", "SAGA", "COMPENSATION", "CANCEL_ORDER"]
    items = [
        request(f"[KB{kb}] 1. Snapshot", "GET", "{{inventoryUrl}}/api/inventory/products", test=SNAPSHOT),
        request(f"[KB{kb}] 2. Place order", "POST", "{{orderUrl}}/api/orders", body, code),
        request(f"[KB{kb}] 3. Verify stock and wallets", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify(stock, balance)),
        request(f"[KB{kb}] 4. Saga logs", "GET", "{{orderUrl}}/api/orders/{{kb" + str(kb) + "OrderId}}/saga-logs", test=HTTP + 'const logs = pm.response.json();\npm.test("Exact saga step order", () => pm.expect(logs.map(l => l.step)).to.eql(' + json.dumps(steps) + '));\npm.test("ISO timestamps", () => logs.forEach(l => pm.expect(l.time).to.match(/^\\d{4}-\\d{2}-\\d{2}T/)));')
    ]
    if kb in (2, 5, 6):
        items.append(request(f"[KB{kb}] 5. Reservation released", "GET", "{{inventoryUrl}}/api/inventory/reservations", test=HTTP + f'const r = pm.response.json().find(r => r.orderId === Number(pm.collectionVariables.get("kb{kb}OrderId")));\npm.test("Reservation is RELEASED", () => {{ pm.expect(r).to.exist; pm.expect(r.status).to.eql("RELEASED"); }});'))
    return {"name": f"KB{kb} - {title}", "item": items}


folders = [{"name": "0 - Service checks", "item": [
    request("Products", "GET", "{{inventoryUrl}}/api/inventory/products", test=HTTP + 'pm.test("Two products", () => pm.expect(pm.response.json()).to.have.length(2));'),
    request("Wallets", "GET", "{{paymentUrl}}/api/payments/wallets", test=HTTP + 'pm.test("Two wallets", () => pm.expect(pm.response.json()).to.have.length(2));'),
    request("Orders", "GET", "{{orderUrl}}/api/orders", test=HTTP + 'pm.test("Orders array", () => pm.expect(pm.response.json()).to.be.an("array"));')]}]
for kb, title, customer, product, qty, amount, error in [
    (1, "Happy path", "C1", "P1", 2, 2000, None),
    (2, "Payment rejected (402)", "C2", "P1", 1, 1000, "402"),
    (3, "Insufficient stock (409)", "C1", "P2", 100, 2000, "409"),
    (4, "Unknown product (404)", "C1", "P9", 1, 10, "404"),
    (5, "Unknown wallet (404)", "C9", "P2", 1, 20, "404")]:
    folders.append(scenario(kb, title, dict(customerId=customer, productId=product, quantity=qty, amount=amount), error,
                            {"P1": -2} if kb == 1 else {}, {"C1": -2000} if kb == 1 else {}))
idem = [request("[KB7] Snapshot", "GET", "{{inventoryUrl}}/api/inventory/products", test=SNAPSHOT)]
for n in (1, 2):
    idem.append(request(f"[KB7] Repeat release {n}", "POST", "{{inventoryUrl}}/api/inventory/release/{{kb2OrderId}}", test=HTTP + 'pm.test("RELEASED response", () => pm.expect(pm.response.text()).to.eql("RELEASED"));'))
idem += [
    request("[KB7] Repeat reserve", "POST", "{{inventoryUrl}}/api/inventory/reserve", {"orderId": "{{kb1OrderId}}", "productId": "P1", "quantity": 2}, HTTP + 'pm.test("Still RESERVED", () => pm.expect(pm.response.json().status).to.eql("RESERVED"));'),
    request("[KB7] Repeat pay", "POST", "{{paymentUrl}}/api/payments/pay", {"orderId": "{{kb1OrderId}}", "customerId": "C1", "amount": 2000}, HTTP + 'pm.test("Still PAID", () => pm.expect(pm.response.json().status).to.eql("PAID"));'),
    request("[KB7] Refund unpaid order", "POST", "{{paymentUrl}}/api/payments/refund/{{kb2OrderId}}", test=HTTP),
    request("[KB7] Verify repeated calls", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify()),
    request("[KB7] Pay isolated test payment", "POST", "{{paymentUrl}}/api/payments/pay", {"orderId": "{{isolatedOrderId}}", "customerId": "C1", "amount": 20}, HTTP, 'pm.collectionVariables.set("isolatedOrderId", Date.now());')]
for n in (1, 2):
    idem.append(request(f"[KB7] Refund isolated payment {n}", "POST", "{{paymentUrl}}/api/payments/refund/{{isolatedOrderId}}", test=HTTP + 'pm.test("REFUNDED response", () => pm.expect(pm.response.text()).to.eql("REFUNDED"));'))
idem.append(request("[KB7] Verify refund happens once", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify()))
folders.append({"name": "KB7 - Idempotency", "item": idem})
manual = scenario(6, "Payment service stopped", dict(customerId="C1", productId="P2", quantity=1, amount=20), "Service không phản hồi")
manual["description"] = "Snapshot; stop payment-service; place order; start payment-service and wait until ready; verify; inspect saga logs."
kb8 = {"name": "KB8 - Compensation also unavailable", "description": "Set PAYMENT_DELAY_MS=15000, recreate order-service, send place order, then stop payment-service and inventory-service after RESERVE_INVENTORY. Restart both before verifying. See scripts/verify_failures.py for deterministic coordination.", "item": [
    request("[KB8] 1. Snapshot", "GET", "{{inventoryUrl}}/api/inventory/products", test=SNAPSHOT),
    request("[KB8] 2. Place order during outage", "POST", "{{orderUrl}}/api/orders", dict(customerId="C1", productId="P2", quantity=1, amount=20), HTTP + 'const o = pm.response.json(); pm.collectionVariables.set("kb8OrderId", o.id); pm.test("Pending compensation", () => { pm.expect(o.status).to.eql("PENDING"); pm.expect(o.sagaStatus).to.eql("COMPENSATING"); });'),
    request("[KB8] 3. Verify after services restart", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify({"P2": -1})),
    request("[KB8] 4. Three failed retries", "GET", "{{orderUrl}}/api/orders/{{kb8OrderId}}/saga-logs", test=HTTP + 'const logs = pm.response.json(); pm.test("Exactly three compensation failures", () => pm.expect(logs.filter(l => l.step === "COMPENSATION" && l.result === "FAILED")).to.have.length(3)); pm.test("No false cancellation", () => pm.expect(logs.some(l => l.step === "CANCEL_ORDER")).to.be.false);')
]}
folders.append({"name": "Manual - exclude from normal Runner", "item": [manual, kb8]})
collection = {"info": {"name": "MSS301 - Saga Demo (Spring Boot 4.1.1)", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json", "description": "TODO-11: Run folders 0, KB1-5, KB7 in order. KB6 needs Docker service control. Monetary/stock assertions compare snapshot deltas. KB1 consumes 2000 from C1; ensure sufficient seed funds before repeating."},
              "variable": [{"key": k, "value": v} for k, v in [("orderUrl", "http://localhost:8081"), ("inventoryUrl", "http://localhost:8082"), ("paymentUrl", "http://localhost:8083")]], "item": folders}
(ROOT / "Saga_Postman_Collection.json").write_text(json.dumps(collection, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("Generated Saga_Postman_Collection.json")
