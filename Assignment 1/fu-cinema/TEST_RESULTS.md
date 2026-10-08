# Kết quả kiểm thử — 2026-10-07

## Đã xác minh

| Kiểm tra | Kết quả |
|---|---|
| Build bốn project Java 21 / Boot 4.1.0 / Cloud 2025.1.3 | PASS — tạo được bốn executable JAR |
| Customer JUnit + ApplicationContext với SQL Server Docker | 6 test, 0 failure/error |
| Movie JUnit + ApplicationContext với MongoDB Docker | 7 test, 0 failure/error |
| Gateway ApplicationContext | 1 test, 0 failure/error |
| Booking JUnit + ApplicationContext với MySQL Docker | 13 test, 0 failure/error |
| Tổng JUnit của bốn project | 27 test, 0 failure/error |
| Collection đầy đủ folder 01–08 qua Gateway, chạy Newman | 85 request, 236 assertion, 0 failure |
| BR14 thực tế: dừng Movie Service rồi đặt vé | 503; 2 assertion PASS; Movie Service đã khởi động lại |
| Lịch sử booking và báo cáo khi Movie Service bị dừng | 2 API trả 200, snapshot và doanh thu chính xác |
| Booking bổ sung: vé null, ghế sai định dạng, rollback khi ghế bị xung đột, header giả, thời hạn hủy, đặc quyền Admin, booking nhiều suất chiếu | 16 kiểm tra PASS |
| MySQL: Flyway và tham chiếu ObjectId | Migration 1 đã áp dụng; `showtime_id`, `movie_id` là VARCHAR(24) |
| MySQL: snapshot Unicode đọc lại | `Hành Trình Phương Nam` đúng dấu |
| Report sau khi loại các booking bị hủy | 2 booking CONFIRMED, 3 vé, doanh thu 285000 |
| API bổ sung: JWT hết hạn/sai chữ ký, CRUD xóa bản ghi không có tham chiếu, lịch chiếu chạm biên, cập nhật loại trừ chính nó, tham chiếu phòng không tồn tại | 23 kiểm tra PASS |
| Giá vé có hơn hai chữ số thập phân | 400 — PASS |
| SQL Server: `customer_name` và giá trị đọc lại | NVARCHAR; `Nguyễn Văn An (updated)` đúng dấu |
| SQL Server Flyway | Migration 1 và 2 đã áp dụng |
| MongoDB: `_id` / `ticketPrice` | ObjectId / Decimal128 |
| MongoDB index | Unique `genreName`, unique `roomName` |
| Khởi động lại Movie Service | Bỏ qua seed; không nhân đôi dữ liệu |

Customer Service kết nối SQL Server Docker qua `127.0.0.2:1434`, theo lựa chọn của người dùng để tránh SQL Server Windows đang giữ các địa chỉ loopback chuẩn. SQL Server container vẫn dùng cổng nội bộ 1433.

Ba database đã chạy; SQL Server healthy và `sqlserver-init` Exited (0). Bốn ứng dụng đang chạy ở 8081, 8082, 8083, 9000; Gateway health UP. MySQL84 Windows đã được người dùng dừng để giải phóng port 3306.

Chi tiết các request, assertion và kiểm tra bổ sung: [verification-results.json](postman/verification-results.json). File này lưu kết quả đã rút gọn, không lưu access token.

## Bổ sung Postman Desktop — 08/10/2026

Sinh viên cung cấp [ảnh Collection Runner](postman/Result.png) của lần chạy lúc 13:48:13: một iteration, **236 assertion Passed, 0 Failed, 0 Errors**, thời gian 9 giây 576 ms. Ảnh đã được chèn vào README.

Lần chạy trước có 6 assertion thất bại bắt nguồn từ request 5.3 xung đột với suất chiếu thử nghiệm cũ còn SCHEDULED trong Room 02. Ảnh Desktop mới xác nhận toàn bộ assertion đã pass.

Annotated tag `v1.0.0` đã được tạo tại commit `4bbe002`. Tag hiện chưa bao gồm ảnh Desktop và các tài liệu bổ sung sau commit này.

Kiểm thử tự động đã pass toàn bộ. Báo cáo Newman đầy đủ và environment có token khi chạy được lưu trong `.runtime/` và không commit.
