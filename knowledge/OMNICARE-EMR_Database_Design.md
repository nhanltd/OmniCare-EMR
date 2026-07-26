# TÀI LIỆU THIẾT KẾ CƠ SỞ DỮ LIỆU - OMNICARE EMR[cite: 3]

**Người thiết kế:** Lê Thành Nhân[cite: 3]
**Hệ quản trị CSDL:** PostgreSQL[cite: 3]
**Triết lý thiết kế:** Tối ưu hóa chuẩn Normalized (chuẩn hóa) kết hợp với JSONB cho các dữ liệu đa hình (polymorphic data)[cite: 3]. Triển khai cơ chế Soft Delete (Xóa mềm) và Optimistic Locking để đảm bảo tính vẹn toàn y tế[cite: 3].

## 1. Cấu trúc Base Entity (Áp dụng cho mọi bảng)[cite: 3]
Tất cả các bảng nghiệp vụ bắt buộc phải kế thừa các trường quản trị (Audit fields) sau đây để phục vụ cho việc truy vết và chống tranh chấp dữ liệu đồng thời[cite: 3].

| Tên Cột (Column) | Kiểu dữ liệu (Data Type) | Ràng buộc (Constraints) | Mô tả (Description) |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Định danh duy nhất, chống tấn công dò ID[cite: 3]. |
| `created_at` | TIMESTAMP | NOT NULL | Thời điểm bản ghi được tạo (Tự động sinh bởi JPA Auditing)[cite: 3]. |
| `updated_at` | TIMESTAMP | NOT NULL | Thời điểm sửa đổi cuối cùng[cite: 3]. |
| `version` | INTEGER | NOT NULL, DEFAULT 0 | Cơ chế Optimistic Locking chống 2 bác sĩ lưu đè bệnh án[cite: 3]. |
| `is_deleted` | BOOLEAN | NOT NULL, DEFAULT false | Cơ chế Soft Delete (Xóa mềm)[cite: 3]. |

## 2. Phân hệ Bệnh nhân & Nhân sự (Core Roles)[cite: 3]

### 2.1. Bảng `patient` (Bệnh nhân)[cite: 3]
| Tên Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `identifier` | VARCHAR(20) | Số CCCD hoặc mã nội bộ (UNIQUE)[cite: 3]. |
| `full_name` | VARCHAR(100) | Họ và tên bệnh nhân[cite: 3]. |
| `gender` | VARCHAR(10) | Giới tính (male, female, other)[cite: 3]. |
| `birth_date` | DATE | Ngày tháng năm sinh[cite: 3]. |
| `phone_number` | VARCHAR(15) | Số điện thoại liên hệ[cite: 3]. |

### 2.2. Bảng `practitioner` (Nhân viên y tế)[cite: 3]
| Tên Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `license_number` | VARCHAR(50) | Số chứng chỉ hành nghề[cite: 3]. |
| `full_name` | VARCHAR(100) | Họ và tên bác sĩ/điều dưỡng[cite: 3]. |
| `specialty` | VARCHAR(50) | Chuyên khoa (Nội, Ngoại, Sản, Nhi...)[cite: 3]. |
| `role` | VARCHAR(20) | Vai trò (DOCTOR, NURSE, ADMIN) để phục vụ RBAC[cite: 3]. |

## 3. Phân hệ Khám chữa bệnh (Clinical Records)[cite: 3]

### 3.1. Bảng `encounter` (Phiên khám bệnh)[cite: 3]
Bảng trung tâm liên kết mọi hoạt động trong một lần bệnh nhân đến viện[cite: 3].

| Tên Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `patient_id` | UUID | FOREIGN KEY tham chiếu bảng patient[cite: 3]. |
| `practitioner_id` | UUID | FOREIGN KEY tham chiếu bảng practitioner[cite: 3]. |
| `status` | VARCHAR(20) | Trạng thái (PLANNED, IN_PROGRESS, FINISHED)[cite: 3]. |
| `start_time` | TIMESTAMP | Thời gian bắt đầu khám[cite: 3]. |
| `end_time` | TIMESTAMP | Thời gian kết thúc phiên khám[cite: 3]. |

### 3.2. Bảng `observation` (Chỉ số Sinh hiệu & Xét nghiệm)[cite: 3]
Sử dụng JSONB để linh hoạt lưu trữ các loại chỉ số khác nhau mà không cần thêm cột vào database[cite: 3].

| Tên Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `encounter_id` | UUID | FOREIGN KEY tham chiếu bảng encounter[cite: 3]. |
| `type` | VARCHAR(50) | Loại chỉ số (blood_pressure, heart_rate, glucose)[cite: 3]. |
| `value_json` | JSONB | Giá trị linh hoạt. VD: `{"systolic": 120, "diastolic": 80}`[cite: 3] |
| `unit` | VARCHAR(20) | Đơn vị đo lường (mmHg, bpm, mmol/L)[cite: 3]. |

## 4. Phân hệ Truy vết Bảo mật (Audit & Compliance)[cite: 3]

### 4.1. Bảng `audit_log` (Nhật ký hệ thống)[cite: 3]
Bảng này nhận dữ liệu từ các Aspect (AOP) trong Spring Boot, phục vụ tuân thủ Nghị định bảo vệ dữ liệu[cite: 3].

| Tên Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `action_type` | VARCHAR(20) | Loại hành động (CREATE, READ, UPDATE, DELETE)[cite: 3]. |
| `entity_name` | VARCHAR(50) | Tên bảng bị tác động (Ví dụ: Encounter)[cite: 3]. |
| `entity_id` | UUID | ID của bản ghi bị tác động[cite: 3]. |
| `actor_id` | UUID | ID của người thực hiện thao tác (từ JWT Token)[cite: 3]. |
| `ip_address` | VARCHAR(45) | Địa chỉ IP thực hiện request[cite: 3]. |
| `timestamp` | TIMESTAMP | Thời gian chính xác xảy ra sự kiện[cite: 3]. |