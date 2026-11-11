package controller;

import dao.LocationDAO;
import dao.LocationDAO.MutateResult;
import model.StorageBin;
import model.StorageRack;
import model.StorageZone;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * /inventory/locations — Zone / rack / bin master data.
 * Dispatch via ?action= (rule.md §22).
 *
 * Roles: CHU_QUAN_QUAN_TRI and NHAN_VIEN (same gate as inventory).
 */
@WebServlet(name = "LocationServlet", urlPatterns = {"/inventory/locations"})
public class LocationServlet extends HttpServlet {

    private static final int PAGE_SIZE = 20;
    private static final String BASE = "/inventory/locations";
    private static final String ZONES_JSP = "/WEB-INF/views/admin/location-zones.jsp";
    private static final String RACKS_JSP = "/WEB-INF/views/admin/location-racks.jsp";
    private static final String BINS_JSP = "/WEB-INF/views/admin/location-bins.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireInventoryAccess(req, resp);
        if (user == null) {
            return;
        }
        String action = trim(req.getParameter("action"));
        if (action.isEmpty()) {
            action = "zones";
        }

        switch (action) {
            case "racks":
                handleRacksList(req, resp);
                break;
            case "bins":
                handleBinsList(req, resp);
                break;
            case "zone-form":
                handleZoneForm(req, resp);
                break;
            case "rack-form":
                handleRackForm(req, resp);
                break;
            case "bin-form":
                handleBinForm(req, resp);
                break;
            case "api-racks":
                handleApiRacks(req, resp);
                break;
            case "api-bins":
                handleApiBins(req, resp);
                break;
            default:
                handleZonesList(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireInventoryAccess(req, resp);
        if (user == null) {
            return;
        }
        String action = trim(req.getParameter("action"));
        if (action.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        switch (action) {
            case "zone-create":
                handleZoneCreate(req, resp);
                break;
            case "zone-update":
                handleZoneUpdate(req, resp);
                break;
            case "zone-deactivate":
                handleZoneDeactivate(req, resp);
                break;
            case "zone-activate":
                handleZoneActivate(req, resp);
                break;
            case "rack-create":
                handleRackCreate(req, resp);
                break;
            case "rack-update":
                handleRackUpdate(req, resp);
                break;
            case "rack-deactivate":
                handleRackDeactivate(req, resp);
                break;
            case "rack-activate":
                handleRackActivate(req, resp);
                break;
            case "bin-create":
                handleBinCreate(req, resp);
                break;
            case "bin-update":
                handleBinUpdate(req, resp);
                break;
            case "bin-deactivate":
                handleBinDeactivate(req, resp);
                break;
            case "bin-activate":
                handleBinActivate(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET — lists ==================== */

    private void handleZonesList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String q = trimOrNull(req.getParameter("q"));
        String status = trimOrNull(req.getParameter("status"));
        LocationDAO dao = new LocationDAO();
        int offset = bindPage(req, dao.countZones(q, status));
        req.setAttribute("zones", dao.findZones(q, status, offset, PAGE_SIZE));
        req.getRequestDispatcher(ZONES_JSP).forward(req, resp);
    }

    private void handleRacksList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long zoneId = parseIdOrNull(req.getParameter("zoneId"));
        String q = trimOrNull(req.getParameter("q"));
        String status = trimOrNull(req.getParameter("status"));
        LocationDAO dao = new LocationDAO();
        int offset = bindPage(req, dao.countRacks(zoneId, q, status));
        req.setAttribute("racks", dao.findRacks(zoneId, q, status, offset, PAGE_SIZE));
        req.setAttribute("zones", dao.findZones(null, null));
        if (zoneId != null && zoneId > 0) {
            req.setAttribute("filterZone", dao.findZoneById(zoneId));
        }
        req.getRequestDispatcher(RACKS_JSP).forward(req, resp);
    }

    private void handleBinsList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long rackId = parseIdOrNull(req.getParameter("rackId"));
        Long zoneId = parseIdOrNull(req.getParameter("zoneId"));
        String q = trimOrNull(req.getParameter("q"));
        String status = trimOrNull(req.getParameter("status"));
        LocationDAO dao = new LocationDAO();
        int offset = bindPage(req, dao.countBins(rackId, zoneId, q, status));
        req.setAttribute("bins", dao.findBins(rackId, zoneId, q, status, offset, PAGE_SIZE));
        req.setAttribute("zones", dao.findZones(null, null));
        if (zoneId != null && zoneId > 0) {
            req.setAttribute("filterZone", dao.findZoneById(zoneId));
            req.setAttribute("racksInZone", dao.findRacks(zoneId, null, null));
        }
        if (rackId != null && rackId > 0) {
            req.setAttribute("filterRack", dao.findRackById(rackId));
        }
        req.getRequestDispatcher(BINS_JSP).forward(req, resp);
    }

    /* ==================== GET — forms ==================== */

    private void handleZoneForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        LocationDAO dao = new LocationDAO();
        if (id > 0) {
            StorageZone zone = dao.findZoneById(id);
            if (zone == null) {
                resp.sendRedirect(req.getContextPath() + BASE + "?err=notfound");
                return;
            }
            req.setAttribute("formMode", "edit");
            req.setAttribute("zone", zone);
        } else {
            req.setAttribute("formMode", "create");
        }
        String q = trimOrNull(req.getParameter("q"));
        String status = trimOrNull(req.getParameter("status"));
        int offset = bindPage(req, dao.countZones(q, status));
        req.setAttribute("zones", dao.findZones(q, status, offset, PAGE_SIZE));
        req.getRequestDispatcher(ZONES_JSP).forward(req, resp);
    }

    private void handleRackForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        Long zoneId = parseIdOrNull(req.getParameter("zoneId"));
        LocationDAO dao = new LocationDAO();
        if (id > 0) {
            StorageRack rack = dao.findRackById(id);
            if (rack == null) {
                resp.sendRedirect(req.getContextPath() + BASE + "?action=racks&err=notfound");
                return;
            }
            req.setAttribute("formMode", "edit");
            req.setAttribute("rack", rack);
            zoneId = rack.getZoneId();
        } else {
            req.setAttribute("formMode", "create");
        }
        req.setAttribute("zones", dao.findZones(null, null));
        if (zoneId != null && zoneId > 0) {
            req.setAttribute("prefillZoneId", zoneId);
        }
        String q = trimOrNull(req.getParameter("q"));
        String status = trimOrNull(req.getParameter("status"));
        int offset = bindPage(req, dao.countRacks(zoneId, q, status));
        req.setAttribute("racks", dao.findRacks(zoneId, q, status, offset, PAGE_SIZE));
        if (zoneId != null && zoneId > 0) {
            req.setAttribute("filterZone", dao.findZoneById(zoneId));
        }
        req.getRequestDispatcher(RACKS_JSP).forward(req, resp);
    }

    private void handleBinForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        Long rackId = parseIdOrNull(req.getParameter("rackId"));
        Long zoneId = parseIdOrNull(req.getParameter("zoneId"));
        LocationDAO dao = new LocationDAO();
        if (id > 0) {
            StorageBin bin = dao.findBinById(id);
            if (bin == null) {
                resp.sendRedirect(req.getContextPath() + BASE + "?action=bins&err=notfound");
                return;
            }
            req.setAttribute("formMode", "edit");
            req.setAttribute("bin", bin);
            rackId = bin.getRackId();
            zoneId = bin.getZoneId();
        } else {
            req.setAttribute("formMode", "create");
        }
        req.setAttribute("zones", dao.findZones(null, null));
        if (zoneId != null && zoneId > 0) {
            req.setAttribute("prefillZoneId", zoneId);
            req.setAttribute("racksInZone", dao.findRacks(zoneId, null, null));
            req.setAttribute("filterZone", dao.findZoneById(zoneId));
        }
        if (rackId != null && rackId > 0) {
            req.setAttribute("prefillRackId", rackId);
            req.setAttribute("filterRack", dao.findRackById(rackId));
        }
        String q = trimOrNull(req.getParameter("q"));
        String status = trimOrNull(req.getParameter("status"));
        int offset = bindPage(req, dao.countBins(rackId, zoneId, q, status));
        req.setAttribute("bins", dao.findBins(rackId, zoneId, q, status, offset, PAGE_SIZE));
        req.getRequestDispatcher(BINS_JSP).forward(req, resp);
    }

    /* ==================== GET — JSON cascade ==================== */

    private void handleApiRacks(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter w = resp.getWriter();
        Long zoneId = parseIdOrNull(req.getParameter("zoneId"));
        // Always emit a JSON array so fetch().json() never blows up on 400/empty body.
        if (zoneId == null || zoneId <= 0) {
            w.print("[]");
            return;
        }
        List<StorageRack> racks = new LocationDAO().findRacks(zoneId, null, "HOAT_DONG");
        w.print('[');
        boolean first = true;
        for (StorageRack r : racks) {
            if (!first) {
                w.print(',');
            }
            first = false;
            w.print("{\"rackId\":");
            w.print(r.getRackId());
            w.print(",\"rackCode\":\"");
            w.print(jsonEscape(r.getRackCode()));
            w.print("\",\"rackName\":\"");
            w.print(jsonEscape(r.getRackName()));
            w.print("\"}");
        }
        w.print(']');
    }

    private void handleApiBins(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter w = resp.getWriter();
        Long rackId = parseIdOrNull(req.getParameter("rackId"));
        if (rackId == null || rackId <= 0) {
            w.print("[]");
            return;
        }
        List<StorageBin> bins = new LocationDAO().findBins(rackId, null, null, "HOAT_DONG");
        w.print('[');
        boolean first = true;
        for (StorageBin b : bins) {
            if (!first) {
                w.print(',');
            }
            first = false;
            w.print("{\"binId\":");
            w.print(b.getBinId());
            w.print(",\"binCode\":\"");
            w.print(jsonEscape(b.getBinCode()));
            w.print("\"}");
        }
        w.print(']');
    }

    /* ==================== POST — zones ==================== */

    private void handleZoneCreate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        MutateResult r = new LocationDAO().createZone(
                trim(req.getParameter("zoneCode")),
                trim(req.getParameter("zoneName")));
        redirectMutate(req, resp, BASE, r, "created");
    }

    private void handleZoneUpdate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().updateZone(id,
                trim(req.getParameter("zoneCode")),
                trim(req.getParameter("zoneName")));
        redirectMutate(req, resp, BASE, r, "updated");
    }

    private void handleZoneDeactivate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().setZoneStatus(id, "NGUNG_HOAT_DONG");
        redirectMutate(req, resp, BASE, r, "deactivated");
    }

    private void handleZoneActivate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().setZoneStatus(id, "HOAT_DONG");
        redirectMutate(req, resp, BASE, r, "activated");
    }

    /* ==================== POST — racks ==================== */

    private void handleRackCreate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long zoneId = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().createRack(zoneId,
                trim(req.getParameter("rackCode")),
                trim(req.getParameter("rackName")));
        String back = BASE + "?action=racks" + rackZoneQuery(zoneId);
        redirectMutate(req, resp, back, r, "created");
    }

    private void handleRackUpdate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("rackId"));
        long zoneId = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().updateRack(id,
                trim(req.getParameter("rackCode")),
                trim(req.getParameter("rackName")));
        String back = BASE + "?action=racks" + rackZoneQuery(zoneId);
        redirectMutate(req, resp, back, r, "updated");
    }

    private void handleRackDeactivate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("rackId"));
        long zoneId = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().setRackStatus(id, "NGUNG_HOAT_DONG");
        String back = BASE + "?action=racks" + rackZoneQuery(zoneId);
        redirectMutate(req, resp, back, r, "deactivated");
    }

    private void handleRackActivate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("rackId"));
        long zoneId = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().setRackStatus(id, "HOAT_DONG");
        String back = BASE + "?action=racks" + rackZoneQuery(zoneId);
        redirectMutate(req, resp, back, r, "activated");
    }

    /* ==================== POST — bins ==================== */

    private void handleBinCreate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long rackId = parseId(req.getParameter("rackId"));
        long zoneId = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().createBin(rackId, trim(req.getParameter("binCode")));
        String back = BASE + "?action=bins" + binFilterQuery(zoneId, rackId);
        redirectMutate(req, resp, back, r, "created");
    }

    private void handleBinUpdate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("binId"));
        long rackId = parseId(req.getParameter("rackId"));
        long zoneId = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().updateBin(id, trim(req.getParameter("binCode")));
        String back = BASE + "?action=bins" + binFilterQuery(zoneId, rackId);
        redirectMutate(req, resp, back, r, "updated");
    }

    private void handleBinDeactivate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("binId"));
        long rackId = parseId(req.getParameter("rackId"));
        long zoneId = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().setBinStatus(id, "NGUNG_HOAT_DONG");
        String back = BASE + "?action=bins" + binFilterQuery(zoneId, rackId);
        redirectMutate(req, resp, back, r, "deactivated");
    }

    private void handleBinActivate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = parseId(req.getParameter("binId"));
        long rackId = parseId(req.getParameter("rackId"));
        long zoneId = parseId(req.getParameter("zoneId"));
        MutateResult r = new LocationDAO().setBinStatus(id, "HOAT_DONG");
        String back = BASE + "?action=bins" + binFilterQuery(zoneId, rackId);
        redirectMutate(req, resp, back, r, "activated");
    }

    /* ==================== helpers ==================== */

    private User requireInventoryAccess(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User) {
            User user = (User) u;
            String role = user.getRoleName();
            if ("CHU_QUAN_QUAN_TRI".equals(role) || "NHAN_VIEN".equals(role)) {
                return user;
            }
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return null;
    }

    private static void redirectMutate(HttpServletRequest req, HttpServletResponse resp,
            String path, MutateResult result, String okCode) throws IOException {
        String ctx = req.getContextPath();
        if (result.ok) {
            resp.sendRedirect(ctx + path + (path.contains("?") ? "&" : "?") + "ok=" + okCode);
        } else {
            String err = result.error != null ? result.error : "db";
            resp.sendRedirect(ctx + path + (path.contains("?") ? "&" : "?") + "err=" + err);
        }
    }

    private static String rackZoneQuery(long zoneId) {
        return zoneId > 0 ? "&zoneId=" + zoneId : "";
    }

    private static String binFilterQuery(long zoneId, long rackId) {
        StringBuilder sb = new StringBuilder();
        if (zoneId > 0) {
            sb.append("&zoneId=").append(zoneId);
        }
        if (rackId > 0) {
            sb.append("&rackId=").append(rackId);
        }
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /** Writes page, totalPages, total. Returns the SQL offset. */
    private int bindPage(HttpServletRequest req, int total) {
        int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        return (page - 1) * PAGE_SIZE;
    }

    private static int parsePage(String s) {
        if (s == null || s.isEmpty()) {
            return 1;
        }
        try {
            int p = Integer.parseInt(s.trim());
            return p < 1 ? 1 : p;
        } catch (NumberFormatException e) {
            return 1;
        }
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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimOrNull(String s) {
        String t = trim(s);
        return t.isEmpty() ? null : t;
    }
}
