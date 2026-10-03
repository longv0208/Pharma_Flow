package controller;

import dao.CategoryDAO;
import dao.ProductDAO;
import model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Storefront catalog + product detail.
 *
 *   /products → grid, filters: ?q=&category=&type=&page= /products/{id} → detail
 * page (path-info id)
 *
 * Only ACTIVE + online_sale_allowed products are visible (same rule as home).
 */
@WebServlet(name = "ProductServlet", urlPatterns = {"/products", "/products/*"})
public class ProductServlet extends HttpServlet {

    private static final int PAGE_SIZE = 12;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path != null && path.length() > 1) {
            handleDetail(req, resp, path.substring(1));   // strip leading '/'
        } else {
            handleCatalog(req, resp);
        }
    }

    /* ==================== catalog ==================== */
    private void handleCatalog(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String q = trim(req.getParameter("q"));
        Long catId = parseId(req.getParameter("category"));
        String type = trimOrNull(req.getParameter("type"));
        int page = Math.max(1, (int) parseLong(req.getParameter("page"), 1));

        ProductDAO dao = new ProductDAO();
        int total = dao.countCatalog(q, catId, type);
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > pages) {
            page = pages;
        }

        req.setAttribute("products", dao.findCatalog(q, catId, type, PAGE_SIZE, (page - 1) * PAGE_SIZE));
        req.setAttribute("categories", new CategoryDAO().findAllActive());
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pages", pages);
        req.getRequestDispatcher("/WEB-INF/views/products.jsp").forward(req, resp);
    }

    /* ==================== detail ==================== */
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, String idPart)
            throws ServletException, IOException {
        long id = parseLong(idPart, -1);
        Product p = id > 0 ? new ProductDAO().findStorefrontById(id) : null;
        if (p == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("product", p);
        req.setAttribute("categories", new CategoryDAO().findAllActive());
        req.getRequestDispatcher("/WEB-INF/views/product-detail.jsp").forward(req, resp);
    }

    /* ==================== helpers ==================== */
    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimOrNull(String s) {
        String t = trim(s);
        return t.isEmpty() ? null : t;
    }

    private static long parseLong(String s, long fallback) {
        try {
            return s == null ? fallback : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Long parseId(String s) {
        long v = parseLong(s, -1);
        return v > 0 ? v : null;
    }
}
