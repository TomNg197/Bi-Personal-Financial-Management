# Mrs Bi Handout

Ứng dụng Android quản lý lớp dạy cho cô giáo: lịch hôm nay, điểm danh, xong buổi / hoãn buổi, thời khoá biểu tuần, học phí tháng (đã thu / chưa thu), sao lưu Google Drive, khoá vân tay / khuôn mặt, tự cập nhật.

- Code: Kotlin + Jetpack Compose (`app/`).
- Quy tắc tiền: `app/src/main/java/vn/mrsbi/handout/data/Rules.kt` (test: `app/src/test/.../RulesTest.kt`).
- Bản cập nhật: nhánh `update` (`version.json` + APK đã ký). Khoá ký **không** nằm trong repo.

Build: `gradle assembleRelease` (cần JDK 17, Android SDK 35, `keystore.properties` riêng).
