(function () {
  "use strict";

  var data = window.SFE_ACTIONS;
  if (!data) {
    console.error("SFE_ACTIONS missing");
    return;
  }

  var els = {
    list: document.getElementById("sfeActionList"),
    inspector: document.getElementById("sfeInspector"),
    traceBtn: document.getElementById("sfeTraceBtn"),
    pageInfo: document.getElementById("sfePageInfo"),
    form: document.getElementById("grForm"),
    rightPanel: document.querySelector(".sfe-right"),
    inspectorToggle: document.getElementById("sfeInspectorToggle")
  };

  var activeId = null;
  var actionOrder = data.page.actionOrder || [
    "po-change",
    "add-item",
    "cancel-link",
    "save-draft"
  ];

  var PO_SYNC_FIELDS = ["receiptDate", "invoiceNumber", "note"];
  var HTTP_REFRESH_ACTIONS = { "po-change": true, "save-draft": true };

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function methodTag(action) {
    if (action.method === "POST") return '<span class="sfe-tag post">POST</span>';
    if (action.method === "GET") return '<span class="sfe-tag get">GET</span>';
    return '<span class="sfe-tag js">JS only</span>';
  }

  function shortRoute(action) {
    if (action.formAction) return action.method + " " + action.formAction;
    if (action.clientBehavior) return "Client-side";
    return "—";
  }

  function fieldValue(id, fallback) {
    var el = document.getElementById(id);
    return el && el.value !== "" ? el.value : fallback;
  }

  function collectFormBody() {
    if (!els.form) return "purchaseOrderId=12&receiptDate=2026-11-09&...";
    var fd = new FormData(els.form);
    fd.set("submitAction", "draft");
    var parts = [];
    fd.forEach(function (v, k) {
      parts.push(encodeURIComponent(k) + "=" + encodeURIComponent(String(v)));
    });
    return parts.join("&");
  }

  var httpDemoLines = {
    "cancel-link": function () {
      return [
        "GET /Pharma_Flow/inventory/receipts HTTP/1.1",
        "Host: localhost:8080",
        "Cookie: JSESSIONID=demo..."
      ].join("\n");
    },
    "po-change": function () {
      var poVal = fieldValue("purchaseOrderId", "12");
      return [
        "GET /Pharma_Flow/inventory/receipts?action=new" +
          "&po=" + encodeURIComponent(poVal) +
          "&receiptDate=" + encodeURIComponent(fieldValue("receiptDate", "2026-11-09")) +
          "&invoiceNumber=" + encodeURIComponent(fieldValue("invoiceNumber", "")) +
          "&note=" + encodeURIComponent(fieldValue("note", "")) +
          " HTTP/1.1",
        "Host: localhost:8080",
        "Cookie: JSESSIONID=demo..."
      ].join("\n");
    },
    "save-draft": function () {
      return [
        "POST /Pharma_Flow/inventory/receipts?action=create HTTP/1.1",
        "Host: localhost:8080",
        "Content-Type: application/x-www-form-urlencoded",
        "Cookie: JSESSIONID=demo...",
        "",
        collectFormBody()
      ].join("\n");
    }
  };

  function buildHttpPreview(action) {
    if (!action.method) {
      return "Không tạo HTTP request.\n" + (action.clientBehavior || "");
    }
    var builder = httpDemoLines[action.id];
    if (builder) return builder();
    return action.method + " " + (action.formAction || "") + " HTTP/1.1";
  }

  function refreshHttpPreview() {
    if (!activeId || !HTTP_REFRESH_ACTIONS[activeId]) return;
    var box = document.getElementById("sfeHttpBox");
    var action = data.actions[activeId];
    if (box && action) box.textContent = buildHttpPreview(action);
  }

  function dlFromPairs(title, pairs) {
    if (!pairs.length) return "";
    return (
      '<div class="sfe-section"><h4>' +
      escapeHtml(title) +
      '</h4><dl class="sfe-kv">' +
      pairs.join("") +
      "</dl></div>"
    );
  }

  function renderParameterSection(action) {
    if (action.parameters && action.parameters.length) {
      return dlFromPairs(
        "Parameters",
        action.parameters.map(function (p) {
          return (
            "<dt>" +
            escapeHtml(p.name) +
            '</dt><dd><code>' +
            escapeHtml(p.in) +
            "</code> · " +
            escapeHtml(p.value) +
            '<br><span class="sfe-muted-inline">' +
            escapeHtml(p.source || "") +
            "</span></dd>"
          );
        })
      );
    }
    if (action.queryParams && action.queryParams.length) {
      return dlFromPairs(
        "Query params",
        action.queryParams.map(function (p) {
          return (
            "<dt>" +
            escapeHtml(p.name) +
            "</dt><dd>" +
            escapeHtml(p.value) +
            '<br><span class="sfe-muted-inline">' +
            escapeHtml(p.source || "") +
            "</span></dd>"
          );
        })
      );
    }
    return "";
  }

  function renderRefBlocks(items, opts) {
    opts = opts || {};
    if (!items || !items.length) return opts.emptyHtml || "";

    var extraClass = opts.extraClass || "";
    var valueKey = opts.valueKey || "note";

    return items
      .map(function (item) {
        var lines = item.lines ? " :" + escapeHtml(item.lines) : "";
        var label = item.file || item.dao;
        var detail = item.sql || item.when || item[valueKey] || "";
        return (
          '<div class="sfe-ref' +
          (extraClass ? " " + extraClass : "") +
          '"><div class="f">' +
          escapeHtml(label) +
          lines +
          '</div><div class="n">' +
          escapeHtml(detail) +
          "</div></div>"
        );
      })
      .join("");
  }

  function renderBranches(branches) {
    if (!branches) return "";
    return (
      '<div class="sfe-section"><h4>Nhánh xử lý</h4>' +
      Object.keys(branches)
        .map(function (key) {
          var fail = /fail|error/i.test(key);
          return (
            '<div class="sfe-branch' +
            (fail ? " fail" : "") +
            '"><strong>' +
            escapeHtml(key) +
            ":</strong> " +
            escapeHtml(branches[key]) +
            "</div>"
          );
        })
        .join("") +
      "</div>"
    );
  }

  function renderInspector(action) {
    if (!action) {
      els.inspector.innerHTML =
        '<p class="sfe-empty">Chọn một thành phần có đánh số trên giao diện (hoặc danh sách bên trái) để xem Action Inspector.</p>';
      els.traceBtn.disabled = true;
      return;
    }

    var nav =
      action.navigation || action.successNav || action.nextScreen || "—";
    var rationale = action.whyGet || action.whyPost || action.note || "";

    els.inspector.innerHTML =
      '<p class="sfe-insp-title">Nút / phần tử: ' +
      escapeHtml(action.element) +
      "</p>" +
      '<p class="sfe-insp-sub">File giao diện: <code class="sfe-code">' +
      escapeHtml(action.page) +
      "</code></p>" +
      '<dl class="sfe-kv">' +
      "<dt>HTML</dt><dd><code>" +
      escapeHtml(action.html || "—") +
      "</code></dd>" +
      "<dt>Event</dt><dd>" +
      escapeHtml(action.event || "—") +
      "</dd>" +
      "<dt>Form action</dt><dd><code>" +
      escapeHtml(action.formAction || "(không submit)") +
      "</code></dd>" +
      "<dt>HTTP method</dt><dd>" +
      escapeHtml(action.method || "—") +
      "</dd>" +
      "<dt>Servlet</dt><dd>" +
      escapeHtml(action.servlet || "—") +
      "</dd>" +
      "<dt>Entry</dt><dd><code>" +
      escapeHtml(action.entryMethod || "—") +
      "</code></dd>" +
      "<dt>Xử lý</dt><dd><code>" +
      escapeHtml(action.processMethod || "—") +
      "</code></dd>" +
      "<dt>Nhánh</dt><dd><code>" +
      escapeHtml(action.branch || "—") +
      "</code></dd>" +
      "<dt>Điều hướng</dt><dd>" +
      escapeHtml(nav) +
      "</dd>" +
      "</dl>" +
      '<div class="sfe-section"><h4>HTTP request (demo)</h4><div class="sfe-http-box" id="sfeHttpBox"></div></div>' +
      renderParameterSection(action) +
      '<div class="sfe-section"><h4>Vì sao?</h4><p class="sfe-prose">' +
      escapeHtml(rationale) +
      "</p></div>" +
      '<div class="sfe-section"><h4>Source references</h4>' +
      renderRefBlocks(action.sourceRefs) +
      "</div>" +
      '<div class="sfe-section"><h4>DAO / SQL</h4>' +
      renderRefBlocks(action.daoCalls, {
        extraClass: "sfe-dao",
        valueKey: "when",
        emptyHtml: '<p class="sfe-empty">Không gọi DAO.</p>'
      }) +
      "</div>" +
      renderBranches(action.branches) +
      '<div class="sfe-section"><h4>Màn hình tiếp theo</h4><p class="sfe-prose">' +
      escapeHtml(action.nextScreen || "—") +
      "</p></div>";

    var box = document.getElementById("sfeHttpBox");
    if (box) box.textContent = buildHttpPreview(action);

    els.traceBtn.disabled = !action.traceReady;
    els.traceBtn.title = action.traceReady
      ? "Mở mô phỏng từng bước (sắp có)"
      : "Action này chưa gắn Trace Flow đầy đủ — ưu tiên Lưu nháp trước";
  }

  function selectAction(id, scrollHotspot) {
    activeId = id;
    var action = data.actions[id];

    els.list.querySelectorAll("button").forEach(function (btn) {
      btn.classList.toggle("active", btn.dataset.actionId === id);
    });
    document.querySelectorAll(".sfe-hotspot").forEach(function (hotspot) {
      hotspot.classList.toggle("active", hotspot.dataset.actionId === id);
    });

    renderInspector(action);

    if (scrollHotspot) {
      var target = document.querySelector(
        '.sfe-hotspot[data-action-id="' + id + '"]'
      );
      if (target) target.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    if (
      els.rightPanel &&
      window.matchMedia("(max-width: 1100px)").matches
    ) {
      els.rightPanel.classList.add("open");
    }
  }

  function buildActionList() {
    els.list.innerHTML = "";
    actionOrder.forEach(function (id) {
      var action = data.actions[id];
      if (!action) return;

      var btn = document.createElement("button");
      btn.type = "button";
      btn.dataset.actionId = id;
      btn.innerHTML =
        '<span class="sfe-badge">' +
        action.badge +
        "</span>" +
        '<span class="sfe-action-meta">' +
        '<div class="t">' +
        escapeHtml(action.element) +
        "</div>" +
        '<div class="s">' +
        escapeHtml(shortRoute(action)) +
        "</div>" +
        methodTag(action) +
        "</span>";
      btn.addEventListener("click", function () {
        selectAction(id, true);
      });

      var li = document.createElement("li");
      li.appendChild(btn);
      els.list.appendChild(li);
    });
  }

  function wireHotspots() {
    document.querySelectorAll(".sfe-hotspot").forEach(function (hotspot) {
      hotspot.addEventListener("click", function (ev) {
        ev.preventDefault();
        ev.stopPropagation();
        selectAction(hotspot.dataset.actionId, false);
      });
    });
  }

  function wireFormDemo() {
    if (!els.form) return;

    els.form.addEventListener("submit", function (ev) {
      ev.preventDefault();
      selectAction("save-draft", false);
      alert(
        "Chế độ học tập: form không gửi tới server thật.\n" +
          "Xem Action Inspector bên phải — Trace Flow đầy đủ sẽ làm ở bước sau."
      );
    });

    var poSelect = document.getElementById("purchaseOrderId");
    if (poSelect) {
      poSelect.addEventListener("change", function () {
        selectAction("po-change", false);
        refreshHttpPreview();
      });
    }

    var addBtn = document.getElementById("addItemBtn");
    if (addBtn) {
      addBtn.addEventListener("click", function () {
        selectAction("add-item", false);
        var tbody = document.getElementById("itemsBody");
        var tpl = document.getElementById("grRowTemplate");
        if (tbody && tpl) tbody.appendChild(tpl.content.cloneNode(true));
      });
    }

    PO_SYNC_FIELDS.forEach(function (fieldId) {
      var field = document.getElementById(fieldId);
      if (!field) return;
      field.addEventListener("input", refreshHttpPreview);
    });
  }

  function initPageInfo() {
    if (!els.pageInfo) return;
    els.pageInfo.innerHTML =
      "<strong>" +
      escapeHtml(data.page.title) +
      "</strong> · <code>" +
      escapeHtml(data.page.jsp) +
      "</code>";
  }

  function wireChrome() {
    if (els.inspectorToggle && els.rightPanel) {
      els.inspectorToggle.addEventListener("click", function () {
        els.rightPanel.classList.toggle("open");
      });
    }

    els.traceBtn.addEventListener("click", function () {
      if (!activeId) return;
      var action = data.actions[activeId];
      if (!action || !action.traceReady) return;
      alert(
        "Trace Flow cho «" +
          action.element +
          "» sẽ là màn tiếp theo (12 bước Servlet Simulator).\n\n" +
          "Luồng đã xác minh từ source:\n" +
          "POST ?action=create → doPost → handleCreate → validateReceipt → createDraft → sendRedirect ?ok=created"
      );
    });
  }

  buildActionList();
  initPageInfo();
  wireHotspots();
  wireFormDemo();
  wireChrome();
  renderInspector(null);
})();
