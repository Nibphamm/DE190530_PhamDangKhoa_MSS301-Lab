"""Check the running Part 5 services; requires Python 3 and Docker infrastructure."""

import json
from pathlib import Path
import urllib.error
import urllib.parse
import urllib.request


def request(url, method="GET", data=None, headers=None):
    req = urllib.request.Request(url, data=data, headers=headers or {}, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return response.status, response.read(), dict(response.headers)
    except urllib.error.HTTPError as error:
        return error.code, error.read(), dict(error.headers)


def check(condition, message):
    if not condition:
        raise AssertionError(message)
    print("PASS:", message)


def main():
    services = [
        ("product", 8080, "Product", "/api/products", ["get", "post"]),
        ("order", 8081, "Order", "/api/order", ["post"]),
        ("inventory", 8082, "Inventory", "/api/inventory", ["get"]),
    ]
    for service, port, title, endpoint, methods in services:
        status, body, _ = request(f"http://localhost:{port}/swagger-ui.html")
        check(status == 200 and b"Swagger UI" in body, f"{title} Swagger UI")
        for url in [
            f"http://localhost:{port}/api-docs",
            f"http://localhost:9000/aggregate/{service}-service/v3/api-docs",
        ]:
            status, body, _ = request(url)
            check(status == 200, f"public docs: {url}")
            spec = json.loads(body)
            check(spec["info"]["title"] == f"{title} Service API", f"{title} docs title")
            check(spec["info"]["version"] == "v0.0.1", f"{title} docs version")
            check(all(method in spec["paths"][endpoint] for method in methods),
                  f"{title} documented operations")

    status, body, _ = request("http://localhost:9000/swagger-ui.html")
    check(status == 200 and b"Swagger UI" in body, "public gateway Swagger UI")
    status, body, _ = request("http://localhost:9000/v3/api-docs/swagger-config")
    check(status == 200, "public gateway Swagger configuration")
    urls = json.loads(body)["urls"]
    check({item["name"] for item in urls} == {
        "Product Service", "Order Service", "Inventory Service"
    }, "gateway dropdown lists three services")
    for path in ["/api/product", "/api/products", "/api/order", "/api/inventory"]:
        status, _, _ = request("http://localhost:9000" + path)
        check(status == 401, f"JWT required: {path}")

    status, _, headers = request("http://localhost:9000/api/products", "OPTIONS", headers={
        "Origin": "http://localhost:8080",
        "Access-Control-Request-Method": "POST",
        "Access-Control-Request-Headers": "Authorization, Content-Type",
    })
    check(status == 200 and headers.get("Access-Control-Allow-Origin") == "*",
          "gateway CORS preflight")

    root = Path(__file__).resolve().parent.parent
    realm = json.loads((root / "api-gateway/docker/keycloak/realms/"
                        "spring-microservices-realm.json").read_text(encoding="utf-8"))
    client = realm["clients"][0]
    status, body, _ = request(
        "http://localhost:8181/realms/spring-microservices-realm/protocol/openid-connect/token",
        "POST", urllib.parse.urlencode({
            "grant_type": "client_credentials",
            "client_id": client["clientId"],
            "client_secret": client["secret"],
        }).encode(), {"Content-Type": "application/x-www-form-urlencoded"})
    check(status == 200, "Keycloak issues a JWT")
    authorization = {"Authorization": "Bearer " + json.loads(body)["access_token"]}
    status, _, _ = request("http://localhost:9000/api/products", headers=authorization)
    check(status == 200, "real JWT permits product API through gateway")
    status, body, _ = request(
        "http://localhost:9000/api/inventory?skuCode=iphone_15&quantity=1",
        headers=authorization)
    check(status == 200 and json.loads(body) is True,
          "real JWT permits inventory API through gateway")
    status, _, _ = request("http://localhost:9000/api/products", headers={
        "Authorization": "Bearer invalid-token"
    })
    check(status == 401, "invalid JWT is rejected")
    print("All Part 5 end-to-end checks passed.")


if __name__ == "__main__":
    main()
