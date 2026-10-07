# Inventory Availability Rules

Contract cho mọi module đọc/ghi tồn kho (POS, Online allocation, Dynamic Alert,
Stocktake). Đọc kỹ trước khi viết query đụng `inventory_batches`.

## 1. Ba con số — không trộn lẫn

| Khái niệm | Công thức | Dùng cho |
|---|---|---|
| Physical On Hand | `SUM(on_hand_quantity)` trên mọi batch | Kiểm kê, báo cáo tồn vật lý |
| Reserved | `SUM(reserved_quantity)` trên mọi batch | Đơn online đang giữ chỗ |
| Saleable | `SUM(on_hand_quantity - reserved_quantity)` chỉ trên batch allocatable | Allocate, bán, cảnh báo hết hàng |

Inventory List hiện tại hiển thị `Available = on_hand - reserved` theo SRS đơn giản.
Khi làm POS / Dynamic Alert, nâng cấp list lên đủ 3 cột trên — chỉ là một
`SUM(CASE WHEN <allocatable> THEN on_hand - reserved ELSE 0 END)` trong cùng query,
không cần đổi schema.

## 2. Allocatable — predicate duy nhất

Một batch được allocate/bán khi và chỉ khi:

```sql
b.status IN ('CO_SAN', 'SAP_HET_HAN') AND b.expiry_date > CURDATE()
```

Loại ra: `BI_KHOA`, `HET_HANG`, `HET_HAN`, và cả batch còn status CO_SAN
nhưng `expiry_date` đã qua.

Đã có sẵn trong `InventoryDAO` — `public static final String ALLOCATABLE`:

```java
private static final String ALLOCATABLE =
    "b.status IN ('CO_SAN','SAP_HET_HAN') AND b.expiry_date > CURDATE()";
```

Mọi query FEFO / availability / alert append fragment này. Không copy-paste
điều kiện, không để mỗi module tự định nghĩa.

## 3. Status là cache, expiry_date là truth

- "Hết hạn" = `expiry_date <= CURDATE()` — bất kể cột `status` đang ghi gì.
  Trong Java dùng `!expiryDate.after(today)` (tức `<=`), không dùng `.before()`
  vì hết hạn đúng hôm nay cũng là hết hạn. Đã áp ở `InventoryServlet`
  (expiryWarning) và `InventoryDAO.unblockBatch`.
- Mọi query allocate đều check `expiry_date` trực tiếp; không phụ thuộc job
  quét đã flip status kịp hay chưa.
- `SAP_HET_HAN` vẫn saleable (giống ProductDAO hiện tại) — chỉ là cảnh báo.

## 4. Expiry flip — on-read, không cần cron sớm

- On-read: predicate ở §2 tự loại batch hết hạn khỏi luồng bán ngay cả khi
  status chưa flip. Batch "chết" khỏi bán mà không cần chạy job.
- On-write (khi nào có batch job / stocktake): một câu
  `UPDATE inventory_batches SET status='HET_HAN'
   WHERE expiry_date <= CURDATE() AND status != 'HET_HAN'`
  + một dòng `inventory_movements` type `DIEU_CHINH` để audit. Đây chỉ là
  bookkeeping, không ảnh hưởng luồng allocate.

## 5. Reserved chỉ nằm trên batch allocatable

- Khóa một batch có `reserved_quantity > 0`: phải release reservation trước
  (ghi `GIAI_PHONG_GIU_HANG` movement) rồi mới `KHOA`. ĐÃ enforce:
  `InventoryDAO.changeBatchStatus` trả `hasreserved`, `inventory-batch.jsp`
  ẩn nút Khóa + báo "release the reservations first". Không auto-release —
  phần release do online-order/reservation flow xử lý sau.
- Nếu không chặn ở đây, Saleable bị lệch vì reserved kẹt trên batch đã khóa.

## 6. Mở khóa

Backend đang tự pick status sau mở khóa (giữ nguyên):

- `expiry_date <= CURDATE()` → từ chối mở khóa (`expiredbatch`)
- `on_hand_quantity <= 0` → `HET_HANG`
- còn lại → `CO_SAN`

## 7. Checklist cho module tiếp theo

- FEFO pick: `WHERE <ALLOCATABLE> ORDER BY expiry_date ASC, batch_id ASC`
- POS/Online reserve: chỉ trừ trên batch allocatable; check `saleable >= qty`
  trước khi allocate
- Dynamic Alert low-stock: cảnh báo trên Saleable, không phải Physical On Hand
- Khóa batch có reserved: bắt release trước (§5)
