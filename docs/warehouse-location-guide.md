# Hướng dẫn vận hành web — Vị trí kho (Zone / Rack / Bin)

Dành cho người **đã quen dùng PharmaFlow** (nhập kho, tồn, POS, đơn online).  
Chỉ mô tả **cách thao tác trên giao diện web** sau khi hệ thống đã có chức năng vị trí kho.

> Giả định: app đã deploy, DB đã migrate. Mở site như trước, ví dụ  
> `http://localhost:9999/Pharma_Flow/` (port tùy máy).

**Đăng nhập thường dùng** (mật khẩu hầu hết `123123`):

| Tài khoản | Vai trò | Làm gì trên web |
|-----------|---------|-----------------|
| `admin` | Quản trị | Vị trí kho, nhập kho, chuyển vị trí, kiểm kê |
| `staff` | Nhân viên | POS, fulfillment, xem tồn / nhập / kiểm kê |
| `ship` | Giao hàng | Giao đơn online |
| `tester` | Khách | Mua online |

---

## 1. Có gì mới trên menu?

Đăng nhập **admin** hoặc **staff** → sidebar **Kho**:

| Mục mới / đổi | Làm gì |
|---------------|--------|
| **Vị trí kho** | Tạo / sửa / ngừng Zone, Kệ (Rack), Ô (Bin) |
| **Chuyển vị trí** | Chuyển hàng từ ngăn này sang ngăn khác (cùng lô) |
| **Tồn kho → Chi tiết lô** | Thêm bảng vị trí: Zone / Kệ / Ngăn + số lượng |
| **Nhập kho** | Mỗi dòng chấp nhận phải chọn Zone → Kệ → Ngăn |
| **Kiểm kê** | Tạo đợt theo Toàn kho / Zone / Kệ / Ngăn |

Luồng POS, đơn online, giao hàng **vẫn vào như cũ** — hệ thống tự trừ/giữ đúng ngăn.

---

## 2. Vị trí kho (Zone → Kệ → Ngăn)

**Đường đi:** sidebar **Vị trí kho** → `/inventory/locations`

### 2.1 Xem danh sách
- Tab / link **Khu vực (Zone)**, **Kệ**, **Ô**.
- Có ô tìm theo mã/tên và lọc trạng thái.

### 2.2 Thêm vị trí (thứ tự đúng)
1. **Thêm khu vực** → nhập mã Zone (vd. `A`), tên → Lưu.  
2. Vào **Kệ** (lọc theo Zone vừa tạo) → **Thêm kệ** → mã Rack, tên.  
3. Vào **Ô** → chọn Kệ → **Thêm ô** → mã Bin (vd. `A-01`).

Quy tắc trên UI:
- Mã **không trùng** trong cùng cấp (cùng Zone không hai Rack cùng mã).
- **Ngừng dùng** chỉ khi ngăn/kệ/khu **không còn hàng**. Còn tồn → báo lỗi, phải chuyển hết hàng trước.

### 2.3 Hàng cũ sau nâng cấp
Hàng migrate thường nằm ở:

`TMP` / `UNASSIGNED` / `BIN-UNKNOWN`

→ Dùng **Chuyển vị trí** để đưa dần sang Zone/Kệ/Ngăn thật.

---

## 3. Chuyển vị trí

**Đường đi:** **Chuyển vị trí** → `/inventory/transfers`

1. Chọn sản phẩm / lô (batch).  
2. Chọn **ngăn nguồn** (đang có hàng khả dụng).  
3. Chọn **ngăn đích**.  
4. Nhập số lượng ≤ khả dụng tại nguồn.  
5. Nhập lý do → Xác nhận.

Kết quả:
- Tổng tồn của **lô không đổi**.
- Chỉ đổi chỗ giữa hai ngăn.
- Quá số khả dụng / cùng một ngăn → form báo lỗi, không chuyển.

---

## 4. Nhập kho (có chọn ngăn)

**Đường đi:** **Nhập kho** → như trước (chọn Đơn đặt hàng → thêm dòng).

### Thêm so với trước
Mỗi dòng **Chấp nhận** cần chọn:

**Zone → Kệ → Ngăn**

1. Chọn Zone → danh sách Kệ cập nhật.  
2. Chọn Kệ → danh sách Ngăn cập nhật.  
3. Chọn Ngăn.  
4. Điền số lô, hạn dùng, SL, giá, kết quả kiểm tra.  
5. **Lưu nháp** → kiểm tra lại → **Xác nhận nhập**.

Gợi ý:
- Cùng một lô nhập vào **hai ngăn**: thêm **hai dòng**, cùng số lô/hạn, khác Bin, tổng SL không vượt PO.
- Chấp nhận mà **chưa chọn ngăn** → không xác nhận được (bắt buộc chọn Bin).
- Sau xác nhận: **Tồn kho → chi tiết lô** thấy số tăng đúng ngăn vừa chọn.

---

## 5. Xem tồn theo vị trí

**Đường đi:** **Tồn kho** → chọn sản phẩm → **Xem** một lô.

Trên trang chi tiết lô có khối **Vị trí trong kho**:

| Cột | Ý nghĩa |
|-----|---------|
| Zone / Kệ / Ngăn | Chỗ đang để hàng |
| Tồn vật lý | On-hand tại ngăn |
| Đã giữ | Reserved (đơn online) |
| Khả dụng | Tồn − đã giữ |
| Tổng lô | Tổng mọi ngăn của lô đó |

Dùng bảng này để biết hàng đang ở `BIN-UNKNOWN` hay đã về ngăn thật.

---

## 6. Kiểm kê theo phạm vi

**Đường đi:** **Kiểm kê**

### 6.1 Tạo đợt mới
Trên đầu trang có form **Tạo kiểm kê mới**:

1. Chọn phạm vi:
   - **Toàn kho**
   - **Một Zone** → chọn Zone
   - **Một Rack** → chọn Zone rồi Kệ
   - **Một Bin** → chọn Zone → Kệ → Ngăn
2. Bấm **Tạo kiểm kê**.

### 6.2 Bắt đầu & đếm
1. Mở đợt (trạng thái Bản nháp) → **Bắt đầu kiểm kê**.  
2. Hệ thống tạo từng dòng = **một lô tại một ngăn** trong phạm vi.  
3. Bảng có cột Zone / Rack / Bin / sản phẩm / lô / SL hệ thống / SL thực tế.  
4. Nhập SL thực tế → **Lưu số đếm** (có thể lưu nhiều lần).  
5. Khi đủ → **Hoàn thành & Đối chiếu**.

### 6.3 Lỗi thường gặp khi hoàn tất
| Thông báo | Việc cần làm |
|-----------|--------------|
| Phải đếm hết các dòng | Điền SL thực tế (kể cả 0) cho mọi dòng |
| Dưới số đang giữ (below reserved) | Có đơn online đang giữ hàng tại ngăn — xử lý/hủy đơn trước hoặc không đếm thấp hơn số giữ |
| Tồn đã đổi lúc kiểm kê | Ai đó đã bán/nhập/chuyển trong lúc đếm — tạo đợt mới hoặc đếm lại |
| Không có lô để đếm | Phạm vi (Bin/Zone) trống hàng |

---

## 7. Điều chỉnh tồn

**Đường đi:** từ **Chi tiết lô** → Điều chỉnh (như trước).

**Mới:** bắt buộc chọn **Ngăn (Bin)** trước khi nhập thay đổi số lượng.  
Chỉ điều chỉnh đúng ngăn đang xem/chọn — không còn “trừ chung cả lô” không rõ chỗ.

---

## 8. POS / Online / Giao hàng (thao tác không đổi)

| Việc | Vào đâu | Ghi chú |
|------|---------|---------|
| Bán quầy | `staff` → **Bán hàng (POS)** | Thêm hàng → thanh toán như cũ; hệ thống tự trừ đúng ngăn (FEFO) |
| Mua online | `tester` → Cửa hàng → Giỏ → Checkout COD | Như cũ |
| Hủy đơn chờ xử lý | Khách → Đơn hàng → Hủy | Giải phóng giữ đúng ngăn |
| Xử lý đơn | `staff` → **Đơn hàng trực tuyến** | Xác nhận → chuẩn bị → sẵn sàng |
| Giao hàng | `ship` → **Đơn giao hàng** | Bắt đầu giao → xác nhận đã giao |

Bạn **không** chọn Bin trên màn POS/checkout — Bin do hệ thống chọn theo hạn dùng và tồn khả dụng.

---

## 9. Quy trình gợi ý sau khi lên chức năng mới

1. **admin** tạo Zone / Kệ / Ngăn thật (ngoài `TMP`).  
2. **Chuyển vị trí** hàng từ `BIN-UNKNOWN` sang ngăn thật.  
3. Nhập kho mới luôn chọn ngăn đích.  
4. Kiểm kê theo Bin hoặc Zone khi cần đối chiếu.  
5. Vận hành POS / online như trước.

---

## 10. Checklist thao tác nhanh (5 phút)

- [ ] Login `admin` → **Vị trí kho** thấy Zone (ít nhất `TMP`).  
- [ ] **Tồn kho** → mở 1 lô → thấy bảng vị trí.  
- [ ] **Chuyển vị trí** vài đơn vị `BIN-UNKNOWN` → Bin thật → thành công.  
- [ ] **Nhập kho** nháp 1 dòng chấp nhận + chọn Bin → Xác nhận → lô tăng đúng ngăn.  
- [ ] **Kiểm kê** tạo phạm vi Bin (có hàng) → Bắt đầu → thấy cột Zone/Rack/Bin.  
- [ ] (Tuỳ chọn) `staff` bán 1 món OTC trên POS thành công.

Hết. Không cần đụng SQL hay file code để vận hành hàng ngày.
