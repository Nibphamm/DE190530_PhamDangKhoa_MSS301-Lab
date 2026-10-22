# Trả lời câu hỏi ôn tập

1. **Đặt `@Transactional` trên orchestrator có rollback MongoDB không?**
   Không. Transaction manager MySQL chỉ điều khiển connection MySQL; HTTP
   sang Inventory là transaction khác. Saga cần gọi release qua HTTP.

2. **Vì sao compensation chạy ngược?**
   Bước sau có thể phụ thuộc tài nguyên của bước trước. Hoàn tác bước sau
   trước giữ đúng quan hệ phụ thuộc: shipping lỗi → refund → release →
   cancelOrder. Stack chỉ chứa action của bước đã thành công; riêng profile
   timeout đăng ký refund sớm để xử lý kết quả remote chưa biết chắc.

3. **Vì sao KB3 không gọi release?**
   Reserve báo thiếu hàng trước khi cập nhật kho; local transaction không
   giữ hàng nên stack chưa có release.

4. **Release không idempotent mà retry ba lần thì sao?**
   Nếu lần đầu đã cộng hàng nhưng response mất, retry có thể cộng tiếp,
   làm tồn kho sai. Reservation RELEASED khiến các lần sau không cộng nữa.

5. **Saga đảm bảo và đánh đổi thuộc tính ACID nào?**
   Mỗi local transaction giữ ACID trong database của nó. Chuỗi phân tán
   không có isolation toàn cục và không có atomic rollback tức thời.
   Compensation hướng tới nhất quán cuối cùng; thất bại compensation phải
   được retry/khôi phục. State và log bền vững hỗ trợ kiểm tra tiến độ.

6. **Saga so với 2PC?**
   2PC prepare rồi commit, có thể giữ lock và chờ coordinator khi có lỗi,
   gây blocking và tăng chi phí mạng/tài nguyên. Saga commit từng bước,
   ít giữ lock xuyên dịch vụ, nhưng có trạng thái trung gian và phải viết
   compensation. Orchestrator vẫn có thể là điểm lỗi của tiến trình nếu
   không lưu trạng thái/khôi phục. MongoDB không tham gia XA cùng hai JDBC
   database; một transaction Spring không biến ba service thành 2PC.

7. **Khi nào chọn choreography?**
   Khi các service phản ứng tự nhiên với sự kiện và cần giảm phụ thuộc vào
   một bộ điều phối. Cần theo dõi event, duplicate, thứ tự và consumer lag.
   Quy trình dài, nhiều nhánh và cần audit tập trung thường dễ quản lý hơn
   bằng orchestration.

8. **COMPENSATING kéo dài là gì và xử lý thế nào?**
   Có action hoàn tác chưa thành công; không được báo COMPENSATED giả.
   KB8 lưu ba lần release thất bại. Bật lại service và dùng profile recovery,
   POST `/api/orders/{id}/compensate`; refund/release idempotent cho phép
   gọi lại. Có thể thay API bằng job quét state lâu ngày trong hệ thống thật.

9. **Vì sao MongoDB cần replica set và transaction manager?**
   Reserve sửa cả product và reservation. Replica set hỗ trợ transaction
   nhiều document; MongoTransactionManager gắn session/transaction vào
   method Spring. Bỏ chúng không còn bảo đảm hai lần ghi là một đơn vị ACID;
   atomicity của một document riêng lẻ không đủ cho nghiệp vụ này.

10. **Vì sao Postman so sánh chênh lệch?**
    Database có volume và giữ số liệu sau restart. Snapshot cho biết tác động
    của lượt chạy hiện tại, tránh giả định mọi lần đều bắt đầu ở 10/5000.
    Vẫn phải còn đủ hàng và số dư để đi đúng nhánh thành công mong muốn.

## Đối chiếu debug hai kiến trúc

| Tình huống | REST orchestration | Kafka choreography |
|---|---|---|
| Xác định bước lỗi | Xem orchestrator và saga_log | Theo orderId qua event và ba consumer group |
| Payment ngừng chạy | HTTP lỗi, release ngay nếu Inventory còn hoạt động | Event chờ consumer Payment; cần kiểm tra lag |
| Response / thời điểm hoàn tất | POST đồng bộ trả trạng thái Saga | POST tạo order; poll trạng thái cuối |
| Duplicate | Retry HTTP dùng reservation/payment idempotent | Replay event vẫn gọi local operation idempotent |
| Lỗi sau DB commit trước gửi lệnh/event | Base cần phục hồi; EXT-5 lưu REST command trong outbox | Cần kết hợp event outbox/inbox khi triển khai thật |
| Công cụ kiểm tra | Saga logs, HTTP và dữ liệu ba DB | Thêm Kafka topic, offset và consumer logs |
