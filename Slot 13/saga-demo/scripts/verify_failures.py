"""Exercise real container outages for KB6 and KB8; retain JSON evidence."""
import concurrent.futures
import json
import os
import subprocess
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def compose(*args, delay="0"):
    env = dict(os.environ, PAYMENT_DELAY_MS=delay)
    subprocess.run(["docker", "compose", *args], cwd=ROOT, env=env, check=True,
                   stdout=subprocess.DEVNULL)


def api(port, path, body=None):
    req = urllib.request.Request(f"http://localhost:{port}/api/{path}",
                                 data=json.dumps(body).encode() if body is not None else None,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=90) as response:
        data = response.read().decode()
        try:
            return json.loads(data)
        except json.JSONDecodeError:
            return data


def ready(port, path):
    for _ in range(90):
        try:
            return api(port, path)
        except Exception:
            time.sleep(1)
    raise RuntimeError(f"Service {port} not ready")


def snapshot():
    return {"stock": {p["id"]: p["stock"] for p in api(8082, "inventory/products")},
            "wallets": {w["customerId"]: w["balance"] for w in api(8083, "payments/wallets")}}


def logs(order):
    return api(8081, f"orders/{order['id']}/saga-logs")


def main():
    evidence = {}
    payload = dict(customerId="C1", productId="P2", quantity=1, amount=20)
    before = snapshot()
    try:
        compose("stop", "payment-service")
        order = api(8081, "orders", payload)
        assert (order["status"], order["sagaStatus"]) == ("CANCELLED", "COMPENSATED"), order
        assert order["failureReason"].startswith("Service không phản hồi"), order
        saga_logs = logs(order)
        assert [l["step"] for l in saga_logs] == ["CREATE_ORDER", "RESERVE_INVENTORY", "SAGA", "COMPENSATION", "CANCEL_ORDER"]
    finally:
        compose("start", "payment-service")
        ready(8083, "payments/wallets")
    assert snapshot() == before
    evidence["KB6"] = dict(order=order, logs=saga_logs, before=before, after=snapshot(), passed=True)

    before = snapshot()
    try:
        compose("up", "-d", "--no-deps", "order-service", delay="15000")
        ready(8081, "orders")
        previous = {o["id"] for o in api(8081, "orders")}
        with concurrent.futures.ThreadPoolExecutor() as pool:
            pending = pool.submit(api, 8081, "orders", payload)
            found = False
            for _ in range(100):
                for candidate in api(8081, "orders"):
                    if candidate["id"] not in previous and any(l["step"] == "RESERVE_INVENTORY" for l in logs(candidate)):
                        found = True
                        break
                if found:
                    break
                time.sleep(.1)
            assert found, "Did not observe successful reserve before outage"
            compose("stop", "payment-service", "inventory-service")
            order = pending.result(timeout=90)
        saga_logs = logs(order)
        assert (order["status"], order["sagaStatus"]) == ("PENDING", "COMPENSATING"), order
        assert len([l for l in saga_logs if l["step"] == "COMPENSATION" and l["result"] == "FAILED"]) == 3, saga_logs
        assert not any(l["step"] == "CANCEL_ORDER" for l in saga_logs)
    finally:
        compose("start", "inventory-service", "payment-service")
        ready(8082, "inventory/products")
        ready(8083, "payments/wallets")
        compose("up", "-d", "--no-deps", "order-service")
        ready(8081, "orders")
    after = snapshot()
    assert after["stock"]["P2"] == before["stock"]["P2"] - 1
    assert after["wallets"] == before["wallets"]
    evidence["KB8"] = dict(order=order, logs=saga_logs, before=before, after=after, passed=True)
    (ROOT / "evidence/failure-scenarios.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    print("KB6 and KB8 passed; KB8 remains COMPENSATING for EXT-4 recovery test")


if __name__ == "__main__":
    main()
