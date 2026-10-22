"""Postman folders correspond to separate Compose profiles, never run all at once."""
import json
from pathlib import Path
from create_collection import request, HTTP, SNAPSHOT, verify, ROOT


def snapshot(name="Snapshot"):
    return request(name, "GET", "{{inventoryUrl}}/api/inventory/products", test=SNAPSHOT)


def order(name, customer="C1", product="P2", qty=1, amount=20, status="CONFIRMED", saga="COMPLETED", asynchronous=False):
    code = HTTP + 'const o = pm.response.json(); pm.collectionVariables.set("extensionOrderId", o.id);\n'
    if not asynchronous:
        code += f'pm.test("Order {status}", () => pm.expect(o.status).to.eql("{status}"));\npm.test("Saga {saga}", () => pm.expect(o.sagaStatus).to.eql("{saga}"));'
    return request(name, "POST", "{{orderUrl}}/api/orders", dict(customerId=customer, productId=product, quantity=qty, amount=amount), code)


def poll(status="CONFIRMED", saga="COMPLETED"):
    code = HTTP + f'''
const id = Number(pm.collectionVariables.get("extensionOrderId"));
function check(remaining) {{
  pm.sendRequest(pm.collectionVariables.get("orderUrl") + "/api/orders", (err, response) => {{
    if (err) {{ pm.test("Polling transport succeeds", () => pm.expect(err).to.be.null); return; }}
    const order = response.json().find(o => o.id === id);
    if (order && ["COMPLETED", "COMPENSATED"].includes(order.sagaStatus)) {{
      pm.test("Async order {status}", () => pm.expect(order.status).to.eql("{status}"));
      pm.test("Async saga {saga}", () => pm.expect(order.sagaStatus).to.eql("{saga}"));
    }} else if (remaining > 0) {{ setTimeout(() => check(remaining - 1), 250); }}
    else {{ pm.test("Saga reaches a terminal state", () => pm.expect.fail(JSON.stringify(order))); }}
  }});
}}
check(100);
'''
    return request("Wait for async completion", "GET", "{{orderUrl}}/api/orders", test=code)


def logs(steps=None, compensation=None):
    code = HTTP + 'const logs = pm.response.json();\n'
    if steps:
        code += 'pm.test("Exact execution sequence", () => pm.expect(logs.map(l => l.step)).to.eql(' + json.dumps(steps) + '));\n'
    if compensation:
        code += 'pm.test("Compensation LIFO", () => pm.expect(logs.filter(l => l.step === "COMPENSATION" && l.result === "SUCCESS").map(l => l.message.split(" - ")[0])).to.eql(' + json.dumps(compensation) + '));'
    return request("Inspect saga logs", "GET", "{{orderUrl}}/api/orders/{{extensionOrderId}}/saga-logs", test=code)


folders = []
folders.append({"name": "EXT-4 - Recover KB8", "description": "Start recovery profile; set recoveryOrderId to the KB8 order before running.", "item": [
    snapshot(),
    request("Recover KB8", "POST", "{{orderUrl}}/api/orders/{{recoveryOrderId}}/compensate", test=HTTP + 'const o = pm.response.json(); pm.test("Recovered CANCELLED/COMPENSATED", () => { pm.expect(o.status).to.eql("CANCELLED"); pm.expect(o.sagaStatus).to.eql("COMPENSATED"); });'),
    request("Repeat recovery safely", "POST", "{{orderUrl}}/api/orders/{{recoveryOrderId}}/compensate", test=HTTP + 'pm.test("Already compensated", () => pm.expect(pm.response.json().sagaStatus).to.eql("COMPENSATED"));'),
    request("Stock restored once; no debit", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify({"P2": 1})),
    request("Recovery saga logs", "GET", "{{orderUrl}}/api/orders/{{recoveryOrderId}}/saga-logs", test=HTTP + 'const logs = pm.response.json(); pm.test("Three original failures retained", () => pm.expect(logs.filter(l => l.step === "COMPENSATION" && l.result === "FAILED")).to.have.length(3)); pm.test("Recovery completed", () => pm.expect(logs.at(-1).step).to.eql("CANCEL_ORDER"));')
]})
folders.append({"name": "EXT-1 - Shipping rejection", "description": "Start docker-compose.shipping.yml override.", "item": [
    snapshot(), order("Shipping rejects four items", qty=4, amount=80, status="CANCELLED", saga="COMPENSATED"),
    request("Wallet and stock restored", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify()),
    logs(["CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "SAGA", "COMPENSATION", "COMPENSATION", "CANCEL_ORDER"], ["REFUND_PAYMENT", "RELEASE_INVENTORY"]),
    request("Payment was refunded", "GET", "{{paymentUrl}}/api/payments", test=HTTP + 'const p = pm.response.json().find(p => p.orderId === Number(pm.collectionVariables.get("extensionOrderId"))); pm.test("Payment REFUNDED", () => pm.expect(p.status).to.eql("REFUNDED"));')
]})
folders.append({"name": "EXT-1 - Shipping success", "item": [
    snapshot(), order("Create shipment"),
    request("One item purchased", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify({"P2": -1}, {"C1": -20})),
    logs(["CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "CREATE_SHIPPING", "CONFIRM_ORDER"]),
    request("Inspect shipment", "GET", "{{shippingUrl}}/api/shipping", test=HTTP + 'const s = pm.response.json().find(s => s.orderId === Number(pm.collectionVariables.get("extensionOrderId"))); pm.test("Shipment CREATED", () => pm.expect(s.status).to.eql("CREATED"));'),
    request("Reject recovery of successful purchase", "POST", "{{orderUrl}}/api/orders/{{extensionOrderId}}/compensate", test='pm.test("HTTP 409", () => pm.response.to.have.status(409));')
]})
folders.append({"name": "EXT-1 - Shipping idempotency", "description": "Requires shipping-service on 8084. Uses an isolated shipment id without changing inventory or wallets.", "item": [
    snapshot(),
    request("Create isolated shipment", "POST", "{{shippingUrl}}/api/shipping/create", dict(orderId="{{isolatedShipmentId}}", productId="P2", quantity=1), HTTP + 'pm.test("CREATED", () => pm.expect(pm.response.json().status).to.eql("CREATED"));', 'pm.collectionVariables.set("isolatedShipmentId", Date.now());'),
    request("Repeat create", "POST", "{{shippingUrl}}/api/shipping/create", dict(orderId="{{isolatedShipmentId}}", productId="P2", quantity=1), HTTP + 'pm.test("Still CREATED", () => pm.expect(pm.response.json().status).to.eql("CREATED"));'),
    request("Cancel shipment", "POST", "{{shippingUrl}}/api/shipping/cancel/{{isolatedShipmentId}}", test=HTTP),
    request("Repeat cancellation", "POST", "{{shippingUrl}}/api/shipping/cancel/{{isolatedShipmentId}}", test=HTTP),
    request("One cancelled shipment", "GET", "{{shippingUrl}}/api/shipping", test=HTTP + 'const rows = pm.response.json().filter(s => s.orderId === Number(pm.collectionVariables.get("isolatedShipmentId"))); pm.test("One CANCELLED receipt", () => { pm.expect(rows).to.have.length(1); pm.expect(rows[0].status).to.eql("CANCELLED"); });'),
    request("No other resources changed", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify())
]})
folders.append({"name": "EXT-2 - Timeout after commit", "description": "Start timeout override. Payment delays its HTTP response for 5s after committing, client times out at 2s.", "item": [
    snapshot(), order("Lose payment acknowledgement", status="CANCELLED", saga="COMPENSATED"),
    request("No money or stock lost", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify()),
    logs(compensation=["REFUND_PAYMENT", "RELEASE_INVENTORY"]),
    request("Payment receipt shows refund", "GET", "{{paymentUrl}}/api/payments", test=HTTP + 'const p = pm.response.json().find(p => p.orderId === Number(pm.collectionVariables.get("extensionOrderId"))); pm.test("Committed payment REFUNDED", () => pm.expect(p.status).to.eql("REFUNDED"));')
]})
for mode in ("EXT-5 - Outbox", "EXT-3 - Choreography"):
    folders.append({"name": mode + " success", "item": [snapshot(), order("Submit async order", asynchronous=True), poll(),
        request("Purchase applied once", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify({"P2": -1}, {"C1": -20})), logs()]})
    folders.append({"name": mode + " payment failure", "item": [snapshot(), order("Reject insufficient balance", customer="C2", product="P1", amount=1000, asynchronous=True), poll("CANCELLED", "COMPENSATED"),
        request("Compensation restores all resources", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify()), logs()]})
folders.append({"name": "EXT-5 - Queue before restart", "item": [snapshot(), order("Durably queue order", asynchronous=True),
    request("Inspect durable RESERVE command", "GET", "{{orderUrl}}/api/orders/{{extensionOrderId}}/outbox", test=HTTP + 'const c = pm.response.json(); pm.test("One queued command", () => pm.expect(c).to.have.length(1)); pm.test("RESERVE persists pending", () => { pm.expect(c[0].command).to.eql("RESERVE"); pm.expect(c[0].state).to.eql("PENDING"); });')
]})
folders.append({"name": "EXT-5 - Resume after restart", "item": [poll(),
    request("Purchase completed exactly once", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify({"P2": -1}, {"C1": -20})),
    request("All commands delivered", "GET", "{{orderUrl}}/api/orders/{{extensionOrderId}}/outbox", test=HTTP + 'const c = pm.response.json(); pm.test("RESERVE PAY CONFIRM delivered", () => pm.expect(c.map(c => c.command)).to.eql(["RESERVE", "PAY", "CONFIRM"])); pm.test("All commands DONE", () => c.forEach(c => pm.expect(c.state).to.eql("DONE")));')
]})
folders.append({"name": "EXT-6 - HTTP interfaces", "item": [snapshot(), order("Interface payment rejection", customer="C2", product="P1", amount=1000, status="CANCELLED", saga="COMPENSATED"),
    request("Proxy compensation works", "GET", "{{inventoryUrl}}/api/inventory/products", test=verify()),
    logs(["CREATE_ORDER", "RESERVE_INVENTORY", "SAGA", "COMPENSATION", "CANCEL_ORDER"])]})
collection = {"info": {"name": "MSS301 - Saga Extensions", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json", "description": "Run one folder with its matching Compose override. scripts/run-extensions.cjs switches profiles and executes the complete suite. Do not run the entire collection against one profile."},
              "variable": [{"key": k, "value": v} for k, v in [("orderUrl", "http://localhost:8081"), ("inventoryUrl", "http://localhost:8082"), ("paymentUrl", "http://localhost:8083"), ("shippingUrl", "http://localhost:8084"), ("recoveryOrderId", "")]], "item": folders}
(ROOT / "Saga_Extensions_Postman_Collection.json").write_text(json.dumps(collection, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
