package controller;

import dao.CategoryDAO;
import dao.ProductDAO;
import model.Category;
import model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /home — loads catalog data and forwards to home.jsp.
 *
 * Splits first 8 online-saleable products into 4 featured + 4 health-care
 * (positional on product_id ASC — schema has no "health care" flag). On DB
 * failure, forwards with `errorMessage` so JSP renders a graceful error state
 * instead of a 500.
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    private static final int CATEGORY_LIMIT = 6;
    private static final int PRODUCT_POOL = 8;
    private static final int FEATURED_COUNT = 4;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            CategoryDAO categoryDAO = new CategoryDAO();
            ProductDAO productDAO = new ProductDAO();

            List<Category> allCategories = categoryDAO.findAllActive();
            int categoryEnd = Math.min(CATEGORY_LIMIT, allCategories.size());
            List<Category> categories = allCategories.subList(0, categoryEnd);

            List<Product> products = productDAO.findOnlineSaleable(PRODUCT_POOL, null);
            int featuredEnd = Math.min(FEATURED_COUNT, products.size());
            List<Product> featured = products.subList(0, featuredEnd);

            List<Product> healthCare;
            if (products.size() > FEATURED_COUNT) {
                healthCare = products.subList(FEATURED_COUNT, products.size());
            } else {
                healthCare = new ArrayList<>();
            }

            req.setAttribute("categories", categories);
            req.setAttribute("featuredProducts", featured);
            req.setAttribute("healthCareProducts", healthCare);
        } catch (Exception e) {
            getServletContext().log("Home page data load failed", e);
            req.setAttribute("errorMessage",
                    "Catalog is temporarily unavailable. Please try again shortly.");
        }
        req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req, resp);
    }
}
