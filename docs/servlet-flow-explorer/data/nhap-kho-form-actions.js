/**
 * Action map trích từ source thật — màn goods-receipt-form.jsp (mode=create).
 * Không bịa route/method/SQL. Trường chưa chạy runtime được ghi verified: "static".
 */
window.SFE_ACTIONS = {
  page: {
    id: "goods-receipt-form-create",
    title: "Nhập kho (tạo phiếu nháp)",
    jsp: "web/WEB-INF/views/admin/goods-receipt-form.jsp",
    howReached: "GET /inventory/receipts?action=new → GoodsReceiptServlet.handleNewForm() → forward FORM_JSP",
    servlet: "src/java/controller/GoodsReceiptServlet.java",
    mapping: '@WebServlet(urlPatterns = {"/inventory/receipts"})',
    mode: "create",
    demoNote: "Dữ liệu đơn hàng / dòng sản phẩm bên dưới là DEMO — mô phỏng attribute receivableOrders, poItems, zones.",
    verified: "static",
    /** Thứ tự hotspot / danh sách Actions (badge 1–4). */
    actionOrder: ["po-change", "add-item", "cancel-link", "save-draft"]
  },

  actions: {
    "po-change": {
      id: "po-change",
      badge: 1,
      element: "Select Đơn đặt hàng",
      page: "goods-receipt-form.jsp",
      event: "change (JavaScript)",
      html: '<select id="purchaseOrderId" name="purchaseOrderId" required>',
      formAction: null,
      method: "GET",
      clientBehavior: "poSelect.addEventListener('change') → window.location.href = reloadUrl + '&po=' + pid + fields",
      reloadUrl: "/inventory/receipts?action=new",
      queryParams: [
        { name: "action", value: "new", source: "hardcoded trong reloadUrl (mode create)" },
        { name: "po", value: "(purchaseOrderId đã chọn)", source: "poSelect.value" },
        { name: "receiptDate", value: "(giữ form)", source: "receiptDate input" },
        { name: "invoiceNumber", value: "(giữ form)", source: "invoiceNumber input" },
        { name: "note", value: "(giữ form)", source: "note textarea" }
      ],
      servlet: "GoodsReceiptServlet",
      entryMethod: "doGet()",
      processMethod: "handleNewForm()",
      branch: 'case "new":',
      sourceRefs: [
        { file: "goods-receipt-form.jsp", lines: "362–380", note: "JS reload khi đổi PO" },
        { file: "GoodsReceiptServlet.java", lines: "118–136", note: "handleNewForm + loadPoContext" }
      ],
      daoCalls: [
        { dao: "PurchaseOrderDAO.findReceivable()", when: "mỗi lần render form create" },
        { dao: "PurchaseOrderDAO.findById(po)", when: "khi có ?po=N" },
        { dao: "PurchaseOrderDAO.findItems(po)", when: "khi có ?po=N → attribute poItems" },
        { dao: 'LocationDAO.findZones(null, "HOAT_DONG")', when: "loadActiveZones" }
      ],
      navigation: "RequestDispatcher.forward → goods-receipt-form.jsp",
      whyGet: "Comment JSP: chọn PO reload GET để server dựng lại dòng sản phẩm (ordered/received/remaining) — không có JSON API.",
      nextScreen: "Cùng form, có bảng sản phẩm + nút + Thêm sản phẩm được bật",
      traceReady: false
    },

    "add-item": {
      id: "add-item",
      badge: 2,
      element: "Nút + Thêm sản phẩm",
      page: "goods-receipt-form.jsp",
      event: "click (JavaScript)",
      html: '<button type="button" id="addItemBtn">+ Thêm sản phẩm</button>',
      formAction: null,
      method: null,
      clientBehavior: "clone #grRowTemplate → append vào #itemsBody — KHÔNG gửi HTTP",
      servlet: null,
      entryMethod: null,
      processMethod: null,
      branch: null,
      sourceRefs: [
        { file: "goods-receipt-form.jsp", lines: "442–448", note: "addBtn click clone template" },
        { file: "goods-receipt-form.jsp", lines: "533–575", note: '<template id="grRowTemplate">' }
      ],
      daoCalls: [],
      navigation: "Không — chỉ thao tác DOM phía trình duyệt",
      whyGet: 'type="button" nên không submit form. Cần đã chọn PO (poItems không rỗng) nếu không nút bị disabled.',
      nextScreen: "Thêm 1 dòng lô trống trong bảng",
      traceReady: false
    },

    "cancel-link": {
      id: "cancel-link",
      badge: 3,
      element: "Link Hủy",
      page: "goods-receipt-form.jsp",
      event: "click → điều hướng",
      html: '<a class="btn btn-ghost" href="${ctx}/inventory/receipts">Hủy</a>',
      formAction: "/inventory/receipts",
      method: "GET",
      queryParams: [],
      servlet: "GoodsReceiptServlet",
      entryMethod: "doGet()",
      processMethod: "handleList() (default action=list)",
      branch: "default: handleList",
      sourceRefs: [
        { file: "goods-receipt-form.jsp", lines: "280", note: "link Hủy" },
        { file: "GoodsReceiptServlet.java", lines: "59–71, 108–115", note: "action null → list" }
      ],
      daoCalls: [
        { dao: "GoodsReceiptDAO.findAll(keyword, status)", when: "render danh sách" }
      ],
      navigation: "forward → goods-receipt-list.jsp",
      whyGet: "Anchor <a href> luôn là GET; không gửi body form.",
      nextScreen: "Danh sách phiếu nhập kho",
      traceReady: false
    },

    "save-draft": {
      id: "save-draft",
      badge: 4,
      element: "Nút Lưu nháp",
      page: "goods-receipt-form.jsp",
      event: "submit form",
      html: '<button type="submit" name="submitAction" value="draft" class="btn btn-secondary">Lưu nháp</button>',
      formAction: "/inventory/receipts?action=create",
      method: "POST",
      contentType: "application/x-www-form-urlencoded",
      parameters: [
        { name: "action", in: "query", value: "create", source: "form action URL" },
        { name: "submitAction", in: "body", value: "draft", source: "nút submit name/value" },
        { name: "purchaseOrderId", in: "body", value: "(select)", source: "form field" },
        { name: "receiptDate", in: "body", value: "(date)", source: "form field" },
        { name: "invoiceNumber", in: "body", value: "(text)", source: "form field" },
        { name: "note", in: "body", value: "(textarea)", source: "form field" },
        { name: "poItemId[]", in: "body", value: "parallel arrays", source: "mỗi dòng sản phẩm" },
        { name: "batchNumber[]", in: "body", value: "parallel arrays", source: "readItemsForm" },
        { name: "expiryDate[]", in: "body", value: "parallel arrays", source: "readItemsForm" },
        { name: "quantity[]", in: "body", value: "parallel arrays", source: "readItemsForm" },
        { name: "costPrice[]", in: "body", value: "parallel arrays", source: "readItemsForm" },
        { name: "binId[]", in: "body", value: "parallel arrays", source: "readItemsForm" },
        { name: "inspectionResult[]", in: "body", value: "CHO_KIEM_TRA|CHAP_NHAN|TU_CHOI", source: "readItemsForm" },
        { name: "rejectionReason[]", in: "body", value: "parallel arrays", source: "readItemsForm" }
      ],
      servlet: "GoodsReceiptServlet",
      entryMethod: "doPost()",
      processMethod: "handleCreate(req, resp, user)",
      branch: 'case "create":',
      authGate: "requireReceiver() — session currentUser role CHU_QUAN_QUAN_TRI hoặc NHAN_VIEN",
      sourceRefs: [
        { file: "goods-receipt-form.jsp", lines: "105–107, 281", note: "form action create + nút Lưu nháp" },
        { file: "GoodsReceiptServlet.java", lines: "76–90, 192–213", note: "doPost → handleCreate" },
        { file: "GoodsReceiptDAO.java", lines: "249–258", note: "createDraft INSERT BAN_NHAP" }
      ],
      daoCalls: [
        { dao: "GoodsReceiptDAO.createDraft(receipt, items)", sql: "INSERT INTO goods_receipts (...) VALUES (?,?,?,?,?,?,'BAN_NHAP')" },
        { dao: "insert items", sql: "INSERT INTO goods_receipt_items (...)" }
      ],
      successNav: "sendRedirect → /inventory/receipts?ok=created  (PRG)",
      failNav: "forward lại FORM_JSP với attribute errors (repopulateForm)",
      whyPost: "Tạo bản ghi mới / thay đổi DB → POST. Redirect sau khi OK = Post/Redirect/Get.",
      note: "Mode create KHÔNG có nút Xác nhận nhập — chỉ edit mới có formaction=confirm.",
      nextScreen: "Danh sách phiếu nhập (?ok=created) hoặc form với lỗi validation",
      branches: {
        success: "validate OK → status BAN_NHAP → createDraft → redirect list",
        validationFail: "errors không rỗng → repopulateForm forward form",
        dbFail: "receiptId <= 0 → errors.form → forward form"
      },
      traceReady: true
    },

    "gr-form": {
      id: "gr-form",
      badge: 0,
      element: "Form #grForm",
      page: "goods-receipt-form.jsp",
      event: "submit",
      html: '<form method="post" id="grForm" action=".../inventory/receipts?action=create">',
      formAction: "/inventory/receipts?action=${isEdit ? 'update' : 'create'}",
      method: "POST",
      clientBehavior: "submit listener: bật bin disabled, chặn nếu CHAP_NHAN thiếu bin, disable mọi nút submit chống double-submit",
      sourceRefs: [
        { file: "goods-receipt-form.jsp", lines: "105–107, 484–515", note: "form + submit guard" }
      ],
      servlet: "GoodsReceiptServlet",
      entryMethod: "doPost()",
      note: "Action cụ thể phụ thuộc nút submit (create vs confirm trên edit).",
      traceReady: false
    }
  }
};
