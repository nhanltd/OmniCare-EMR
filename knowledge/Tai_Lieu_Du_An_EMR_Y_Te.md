# TÀI LIỆU ĐẶC TẢ DỰ ÁN: HỆ THỐNG BỆNH ÁN ĐIỆN TỬ (EMR) VÀ PHÂN TÍCH Y TẾ

**Người thực hiện (Project Lead):** Lê Thành Nhân
**Tính chất dự án:** Sản phẩm thực chiến (Portfolio/CV Project)

## 1. Tổng quan dự án (Project Overview)
Dự án xây dựng một hệ thống Bệnh án điện tử (EMR) và xử lý dữ liệu y tế tập trung, mô phỏng lại luồng nghiệp vụ của một bệnh viện số. Hệ thống không chỉ dừng lại ở các thao tác CRUD thông thường mà còn tập trung giải quyết bài toán cốt lõi của ngành y tế: bảo mật dữ liệu nhạy cảm, liên thông dữ liệu chuẩn quốc tế và phân tích, theo dõi thời gian thực.

## 2. Ngăn xếp công nghệ (Tech Stack)
Dự án sử dụng bộ công nghệ chuyên sâu về Backend và Data Engineering, hướng tới kiến trúc ổn định và khả năng xử lý dữ liệu lớn:
*   **Core Backend (API Services):** Java và Spring Boot (Đảm bảo kiến trúc dịch vụ độc lập và xử lý Transaction ACID an toàn).
*   **Cơ sở dữ liệu (Database):** PostgreSQL lưu trữ kho dữ liệu lâm sàng tập trung (CDR - Clinical Data Repository), có thể kết hợp MS SQL Server để phân tách các phân hệ nghiệp vụ khác nhau.
*   **Data Pipeline & Analytics:** Apache NiFi kết hợp PySpark để thu thập, làm sạch luồng dữ liệu sinh hiệu tự động và tính toán các chỉ số vận hành lâm sàng.
*   **DevOps & Deployment:** Toàn bộ hệ thống được container hóa bằng Docker (cung cấp `docker-compose.yml`) để tối ưu hóa việc triển khai độc lập chỉ với 1-click.

## 3. Kiến trúc và Tính năng cốt lõi (Core Features)

### 3.1. Luồng dữ liệu tích hợp (Interoperability By Design)
*   Giao tiếp qua RESTful API tuân thủ tiêu chuẩn quốc tế **HL7 FHIR**.
*   Tự động hóa workflow: Kết nối luồng dữ liệu tiếp đón (HIS) -> Chỉ định cận lâm sàng -> Trả kết quả xét nghiệm (LIS) trên một Dashboard duy nhất (Rule of Three Clicks).

### 3.2. Tuân thủ Pháp lý & Bảo mật (Security & Compliance)
*   **Giám sát truy vết (Audit Trail):** Tự động ghi log toàn bộ thao tác hệ thống (Ai xem, xem khi nào, truy cập từ IP nào, thay đổi trường dữ liệu nào) để đáp ứng chuẩn bảo vệ dữ liệu riêng tư.
*   **Cơ chế lưu trữ bất biến (WORM - Write-Once-Read-Many):** Áp dụng quy tắc "Không bao giờ xóa". Các sai sót y lệnh phải được xử lý qua luồng "Đính chính" (sinh phiên bản mới đè lên phiên bản cũ, nhưng vẫn lưu trữ vĩnh viễn lịch sử sửa đổi).
*   **Quản lý danh tính (SMART on FHIR):** Sử dụng OAuth 2.0 / OpenID Connect để phân quyền an toàn cho các ứng dụng thứ ba truy xuất dữ liệu thông qua Access Token thay vì chia sẻ toàn bộ database.

## 4. Lộ trình Thực thi (Execution Roadmap)

*   **Giai đoạn 1: Khởi tạo & MVP (2 Tuần đầu)**
    *   Thiết kế sơ đồ ERD cho kho dữ liệu CDR trên Notion.
    *   Khởi tạo Project Spring Boot, viết các API quản lý hồ sơ bệnh nhân cơ bản và phân quyền (RBAC).
    *   Đóng gói Docker và viết file `README.md` chuyên nghiệp hướng dẫn deploy hệ thống.
*   **Giai đoạn 2: Xây dựng Data Pipeline (Tuần 3-4)**
    *   Thiết lập Apache NiFi giả lập luồng nhận dữ liệu xét nghiệm (JSON/HL7) đẩy tự động vào Database.
    *   Sử dụng PySpark để viết script xử lý batch, tính toán KPI thời gian chờ kết quả trung bình của bệnh nhân.
*   **Giai đoạn 3: Hoàn thiện tính năng nâng cao (Tuần 5)**
    *   Triển khai Audit Logging (Lưu vết thay đổi) bằng các cơ chế tự động (như Hibernate Envers hoặc Spring AOP).
    *   Tổng hợp tài liệu thiết kế, sơ đồ kiến trúc để sẵn sàng trình bày trong các buổi phỏng vấn vị trí Backend/Data Engineer.
