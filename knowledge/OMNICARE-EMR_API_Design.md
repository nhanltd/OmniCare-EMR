# TÀI LIỆU THIẾT KẾ API - OMNICARE EMR

**Người thiết kế:** Lê Thành Nhân
**Mục tiêu:** Cung cấp hệ thống API chuẩn RESTful, lấy cảm hứng từ kiến trúc HL7 FHIR để đảm bảo khả năng liên thông dữ liệu y tế. Tài liệu này đóng vai trò như bản lề kỹ thuật để trình bày năng lực thiết kế Backend trong các buổi phỏng vấn kỹ sư phần mềm.

## 1. Tiêu chuẩn thiết kế chung
*   **Protocol:** HTTP/1.1 qua TLS (HTTPS)
*   **Data Format:** `application/json`
*   **Authentication:** Bearer Token (JWT) thông qua Header `Authorization: Bearer {token}`
*   **Pagination:** Các endpoint trả về danh sách (GET) bắt buộc sử dụng phân trang `page` và `size`.

## 2. Phân hệ Hành chính (Administrative)

### 2.1. Đăng ký bệnh nhân mới (Tiếp đón)
*   **Endpoint:** `POST /api/v1/patients`
*   **Mô tả:** Tạo hồ sơ y tế cốt lõi cho bệnh nhân mới.

```json
// Request Body
{
  "resourceType": "Patient",
  "identifier": "079004123456", 
  "name": {
    "family": "Nguyễn",
    "given": "Văn A"
  },
  "gender": "male",
  "birthDate": "1990-01-01",
  "telecom": "+84901234567"
}

// Response (201 Created)
{
  "id": "pat-123e4567-e89b-12d3-a456-426614174000",
  "status": "success",
  "message": "Hồ sơ bệnh nhân được tạo thành công."
}