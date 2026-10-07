# MSS301 — Slot 9 — Part 5

Hoàn thành DOC-1 đến DOC-16: Swagger UI và OpenAPI cho Product, Inventory,
Order; tổng hợp tài liệu tại API Gateway; cho phép xem Swagger không cần JWT.

Bốn project nằm trực tiếp dưới `Slot 9`. Mã nguồn được sao chép từ tài nguyên
`Slot 7` được cung cấp trong thư mục này. Các thư mục tài nguyên gốc được giữ nguyên.

| Service | Spring Boot | Springdoc | Cổng | API nghiệp vụ |
| --- | --- | --- | --- | --- |
| Product | 3.5.14 | 2.8.17 | 8080 | `/api/products` |
| Inventory | 4.1.1 | 3.1.1 | 8082 | `/api/inventory` |
| Order | 4.1.1 | 3.1.1 | 8081 | `/api/order` |
| Gateway | 4.1.1 | 3.1.1 | 9000 | chuyển tiếp ba service |

Theo xác nhận của người dùng, dùng Springdoc tương thích thay cho 2.5.0 trong
hướng dẫn, giữ package Product `com.fudn.product_service` và API `/api/products`.
Tham khảo tương thích: <https://springdoc.org/>.

## Khởi động

Cần JDK 21, Docker Desktop đang chạy và các cổng 27017, 3307, 8181,
8080, 8081, 8082, 9000 còn trống.

Từ `Slot 9`:

```powershell
docker compose up -d
docker compose ps
```

Mở bốn terminal riêng, chạy lần lượt:

```powershell
# Terminal 1
cd inventory-service
.\mvnw.cmd spring-boot:run

# Terminal 2 (bắt đầu từ Slot 9)
cd product-service
.\mvnw.cmd spring-boot:run

# Terminal 3 (bắt đầu từ Slot 9)
cd order-service
.\mvnw.cmd spring-boot:run

# Terminal 4 (bắt đầu từ Slot 9)
cd api-gateway
.\mvnw.cmd spring-boot:run
```

Keycloak có thể mất thời gian khởi tạo database và import realm lần đầu.
Chờ endpoint sau trả JSON trước khi kiểm tra JWT:
`http://localhost:8181/realms/spring-microservices-realm/.well-known/openid-configuration`.

## Xem tài liệu API

- Gateway: <http://localhost:9000/swagger-ui.html> có ba lựa chọn service.
- Product: <http://localhost:8080/swagger-ui.html>.
- Order: <http://localhost:8081/swagger-ui.html>.
- Inventory: <http://localhost:8082/swagger-ui.html>.
- JSON của từng service: `/api-docs`.
- Gateway chuyển `/aggregate/{service}-service/v3/api-docs` tới `/api-docs`
  của service tương ứng. Các route dùng `services.*.url` đã có trong cấu hình.

Swagger và các route docs là public. Các API nghiệp vụ qua Gateway cần
`Authorization: Bearer <JWT>`. Cấu hình CSRF disabled, session stateless và
health endpoint public từ Part 3–4 được giữ lại.

Swagger UI trực tiếp tại các service có thể dùng “Try it out” với API nội bộ.
Để kiểm tra API qua Gateway bằng JWT thật, chạy script bên dưới; script lấy token
bằng client credentials từ realm phát triển đã được cung cấp.

## Kiểm thử

Chạy toàn bộ test, gồm test Part 1–4 và test Swagger mới:

```powershell
$services = 'product-service', 'inventory-service', 'order-service', 'api-gateway'
foreach ($service in $services) {
    Push-Location $service
    try {
        .\mvnw.cmd test
        if ($LASTEXITCODE -ne 0) { throw "Tests failed: $service" }
    } finally {
        Pop-Location
    }
}
```

Chỉ chạy Swagger test từ từng thư mục service:

```powershell
# Product / Inventory / Order
.\mvnw.cmd test '-Dtest=SwaggerIntegrationTest'
# Gateway
.\mvnw.cmd test '-Dtest=SwaggerSecurityTest'
```

Sau khi cả hệ thống đã chạy, dùng Python 3 kiểm tra HTTP thực tế từ `Slot 9`:

```powershell
python scripts/verify_part5.py
```

Script kiểm tra UI, metadata, endpoint trong docs trực tiếp/tổng hợp, dropdown,
CORS, API không có JWT, JWT thật từ Keycloak và token không hợp lệ.
Kết quả xác minh được ghi trong [TEST_RESULTS.md](TEST_RESULTS.md).

Để dừng, nhấn Ctrl+C ở các terminal service, rồi chạy `docker compose stop`.

## Git

DOC-1 đến DOC-16 mỗi TODO có một commit riêng. DOC-15a/b/c nằm trong commit
DOC-15; DOC-16a/b/c nằm trong commit DOC-16, theo danh sách 16 TODO của đề.
Commit dùng Conventional Commits theo `GIT_CONVENTIONS.docx`, không có
`Co-Authored-By`. Các commit nền, kiểm thử và hướng dẫn được tách riêng.
