# MSS301 — Order Saga

Bài thực hành tại `Slot 13/saga-demo`, theo `../Saga_guide.md`.
Java 21, Maven, Spring Boot 4.1.1; mỗi service có database riêng.

## Khởi chạy bài gốc

```powershell
Set-Location "D:\Documents\FPT UNI\Fall2026\MSS301\DE190530_PhamDangKhoa_MSS301-Lab\Slot 13\saga-demo"
docker compose up -d --build
docker compose ps
```

Chờ ba database `healthy` và các API trả HTTP 200:

| Service | API kiểm tra | Database / cổng host |
|---|---|---|
| Order | `http://localhost:8081/api/orders` | MySQL 8.4, `localhost:3308/orderdb` |
| Inventory | `http://localhost:8082/api/inventory/products` | MongoDB 8.0 replica set `rs0`, `localhost:27017` |
| Payment | `http://localhost:8083/api/payments/wallets` | SQL Server 2022, `localhost,14330`, `paymentdb` |
| Shipping, khi bật | `http://localhost:8084/api/shipping` | PostgreSQL 17, `localhost:5433/shippingdb` |

MySQL host dùng **3308 theo lựa chọn của người dùng**, vì 3307 đã được
`pacepal-mysql` sử dụng. Container Order vẫn kết nối `mysql:3306`.
Thông tin đăng nhập database là dữ liệu demo trong Compose.

Chạy từ IDE: khởi động ba database trước, rồi Inventory → Payment → Order.
Các giá trị mặc định trong `application.properties` dùng cổng host ở bảng trên.

## Postman — TODO-11

Import hai collection và environment:

- `Saga_Postman_Collection.json`: KB1–KB8.
- `Saga_Extensions_Postman_Collection.json`: EXT-1–EXT-6, từng folder có profile riêng.
- `Saga_Local.postman_environment.json`: các URL localhost.

Bài gốc: bấm **Run collection**, bỏ chọn **Manual - exclude from normal Runner**,
giữ thứ tự `0 → KB1 → KB2 → KB3 → KB4 → KB5 → KB7`, chạy một iteration.
Tests so sánh chênh lệch kho/ví với snapshot và kiểm tra thứ tự saga log.
KB7 còn kiểm tra hoàn tiền hai lần trên một payment riêng.

Chạy cùng collection bằng Newman:

```powershell
npm ci
npm test
```

Một lần KB1 tiêu thụ 2 P1 và 2000 từ C1. Snapshot không làm dữ liệu vô hạn:
chỉ chạy lại khi C1 và kho còn đủ. Với bộ mở rộng có shipping bốn P2,
cần ít nhất bốn P2 trước kịch bản đó.

Kết quả thực tế nằm trong `evidence/`: JSON đầy đủ request/response/assertion,
JUnit XML, kết quả outage và bảng tổng hợp `verification-summary.json`.
Ảnh Postman Desktop Runner và ảnh saga log là hạng mục riêng; trạng thái
thực tế được ghi tại `evidence/VERIFICATION.md`.

## KB6, KB8: tắt container thật

Chạy khi ba service đang dùng profile bài gốc:

```powershell
npm run test:failures
```

Script KB6 snapshot → tắt Payment → đặt hàng → bật Payment → kiểm tra tài nguyên.
KB8 bật delay trước payment 15 giây, đợi log `RESERVE_INVENTORY`, rồi tắt
Payment và Inventory. Sau ba lần release thất bại, order phải là
`PENDING / COMPENSATING`, không có `CANCEL_ORDER`.
Script bật lại các service và bỏ delay, giữ order KB8 để kiểm tra EXT-4.

Trong Postman có các request tương ứng dưới thư mục Manual. Để làm bằng tay:

```powershell
docker compose stop payment-service
# Gửi request đặt hàng KB6 trong Postman.
docker compose start payment-service
```

KB8 cần `$env:PAYMENT_DELAY_MS='15000'`, `docker compose up -d order-service`;
gửi request và tắt hai service sau khi inventory đã reserve. Sau cùng bật lại
service, bỏ biến môi trường và recreate Order như script đã thực hiện.

## Các biến thể mở rộng

Chỉ chọn **một** override trong bảng cho mỗi lượt chạy. `outbox` và
`choreography` là hai cách đặt hàng thay thế, không bật cùng lúc.
Các profile không cần bật trong bài gốc.

| EXT | Override Compose | Spring profile | Kiểm tra chính |
|---|---|---|---|
| EXT-1 | `docker-compose.shipping.yml` | `shipping,recovery` | Shipping giới hạn 3 sản phẩm; lỗi phải refund → release → cancel |
| EXT-2 | `docker-compose.timeout.yml` | Order: `timeout,recovery`; Payment: `timeout` | Payment commit rồi chờ 5s; client timeout sau 2s và hoàn tiền |
| EXT-3 | `docker-compose.choreography.yml` | `choreography` ở cả ba service | Chuỗi event Kafka và release khi payment thất bại |
| EXT-4 | `docker-compose.recovery.yml` | `recovery` | `POST /api/orders/{id}/compensate` khôi phục KB8, gọi lại không thay đổi tài nguyên |
| EXT-5 | `docker-compose.outbox.yml` | `outbox` | Order và command cùng transaction; restart vẫn giao lệnh |
| EXT-6 | `docker-compose.http-interface.yml` | `http-interface,recovery` | `@ImportHttpServices` cho InventoryApi và PaymentApi |

Ví dụ Shipping:

```powershell
docker compose -f docker-compose.yml -f docker-compose.shipping.yml up -d --build
```

Chạy suite mở rộng tự động sau một lượt `test:failures` mới:

```powershell
python scripts/create_extension_collection.py
docker compose -f docker-compose.yml -f docker-compose.shipping.yml build
npm run test:extensions
```

Runner chuyển lần lượt recovery → shipping → timeout → HTTP interface →
outbox → choreography rồi khôi phục profile REST. Không xóa volume.
Có thể chạy riêng `node scripts/run-extensions.cjs outbox` hoặc tên profile
khác trong bảng. Riêng recovery cần order KB8 chưa được khôi phục.

Nếu tự chạy Postman, chọn đúng folder EXT ứng với profile hiện tại.
Không chạy toàn bộ collection mở rộng cùng lúc trên một profile.

### Các điểm triển khai

**Shipping:** PostgreSQL lưu `Shipment` theo `orderId`. Create và cancel
idempotent. Khi shipping thất bại sau thanh toán, stack LIFO hoàn tiền trước
khi trả kho. Saga log ghi rõ tên action để đối chiếu.

**Timeout:** delay nằm sau khi `PaymentService.pay()` đã commit, trước khi
controller trả response. Profile timeout đăng ký refund trước lời gọi pay.
Đây là phép thử mất response sau commit. Với tình huống khác — payment còn
chưa commit khi refund chạy — hệ thống thực tế cần thêm cancellation marker
hoặc cơ chế đối soát để tránh payment commit muộn sau refund.

**Recovery:** API chỉ nhận Saga `FAILED` hoặc `COMPENSATING`; từ chối đơn
đang chạy hoặc đã mua thành công. Retry tối đa ba lần cho từng action;
chỉ cancel khi tất cả compensation thành công. Không giữ transaction MySQL
trong lúc gọi HTTP. API dành cho môi trường lab, chưa tích hợp xác thực admin.

**Outbox:** bảng `outbox` lưu command và trạng thái. Tạo order và command
RESERVE trong cùng transaction MySQL; việc đánh dấu DONE và thêm command
kế tiếp cũng cùng transaction. Worker claim bằng row lock, lease 30 giây,
gọi HTTP ngoài transaction. Nếu crash sau remote commit nhưng trước DONE,
worker giao lại command sau khi lease hết hạn; receiver idempotent.
Refund/release lỗi ba lần sẽ giữ Saga COMPENSATING để vận hành khôi phục.
`GET /api/orders/{id}/outbox` cho phép kiểm tra tiến độ.

**Choreography:** topic `saga.events`, key là `orderId`, có ba partition.
Các loại event: `OrderCreatedEvent`, `InventoryReservedEvent`,
`InventoryFailedEvent`, `PaymentCompletedEvent`, `PaymentFailedEvent`,
`InventoryReleasedEvent`. Mỗi service có consumer group riêng. Topic được
tạo trước khi service chạy để consumer thấy đủ partition ngay khi subscribe.
Inventory trả kho khi nhận PaymentFailedEvent; Order chỉ cancel sau khi nhận
InventoryReleasedEvent. POST trả order đang xử lý; cần poll GET orders.
Phiên bản này minh họa choreography; khoảng trống giữa DB commit và publish
Kafka cần event outbox/inbox nếu triển khai production. EXT-5 minh họa riêng
transactional outbox cho các REST command, không tự động hợp nhất vào Kafka.

**HTTP interfaces:** profile đăng ký hai interface trong hai group với
`spring.http.serviceclient.<group>.base-url`. Adapter client chuyển sang proxy;
orchestrator vẫn dùng cùng API và bắt `RestClientException`.

## Kiểm tra JUnit và tài liệu lý thuyết

```powershell
mvn -B -ntp test -f order-service/pom.xml
```

Xem `REVIEW_ANSWERS.md` cho mười câu hỏi ôn tập và so sánh orchestration /
choreography. `scripts/create_collection.py` và
`scripts/create_extension_collection.py` tái tạo collection khi cần cập nhật test.

## Reset có chủ đích

Các lệnh sau **xóa dữ liệu lab Saga**. Không dùng khi cần giữ lịch sử order.
Chúng không quản lý container/volume của PacePal hay bài khác.

```powershell
docker compose -f docker-compose.yml -f docker-compose.shipping.yml -f docker-compose.choreography.yml down -v
docker compose up -d --build
```

## Nguồn chính thức đã đối chiếu

- [Spring Boot: HTTP service clients](https://docs.spring.io/spring-boot/reference/io/rest-client.html)
- [Spring Kafka: KafkaListener](https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/listener-annotation.html)
- [Apache Kafka: Quick Start](https://kafka.apache.org/41/getting-started/quickstart/)
