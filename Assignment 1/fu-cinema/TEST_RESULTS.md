# Kết quả kiểm thử — 2026-10-07

## Đã xác minh

| Kiểm tra | Kết quả |
|---|---|
| Build bốn project Java 21 / Boot 4.1.0 / Cloud 2025.1.3 | PASS — tạo được bốn executable JAR |
| Customer JUnit + ApplicationContext với SQL Server Docker | 6 test, 0 failure/error |
| Movie JUnit + ApplicationContext với MongoDB Docker | 7 test, 0 failure/error |
| Gateway ApplicationContext | 1 test, 0 failure/error |
| Booking unit test với Mockito | 12 test, 0 failure/error |
| Collection folder 01–05 qua Gateway, chạy Newman | 56 request, 154 assertion, 0 failure |
| API bổ sung: JWT hết hạn/sai chữ ký, CRUD xóa bản ghi không có tham chiếu, lịch chiếu chạm biên, cập nhật loại trừ chính nó, tham chiếu phòng không tồn tại | 23 kiểm tra PASS |
| Giá vé có hơn hai chữ số thập phân | 400 — PASS |
| SQL Server: `customer_name` và giá trị đọc lại | NVARCHAR; `Nguyễn Văn An (updated)` đúng dấu |
| SQL Server Flyway | Migration 1 và 2 đã áp dụng |
| MongoDB: `_id` / `ticketPrice` | ObjectId / Decimal128 |
| MongoDB index | Unique `genreName`, unique `roomName` |
| Khởi động lại Movie Service | Bỏ qua seed; không nhân đôi dữ liệu |

Customer Service kết nối SQL Server Docker qua `127.0.0.2:1434`, theo lựa chọn của người dùng để tránh SQL Server Windows đang giữ các địa chỉ loopback chuẩn. SQL Server container vẫn dùng cổng nội bộ 1433.

## Chưa xác minh đầy đủ

- MySQL Docker chưa khởi động được vì MySQL Windows (`MySQL84`, tiến trình con `mysqld` PID 7216) đang chiếm port 3306. Phiên hiện tại không có quyền dừng dịch vụ; đang chờ người dùng dừng bằng Administrator.
- Chưa chạy Booking ApplicationContext với MySQL Docker, Flyway MySQL và các API folder 06–08.
- Chưa kiểm thử BR14 với Movie Service thực sự bị dừng trong hệ thống đầy đủ. Unit test đã xác minh lỗi kết nối Feign được chuyển thành 503; collection kiểm tra thủ công được cung cấp riêng.
- Chưa có kết quả toàn bộ 85 request của collection.
- Chưa có ảnh Postman Desktop Collection Runner. Người dùng chọn tự chạy Desktop và cung cấp ảnh.
- Chưa gắn tag nộp bài `v1.0.0` vì các bước xác minh và ảnh nộp bài còn thiếu.

Các kết quả chưa chạy được giữ ở trạng thái chưa xác minh. Báo cáo Newman và environment có token khi chạy được lưu trong `.runtime/` và không commit.
