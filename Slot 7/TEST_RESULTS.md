# Nghiệm thu Part 3–4

Ngày kiểm thử: **30/09/2026**, Windows, JDK 21.0.11, Docker Desktop.
Thực hiện trên bản copy service trong Slot 7.

## Kết quả tự động

| Bộ kiểm thử | Kết quả |
|---|---|
| Order Service: WireMock + MySQL Testcontainers | **2/2 PASS** |
| API Gateway: MockMvc + JWT + WireMock | **5/5 PASS** |
| Inventory Service: MySQL Testcontainers | **1/1 PASS** |
| Product Service: MongoDB Testcontainers | **12/12 PASS** |
| `scripts/verify.ps1 -TestTokenExpiry`: service thật, Keycloak thật | **22/22 PASS** |
| Postman Collection chạy bằng Newman | **15 request, 16 assertion, 0 lỗi** |
| Docker Compose config: ba file | **PASS** |
| `git diff --check` | **PASS** |

Không có test Maven bị skip. Báo cáo chi tiết nằm trong
`<service>/target/surefire-reports/`.
Script end-to-end ghi `verification-results.json`; Newman được chạy với
JSON reporter tại `.runtime/postman-results.json`. Hai file kết quả runtime
không đưa vào Git, có thể tái tạo bằng các lệnh trong README.

## Kiểm tra chức năng

- OpenFeign gọi Inventory với đúng `skuCode`, `quantity` trước khi lưu đơn.
- Quantity 100: Inventory `true`, Order `201`, có bản ghi mới trong MySQL.
- Quantity 101 và SKU không tồn tại: Order `500`, số bản ghi không tăng.
- Khi Inventory tạm dừng: Order `500`, không lưu đơn; đã khởi động lại.
- Gateway chuyển tiếp Product/Order/Inventory; query và body được giữ.
- Path không có route trả `404` khi request có JWT hợp lệ.
- PUT `/api/products/{id}` qua Gateway trả `200`; DELETE sản phẩm test trả `204`.
- Khi Product tạm dừng: Gateway trả `500`; khởi động lại trả `200`.
- Keycloak 24.0.1 khởi động và import realm/client thành công.
- Xác thực tài khoản admin/admin thành công qua token endpoint của master realm.
- Admin API xác nhận client confidential, Service accounts ON,
  Standard/Implicit/Direct access grants OFF; secret đúng cấu hình lab.
- MySQL Keycloak chứa cả realm `master` và `spring-microservices-realm`.
- Discovery trả issuer, token endpoint và JWKS URI đúng.
- Client Credentials bằng form và bằng Basic Auth đều cấp token, lifespan 300 giây.
- Sai client secret trả `401`.
- Không token, token rác, payload bị sửa, issuer `127.0.0.1` thay `localhost`
  và token đã hết hạn đều trả `401`.
- Test token hết hạn chờ lifespan thật cộng clock skew; không sửa lifespan realm.
- POST không token không tạo bản ghi Order.
- Public health trả `200`, trạng thái `UP`.
- JWT hợp lệ: GET Product/Inventory `200`, POST Order `201` và có bản ghi mới.
- JWT hợp lệ nhưng hết hàng: `500`, không tạo bản ghi.

Các thử nghiệm sửa code để bỏ annotation/stub/quy tắc bảo mật trong đề là
bài tập âm tùy chọn, không áp dụng vào source cuối cùng. Các test hiện tại
xác minh hành vi cần thiết bằng HTTP, WireMock và kiểm tra database.
Không thu thập screenshot thao tác Admin Console/Postman; các kiểm tra
cấu hình và collection được thực hiện qua API và Newman.

## Phiên bản và cấu hình đã thống nhất

- Order/Inventory/Gateway: Spring Boot **4.1.1**, Java **21**.
- Spring Cloud: **2025.1.3**; WireMock integration **4.2.3**.
- REST Assured của Order/Inventory: **6.0.1**.
- Product giữ Boot **3.5.14** của source nền, Java target đổi về **21**.
- MySQL nghiệp vụ: **3307**, không dùng container dự án khác ở 3306.
- MongoDB: **27017**; Product/Order/Inventory/Gateway: **8080/8081/8082/9000**.
- Keycloak: **8181**; cấu hình development theo đề.

## Commit theo từng phần

| Commit | Nội dung |
|---|---|
| `b1fb7b9` | Copy service nền vào Slot 7 |
| `0a4c9cf` | OpenFeign và kiểm tra tồn kho trước khi lưu |
| `3f71f79` | WireMock và test Order còn hàng/hết hàng |
| `e4a7678` | MySQL 3307, Java 21, sửa config nền và Testcontainers |
| `8c72361` | API Gateway và ba route |
| `3471cfb` | Docker Compose Keycloak, realm và client |
| `a347566` | JWT resource server và SecurityFilterChain |
| `6f488c4` | Test bảo mật/routing Gateway |
| `a246721` | Maven wrapper cho Gateway |
| `3df8515` | Script end-to-end và Postman Collection |

Commit tuân thủ Conventional Commits. Thay đổi yêu cầu JWT có dấu `!`
và footer `BREAKING CHANGE`. Không có footer `Co-Authored-By`.
Các file đề bài và thay đổi có sẵn ở thư mục khác không đưa vào commit.
