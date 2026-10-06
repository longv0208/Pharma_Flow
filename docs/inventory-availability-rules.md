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
b.status IN ('AVAILABLE', 'NEAR_EXPIRY') AND b.expiry_date > CURDATE()
```

Loại ra: `BLOCKED`, `OUT_OF_STOCK`, `EXPIRED`, và cả batch còn status AVAILABLE
nhưng `expiry_date` đã qua.

Đã có sẵn trong `InventoryDAO` — `public static final String ALLOCATABLE`:

```java
private static final String ALLOCATABLE =
    "b.status IN ('AVAILABLE','NEAR_EXPIRY') AND b.expiry_date > CURDATE()";
```

Mọi query FEFO / availability / alert append fragment này. Không copy-paste
điều kiện, không để mỗi module tự định nghĩa.

## 3. Status là cache, expiry_date là truth

- "Expired" = `expiry_date <= CURDATE()` — bất kể cột `status` đang ghi gì.
  Trong Java dùng `!expiryDate.after(today)` (tức `<=`), không dùng `.before()`
  vì hết hạn đúng hôm nay cũng là expired. Đã áp ở `InventoryServlet`
  (expiryWarning) và `InventoryDAO.unblockBatch`.
- Mọi query allocate đều check `expiry_date` trực tiếp; không phụ thuộc job
  quét đã flip status kịp hay chưa.
- `NEAR_EXPIRY` vẫn saleable (giống ProductDAO hiện tại) — chỉ là cảnh báo.

## 4. Expiry flip — on-read, không cần cron sớm

- On-read: predicate ở §2 tự loại batch hết hạn khỏi luồng bán ngay cả khi
  status chưa flip. Batch "chết" khỏi bán mà không cần chạy job.
- On-write (khi nào có batch job / stocktake): một câu
  `UPDATE inventory_batches SET status='EXPIRED'
   WHERE expiry_date <= CURDATE() AND status != 'EXPIRED'`
  + một dòng `inventory_movements` type `ADJUSTMENT` để audit. Đây chỉ là
  bookkeeping, không ảnh hưởng luồng allocate.

## 5. Reserved chỉ nằm trên batch allocatable

- Block một batch có `reserved_quantity > 0`: phải release reservation trước
  (ghi `RESERVATION_RELEASE` movement) rồi mới `BLOCK`. ĐÃ enforce:
  `InventoryDAO.changeBatchStatus` trả `hasreserved`, `inventory-batch.jsp`
  ẩn nút Block + báo "release the reservations first". Không auto-release —
  phần release do online-order/reservation flow xử lý sau.
- Nếu không chặn ở đây, Saleable bị lệch vì reserved kẹt trên batch đã khóa.

## 6. Unblock

Backend đang tự pick status sau unblock (giữ nguyên):

- `expiry_date <= CURDATE()` → từ chối unblock (`expiredbatch`)
- `on_hand_quantity <= 0` → `OUT_OF_STOCK`
- còn lại → `AVAILABLE`

## 7. Checklist cho module tiếp theo

- FEFO pick: `WHERE <ALLOCATABLE> ORDER BY expiry_date ASC, batch_id ASC`
- POS/Online reserve: chỉ trừ trên batch allocatable; check `saleable >= qty`
  trước khi allocate
- Dynamic Alert low-stock: cảnh báo trên Saleable, không phải Physical On Hand
- Block batch có reserved: bắt release trước (§5)
