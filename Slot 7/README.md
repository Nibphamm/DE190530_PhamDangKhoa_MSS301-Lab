# MSS301 — Slot 7, Part 3–4

Bài triển khai OpenFeign, WireMock, API Gateway và Keycloak/OAuth2.
Các service được copy từ Slot 4/5 vào Slot 7. Order, Inventory và Gateway
dùng Spring Boot **4.1.1**, Java **21**; Spring Cloud **2025.1.3**.
Product giữ Spring Boot **3.5.14** của bài nền, chạy Java **21**.

## Thành phần

| Thành phần | Địa chỉ | Ghi chú |
|---|---|---|
| Product | `http://localhost:8080/api/products` | MongoDB |
| Order | `http://localhost:8081/api/order` | MySQL, gọi Inventory bằng OpenFeign |
| Inventory | `http://localhost:8082/api/inventory` | Seed mỗi SKU 100 sản phẩm |
| Gateway | `http://localhost:9000` | Ba route `/api/products/**`, `/api/order/**`, `/api/inventory/**` |
| Keycloak | `http://localhost:8181` | Realm `spring-microservices-realm` |
| MySQL nghiệp vụ | `localhost:3307` | `order_service`, `inventory_service` |
| MongoDB | `localhost:27017` | `product-service` |

MySQL dùng port **3307** theo lựa chọn của người làm bài để tránh trùng
container dự án khác tại 3306. MySQL của Keycloak chỉ nằm trong mạng Docker.
Mongo Express của bài nền không khởi động vì port 8081 dành cho Order.
Database lưu bằng named volume; khởi động lại container giữ nguyên dữ liệu.

## Khởi động

Yêu cầu JDK 21, Maven 3.9+, Docker Desktop đang chạy. Chạy tại thư mục Slot 7:

```powershell
docker compose -f order-service/docker-compose.yml up -d
docker compose -f product-service/docker-compose.yml up -d
docker compose -f api-gateway/docker-compose.yml up -d
```

Mở bốn terminal riêng, mỗi terminal chạy một service:

```powershell
cd inventory-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd product-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd order-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd api-gateway
.\mvnw.cmd spring-boot:run
```

Chờ Keycloak có log `started`, ba service có log Tomcat ở 8080/8081/8082,
Gateway có log Tomcat ở 9000. Không chạy `mvn clean` khi JAR trong `target/`
đang được một tiến trình Java sử dụng trên Windows.

## Keycloak và gọi API

Admin Console: `http://localhost:8181`, tài khoản lab **admin/admin**.
Realm/client được import tự động từ
`api-gateway/docker/keycloak/realms/spring-microservices-realm.json`:

- Realm: `spring-microservices-realm`.
- Client: `spring-microservices-client`.
- Client authentication ON, chỉ bật Service accounts.
- Client secret dành cho lab: `mss301-dev-secret-change-me`.
- Access token lifespan: 300 giây.

Import chỉ thực hiện khi realm chưa tồn tại. Sửa file JSON sau khi đã import
không tự cập nhật realm hiện tại; chỉnh trong Admin Console khi cần.
Thông tin đăng nhập trên là cấu hình development của đề bài.

Lấy token và tạo đơn bằng PowerShell:

```powershell
$realmUrl = 'http://localhost:8181/realms/spring-microservices-realm'
$response = Invoke-RestMethod -Method Post `
    -Uri "$realmUrl/protocol/openid-connect/token" `
    -Body @{
        grant_type = 'client_credentials'
        client_id = 'spring-microservices-client'
        client_secret = 'mss301-dev-secret-change-me'
    }
$headers = @{ Authorization = "Bearer $($response.access_token)" }
Invoke-RestMethod -Uri 'http://localhost:9000/api/products' -Headers $headers
Invoke-WebRequest -UseBasicParsing -Method Post `
    -Uri 'http://localhost:9000/api/order' -Headers $headers `
    -ContentType 'application/json' `
    -Body '{"skuCode":"iphone_15","price":1000,"quantity":1}'
```

Gateway kiểm tra chữ ký, thời hạn và issuer của JWT. Không có token hoặc
token không hợp lệ trả 401; `/actuator/health` công khai trả 200.
Token hợp lệ cho phép chuyển tiếp request. Order chỉ lưu đơn khi Inventory
trả `true`; quantity 100 hợp lệ, 101 hết hàng trả 500 theo đề.
Luồng này kiểm tra tồn kho, không trừ số lượng Inventory.

Trong Postman, import `postman/MSS301-Part3-4.postman_collection.json`, chạy
`Get access token` trước rồi dùng Collection Runner. Request này gửi client
credentials bằng Basic Auth, tự lưu `accessToken`; request Gateway kế thừa
Bearer token. Có thể dùng Authorization → OAuth 2.0 → Client Credentials
với token URL trên, Client ID/Secret trên và `Send as Basic Auth header`.

## Kiểm thử

Chạy trong từng thư mục service:

```powershell
.\mvnw.cmd test
```

| Bộ test | Số test | Phụ thuộc |
|---|---:|---|
| Order | 2 | Docker: MySQL Testcontainers; WireMock thay Inventory |
| Gateway | 5 | MockMvc, JWT giả lập, WireMock; không cần Docker/Keycloak |
| Inventory | 1 | Docker: MySQL Testcontainers |
| Product | 12 | Docker: MongoDB Testcontainers |

Order test xác nhận request Feign đúng query, đơn thành công được lưu,
đơn hết hàng không được lưu. Gateway test xác nhận public health, chặn
request không token, forward GET/POST và giữ body/query tới WireMock.

Khi toàn bộ hệ thống đang chạy, từ Slot 7:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify.ps1
```

Script kiểm tra 21 case với JWT thật: discovery, token, sai secret,
health, không token, token rác, sửa payload, issuer sai, Inventory boundary,
đặt đơn trực tiếp và qua Gateway, routing, path lạ và số bản ghi MySQL.
Script tạo sản phẩm/đơn phục vụ test trong database lab.
Kết quả được ghi vào `verification-results.json` (Git ignore).

Để kiểm tra cả token hết hạn:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify.ps1 -TestTokenExpiry
```

Có thêm case thứ 22; chờ token 300 giây cộng 65 giây để vượt clock skew
mặc định của decoder. Không cần đổi lifespan trên Keycloak.

Chạy Postman Collection bằng CLI:

```powershell
npx --yes newman run postman/MSS301-Part3-4.postman_collection.json
```

Các báo cáo Maven nằm tại `<service>/target/surefire-reports/`.
Kết quả nghiệm thu và commit theo phần được ghi trong `TEST_RESULTS.md`.

## Dừng

Dừng service bằng Ctrl+C ở các terminal. Dừng container từ Slot 7:

```powershell
docker compose -f api-gateway/docker-compose.yml stop
docker compose -f product-service/docker-compose.yml stop
docker compose -f order-service/docker-compose.yml stop
```

Các lệnh `stop` giữ nguyên database để dùng lại lần sau.
