package controller;

import dao.CategoryDAO;
import dao.ProductDAO;
import dao.SupplierDAO;
import model.Category;
import model.Product;
import model.ProductType;
import model.Supplier;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * /admin — OWNER_ADMIN area. Dispatch via ?action= param (rule.md §22).
 *
 * Actions (GET): dashboard (default), categories, category-new, category-edit
 * suppliers, supplier-new, supplier-edit Actions (POST): category-create,
 * category-update, category-delete supplier-create, supplier-update,
 * supplier-delete
 *
 * "Delete" is always a soft delete — flips status to INACTIVE, keeps history.
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/admin"})
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!requireAdmin(req, resp)) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            action = "dashboard";
        }

        switch (action) {
            case "categories":
                handleCategoryList(req, resp);
                break;
            case "category-new":
                handleCategoryNewForm(req, resp);
                break;
            case "category-edit":
                handleCategoryEditForm(req, resp);
                break;
            case "suppliers":
                handleSupplierList(req, resp);
                break;
            case "supplier-new":
                handleSupplierNewForm(req, resp);
                break;
            case "supplier-edit":
                handleSupplierEditForm(req, resp);
                break;
            case "products":
                handleProductList(req, resp);
                break;
            case "product-new":
                handleProductNewForm(req, resp);
                break;
            case "product-edit":
                handleProductEditForm(req, resp);
                break;
            default:
                handleDashboard(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!requireAdmin(req, resp)) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        switch (action) {
            case "category-create":
                handleCategoryCreate(req, resp);
                break;
            case "category-update":
                handleCategoryUpdate(req, resp);
                break;
            case "category-delete":
                handleCategoryDelete(req, resp);
                break;
            case "category-activate":
                handleCategoryActivate(req, resp);
                break;
            case "supplier-create":
                handleSupplierCreate(req, resp);
                break;
            case "supplier-update":
                handleSupplierUpdate(req, resp);
                break;
            case "supplier-delete":
                handleSupplierDelete(req, resp);
                break;
            case "supplier-activate":
                handleSupplierActivate(req, resp);
                break;
            case "product-create":
                handleProductCreate(req, resp);
                break;
            case "product-update":
                handleProductUpdate(req, resp);
                break;
            case "product-delete":
                handleProductDelete(req, resp);
                break;
            case "product-activate":
                handleProductActivate(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    private void handleDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
    }

    private void handleCategoryList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        CategoryDAO dao = new CategoryDAO();
        req.setAttribute("categories", dao.findAll());
        req.getRequestDispatcher("/WEB-INF/views/admin/category-list.jsp").forward(req, resp);
    }

    private void handleCategoryNewForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("mode", "create");
        req.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(req, resp);
    }

    private void handleCategoryEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        CategoryDAO dao = new CategoryDAO();
        Category c = null;
        if (id > 0) {
            c = dao.findById(id);
        }
        if (c == null) {
            resp.sendRedirect(req.getContextPath() + "/admin?action=categories&err=notfound");
            return;
        }
        req.setAttribute("mode", "edit");
        req.setAttribute("category", c);
        req.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    private void handleCategoryCreate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String name = trim(req.getParameter("categoryName"));
        String desc = trim(req.getParameter("description"));

        Map<String, String> errors = validateCategoryForm(name);
        CategoryDAO dao = new CategoryDAO();
        if (errors.isEmpty() && dao.existsByName(name, 0)) {
            errors.put("categoryName", "This category name already exists.");
        }

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("mode", "create");
            req.setAttribute("categoryNameValue", name);
            req.setAttribute("descriptionValue", desc);
            req.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(req, resp);
            return;
        }

        dao.create(name, desc.isEmpty() ? null : desc);
        resp.sendRedirect(req.getContextPath() + "/admin?action=categories&ok=created");
    }

    private void handleCategoryUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("categoryId"));
        String name = trim(req.getParameter("categoryName"));
        String desc = trim(req.getParameter("description"));
        String status = trim(req.getParameter("status"));
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            status = "ACTIVE";
        }

        Map<String, String> errors = validateCategoryForm(name);
        CategoryDAO dao = new CategoryDAO();
        Category existing = null;
        if (id > 0) {
            existing = dao.findById(id);
        }
        if (existing == null) {
            resp.sendRedirect(req.getContextPath() + "/admin?action=categories&err=notfound");
            return;
        }
        if (errors.isEmpty() && dao.existsByName(name, id)) {
            errors.put("categoryName", "This category name already exists.");
        }

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("mode", "edit");
            existing.setCategoryName(name);
            existing.setDescription(desc);
            existing.setStatus(status);
            req.setAttribute("category", existing);
            req.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(req, resp);
            return;
        }

        dao.update(id, name, desc.isEmpty() ? null : desc, status);
        resp.sendRedirect(req.getContextPath() + "/admin?action=categories&ok=updated");
    }

    private void handleCategoryDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("categoryId"));
        if (id > 0) {
            new CategoryDAO().deactivate(id);
        }
        resp.sendRedirect(req.getContextPath() + "/admin?action=categories&ok=deactivated");
    }

    private void handleCategoryActivate(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("categoryId"));
        if (id > 0) {
            new CategoryDAO().activate(id);
        }
        resp.sendRedirect(req.getContextPath() + "/admin?action=categories&ok=activated");
    }

    /* ==================== Supplier handlers ==================== */
    private void handleSupplierList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        SupplierDAO dao = new SupplierDAO();
        req.setAttribute("suppliers", dao.findAll());
        req.getRequestDispatcher("/WEB-INF/views/admin/supplier-list.jsp").forward(req, resp);
    }

    private void handleSupplierNewForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("mode", "create");
        req.getRequestDispatcher("/WEB-INF/views/admin/supplier-form.jsp").forward(req, resp);
    }

    private void handleSupplierEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        SupplierDAO dao = new SupplierDAO();
        Supplier s = null;
        if (id > 0) {
            s = dao.findById(id);
        }
        if (s == null) {
            resp.sendRedirect(req.getContextPath() + "/admin?action=suppliers&err=notfound");
            return;
        }
        req.setAttribute("mode", "edit");
        req.setAttribute("supplier", s);
        req.getRequestDispatcher("/WEB-INF/views/admin/supplier-form.jsp").forward(req, resp);
    }

    private void handleSupplierCreate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Supplier s = readSupplierForm(req);
        Map<String, String> errors = validateSupplier(s);

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("mode", "create");
            req.setAttribute("supplier", s);
            req.getRequestDispatcher("/WEB-INF/views/admin/supplier-form.jsp").forward(req, resp);
            return;
        }

        new SupplierDAO().create(s);
        resp.sendRedirect(req.getContextPath() + "/admin?action=suppliers&ok=created");
    }

    private void handleSupplierUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("supplierId"));
        Supplier s = readSupplierForm(req);
        s.setSupplierId(id);
        String status = trim(req.getParameter("status"));
        if ("INACTIVE".equals(status)) {
            s.setStatus("INACTIVE");
        } else {
            s.setStatus("ACTIVE");
        }

        Map<String, String> errors = validateSupplier(s);
        SupplierDAO dao = new SupplierDAO();
        if (id <= 0 || dao.findById(id) == null) {
            resp.sendRedirect(req.getContextPath() + "/admin?action=suppliers&err=notfound");
            return;
        }

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("mode", "edit");
            req.setAttribute("supplier", s);
            req.getRequestDispatcher("/WEB-INF/views/admin/supplier-form.jsp").forward(req, resp);
            return;
        }

        dao.update(s);
        resp.sendRedirect(req.getContextPath() + "/admin?action=suppliers&ok=updated");
    }

    private void handleSupplierDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("supplierId"));
        if (id > 0) {
            new SupplierDAO().deactivate(id);
        }
        resp.sendRedirect(req.getContextPath() + "/admin?action=suppliers&ok=deactivated");
    }

    private void handleSupplierActivate(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("supplierId"));
        if (id > 0) {
            new SupplierDAO().activate(id);
        }
        resp.sendRedirect(req.getContextPath() + "/admin?action=suppliers&ok=activated");
    }

    private Supplier readSupplierForm(HttpServletRequest req) {
        Supplier s = new Supplier();
        s.setSupplierName(trim(req.getParameter("supplierName")));
        s.setContactPerson(trim(req.getParameter("contactPerson")));
        s.setPhone(trim(req.getParameter("phone")));
        s.setEmail(trim(req.getParameter("email")));
        s.setAddress(trim(req.getParameter("address")));
        s.setTaxBusinessInfo(trim(req.getParameter("taxBusinessInfo")));
        return s;
    }

    private Map<String, String> validateSupplier(Supplier s) {
        Map<String, String> errors = new HashMap<>();
        if (s.getSupplierName().isEmpty()) {
            errors.put("supplierName", "This field is required.");
        } else if (s.getSupplierName().length() > 200) {
            errors.put("supplierName", "Must be at most 200 characters.");
        }
        if (s.getContactPerson().length() > 150) {
            errors.put("contactPerson", "Must be at most 150 characters.");
        }
        if (s.getPhone().length() > 30) {
            errors.put("phone", "Must be at most 30 characters.");
        }
        if (s.getEmail().length() > 150) {
            errors.put("email", "Must be at most 150 characters.");
        } else if (!s.getEmail().isEmpty()
                && !s.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.put("email", "Invalid email format.");
        }
        return errors;
    }

    /* ==================== Product handlers ==================== */
    private static final int PRODUCT_PAGE_SIZE = 15;

    private void handleProductList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String kw = trim(req.getParameter("q"));
        Long catId = parseIdOrNull(req.getParameter("categoryId"));
        String type = trimOrNull(req.getParameter("type"));
        String status = trimOrNull(req.getParameter("status"));

        int page = (int) parseId(req.getParameter("page"));
        if (page < 1) {
            page = 1;
        }

        ProductDAO dao = new ProductDAO();
        int total = dao.countAll(kw, catId, type, status);
        int pages = (total + PRODUCT_PAGE_SIZE - 1) / PRODUCT_PAGE_SIZE;
        if (pages < 1) {
            pages = 1;
        }
        if (page > pages) {
            page = pages;
        }

        req.setAttribute("products", dao.findAll(kw, catId, type, status,
                PRODUCT_PAGE_SIZE, (page - 1) * PRODUCT_PAGE_SIZE));
        req.setAttribute("categories", new CategoryDAO().findAllActive());
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pages", pages);
        req.getRequestDispatcher("/WEB-INF/views/admin/product-list.jsp").forward(req, resp);
    }

    private void handleProductNewForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("mode", "create");
        req.setAttribute("categories", new CategoryDAO().findAllActive());
        req.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(req, resp);
    }

    private void handleProductEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        Product p = null;
        if (id > 0) {
            p = new ProductDAO().findById(id);
        }
        if (p == null) {
            resp.sendRedirect(req.getContextPath() + "/admin?action=products&err=notfound");
            return;
        }
        req.setAttribute("mode", "edit");
        req.setAttribute("product", p);
        req.setAttribute("categories", new CategoryDAO().findAllActive());
        req.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(req, resp);
    }

    private void handleProductCreate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Product p = readProductForm(req);
        p.setStatus("ACTIVE");
        Map<String, String> errors = validateProduct(p, 0);
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("mode", "create");
            req.setAttribute("product", p);
            req.setAttribute("categories", new CategoryDAO().findAllActive());
            req.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(req, resp);
            return;
        }
        new ProductDAO().create(p);
        resp.sendRedirect(req.getContextPath() + "/admin?action=products&ok=created");
    }

    private void handleProductUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("productId"));
        Product p = readProductForm(req);
        p.setProductId(id);
        String status = trim(req.getParameter("status"));
        if ("INACTIVE".equals(status)) {
            p.setStatus("INACTIVE");
        } else {
            p.setStatus("ACTIVE");
        }

        ProductDAO dao = new ProductDAO();
        if (id <= 0 || dao.findById(id) == null) {
            resp.sendRedirect(req.getContextPath() + "/admin?action=products&err=notfound");
            return;
        }
        Map<String, String> errors = validateProduct(p, id);
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("mode", "edit");
            req.setAttribute("product", p);
            req.setAttribute("categories", new CategoryDAO().findAllActive());
            req.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(req, resp);
            return;
        }
        dao.update(p);
        resp.sendRedirect(req.getContextPath() + "/admin?action=products&ok=updated");
    }

    private void handleProductDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("productId"));
        if (id > 0) {
            new ProductDAO().deactivate(id);
        }
        resp.sendRedirect(req.getContextPath() + "/admin?action=products&ok=deactivated");
    }

    private void handleProductActivate(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("productId"));
        if (id > 0) {
            new ProductDAO().activate(id);
        }
        resp.sendRedirect(req.getContextPath() + "/admin?action=products&ok=activated");
    }

    private Product readProductForm(HttpServletRequest req) {
        Product p = new Product();
        Long catId = parseIdOrNull(req.getParameter("categoryId"));
        if (catId == null) {
            p.setCategoryId(0L);
        } else {
            p.setCategoryId(catId);
        }
        p.setProductName(trim(req.getParameter("productName")));
        p.setSku(trim(req.getParameter("sku")));
        p.setBarcode(trim(req.getParameter("barcode")));
        p.setActiveIngredient(trim(req.getParameter("activeIngredient")));
        p.setStrength(trim(req.getParameter("strength")));
        p.setDosageForm(trim(req.getParameter("dosageForm")));
        p.setManufacturer(trim(req.getParameter("manufacturer")));
        p.setRegistrationNumber(trim(req.getParameter("registrationNumber")));
        p.setShortDescription(trim(req.getParameter("shortDescription")));
        p.setIndication(trim(req.getParameter("indication")));
        p.setUsageInstruction(trim(req.getParameter("usageInstruction")));
        p.setWarnings(trim(req.getParameter("warnings")));
        p.setContraindications(trim(req.getParameter("contraindications")));
        p.setProductType(ProductType.fromString(req.getParameter("productType")));
        p.setSellingUnit(trim(req.getParameter("sellingUnit")));
        String price = trim(req.getParameter("sellingPrice"));
        try {
            p.setSellingPrice(new java.math.BigDecimal(price));
        } catch (NumberFormatException e) {
            p.setSellingPrice(null);
        }
        p.setOnlineSaleAllowed("1".equals(req.getParameter("onlineSaleAllowed"))
                || "on".equals(req.getParameter("onlineSaleAllowed")));
        return p;
    }

    private Map<String, String> validateProduct(Product p, long excludeId) {
        Map<String, String> errors = new HashMap<>();
        if (p.getCategoryId() == null || p.getCategoryId() <= 0) {
            errors.put("categoryId", "Please choose a category.");
        }
        if (p.getProductName().isEmpty()) {
            errors.put("productName", "This field is required.");
        } else if (p.getProductName().length() > 200) {
            errors.put("productName", "Must be at most 200 characters.");
        }
        if (p.getSku().isEmpty()) {
            errors.put("sku", "This field is required.");
        } else if (p.getSku().length() > 100) {
            errors.put("sku", "Must be at most 100 characters.");
        } else if (new ProductDAO().existsBySku(p.getSku(), excludeId)) {
            errors.put("sku", "SKU already exists.");
        }
        if (p.getBarcode() != null && p.getBarcode().length() > 100) {
            errors.put("barcode", "Must be at most 100 characters.");
        } else if (new ProductDAO().existsByBarcode(p.getBarcode(), excludeId)) {
            errors.put("barcode", "Barcode already exists.");
        }
        if (p.getSellingUnit().isEmpty()) {
            errors.put("sellingUnit", "This field is required.");
        }
        if (p.getSellingPrice() == null) {
            errors.put("sellingPrice", "Enter a valid price.");
        } else if (p.getSellingPrice().signum() < 0) {
            errors.put("sellingPrice", "Price must be 0 or greater.");
        }
        if (p.getShortDescription() != null && p.getShortDescription().length() > 500) {
            errors.put("shortDescription", "Must be at most 500 characters.");
        }
        return errors;
    }

    /* ==================== helpers ==================== */
    /**
     * Gate: must be logged in as OWNER_ADMIN. Returns false after redirect.
     */
    private boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User && "OWNER_ADMIN".equals(((User) u).getRoleName())) {
            return true;
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return false;
    }

    /**
     * Validate the category form fields — same pattern as supplier/product.
     */
    private Map<String, String> validateCategoryForm(String name) {
        Map<String, String> errors = new HashMap<>();
        if (name.isEmpty()) {
            errors.put("categoryName", "This field is required.");
        } else if (name.length() > 150) {
            errors.put("categoryName", "Must be at most 150 characters.");
        }
        return errors;
    }

    private static long parseId(String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Long parseIdOrNull(String s) {
        long v = parseId(s);
        if (v > 0) {
            return v;
        }
        return null;
    }

    private static String trimOrNull(String s) {
        String t = trim(s);
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
