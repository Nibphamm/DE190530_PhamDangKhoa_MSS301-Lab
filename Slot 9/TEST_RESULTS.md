# Kết quả kiểm thử Part 5 — Slot 9

Kiểm tra ngày 07/10/2026 trên Windows, JDK 21.0.11, Docker Desktop.

## Maven tests

Chạy `mvnw.cmd test` ở cả bốn service, gồm test Part 1–4 và Swagger mới.

| Project | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| product-service | 16 | 0 | 0 | 0 |
| inventory-service | 5 | 0 | 0 | 0 |
| order-service | 6 | 0 | 0 | 0 |
| api-gateway | 16 | 0 | 0 | 0 |
| **Tổng** | **43** | **0** | **0** | **0** |

Cả bốn project: **BUILD SUCCESS**.

23 test mới kiểm tra Swagger UI, metadata title/version/license, các HTTP
operation được ghi trong docs, CORS preflight, cấu hình dropdown, ba route
chuyển tiếp tới `/api-docs`, API yêu cầu JWT và ApplicationContext của Gateway.

## End-to-end với hệ thống thật

Khởi động MongoDB, MySQL và Keycloak bằng `docker compose up -d` tại Slot 9;
khởi động bốn service bằng Maven wrapper, rồi chạy `python scripts/verify_part5.py`.

Script kết thúc bằng **All Part 5 end-to-end checks passed.**

- Swagger UI tại 8080, 8081, 8082 và 9000 trả HTML thành công.
- `/api-docs` của từng service và ba URL docs tổng hợp trả JSON thành công,
  đúng title, version `v0.0.1` và các operation nghiệp vụ.
- Swagger config của Gateway có ba service.
- Các API qua Gateway không có JWT trả 401.
- Gateway cho phép CORS preflight với header Authorization.
- Keycloak cấp JWT thật bằng client credentials của realm được cung cấp.
- JWT thật gọi Product và Inventory qua Gateway thành công (200).
- JWT không hợp lệ bị từ chối (401).

Kiểm tra UI dùng HTTP và nội dung HTML/config; không tự động thao tác browser.
POST/PUT/DELETE Product và POST Order được xác minh trong Maven tests với
database Testcontainers; script end-to-end chỉ đọc dữ liệu nghiệp vụ.

## Commit theo từng TODO

| TODO | Commit | Nội dung |
| --- | --- | --- |
| DOC-1 | `6522f14` | Dependency Springdoc Product |
| DOC-2 | `9783428` | Đường dẫn Swagger Product |
| DOC-3 | `4cb6aa0` | Metadata OpenAPI Product |
| DOC-4 | `7d5a19a` | CORS Product |
| DOC-5 | `821cecd` | Dependency Springdoc Inventory |
| DOC-6 | `31ea29d` | Đường dẫn Swagger Inventory |
| DOC-7 | `67d3ba7` | Metadata OpenAPI Inventory |
| DOC-8 | `6a2a5a7` | CORS Inventory |
| DOC-9 | `38dc3b6` | Dependency Springdoc Order |
| DOC-10 | `6a5681d` | Đường dẫn Swagger Order |
| DOC-11 | `ed2cbdd` | Metadata OpenAPI Order |
| DOC-12 | `348f34e` | CORS Order |
| DOC-13 | `56064e7` | Dependency Springdoc Gateway |
| DOC-14 | `2890e23` | Dropdown Swagger tổng hợp |
| DOC-15 | `d86369d` | Ba route tổng hợp docs |
| DOC-16 | `bb0a2a8` | Cho phép Swagger public và CORS Gateway |

Commit nền, test, Docker Compose và hướng dẫn được tách riêng. Đã kiểm tra
Conventional Commits, tiêu đề không quá 50 ký tự và không có Co-Authored-By.
Không stage các tài nguyên gốc hoặc thay đổi ngoài Slot 9.
