package controller;

import dao.VnpayPaymentDAO;
import dao.VnpayPaymentDAO.PaymentResult;
import util.VnpayConfig;
import util.VnpayUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * GET /payment/vnpay/return — the VNPAY Sandbox browser Return URL.
 *
 * Flow: verify vnp_SecureHash (HMAC-SHA512, timing-safe) → parse the signed
 * vnp_TxnRef ("PF<orderId>") → hand the result to VnpayPaymentDAO, which does
 * every DB check and write under row locks. Nothing is trusted before the
 * signature passes, and an invalid signature writes ZERO rows.
 *
 * LOCAL DEMO ONLY — production must use VNPAY IPN + reconciliation; the
 * browser Return URL is inherently spoofable and only safe here because the
 * HMAC check plus the server-side state machine do the real validation.
 *
 * No login gate: the VNPAY redirect may arrive without a session (e.g. the
 * customer paid in another browser profile). The signed TxnRef + DB state are
 * the identity — the customer-facing redirect afterwards may itself bounce
 * the user to login, which is fine.
 */
@WebServlet(name = "VnpayPaymentServlet", urlPatterns = {"/payment/vnpay/return"})
public class VnpayPaymentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Collect request params into a plain map for signature verification.
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            String[] values = e.getValue();
            if (values != null && values.length > 0) {
                params.put(e.getKey(), values[0]);
            }
        }

        // Step 1 — signature first. An untrusted request changes nothing.
        boolean signatureOk = VnpayUtil.verifyReturnSignature(params);
        if (!signatureOk) {
            getServletContext().log("VNPAY return rejected: invalid vnp_SecureHash");
            sendVerifyError(req, resp, params);
            return;
        }

        // Step 2 — resolve the order from the SIGNED merchant reference only.
        long orderId = VnpayUtil.parseTxnRef(params.get("vnp_TxnRef"));
        if (orderId <= 0) {
            getServletContext().log("VNPAY return rejected: bad vnp_TxnRef");
            sendVerifyError(req, resp, params);
            return;
        }

        // Step 2b — the signed TmnCode must be OUR terminal. A correctly
        // signed return for a different merchant terminal is not ours to
        // process, even though the signature itself verifies.
        String tmnCode = trim(params.get("vnp_TmnCode"));
        if (tmnCode.isEmpty() || !tmnCode.equals(VnpayConfig.tmnCode())) {
            getServletContext().log("VNPAY return rejected: vnp_TmnCode mismatch");
            sendVerifyError(req, resp, params);
            return;
        }

        long signedAmount = parseLong(params.get("vnp_Amount"));
        String transactionNo = trim(params.get("vnp_TransactionNo"));
        String responseCode = trim(params.get("vnp_ResponseCode"));
        String transactionStatus = trim(params.get("vnp_TransactionStatus"));
        boolean paid = "00".equals(responseCode) && "00".equals(transactionStatus);

        // Step 3 — DAO owns state validation + writes under locks.
        VnpayPaymentDAO dao = new VnpayPaymentDAO();
        PaymentResult result;
        if (paid) {
            result = dao.processSuccess(orderId, signedAmount, transactionNo);
        } else {
            result = dao.processFailure(orderId, signedAmount, transactionNo);
        }

        // Step 4 — customer-facing outcome. No secrets, no internals in the URL.
        switch (result.code) {
            case "SUCCESS":
            case "ALREADY_SUCCESS":
                resp.sendRedirect(req.getContextPath()
                        + "/orders?action=detail&id=" + orderId + "&ok=paid");
                return;
            case "PAYMENT_FAILED":
            case "ALREADY_FAILED":
                resp.sendRedirect(req.getContextPath()
                        + "/orders?action=detail&id=" + orderId + "&err=payment_failed");
                return;
            case "AMOUNT_MISMATCH":
                // Integrity error — logged server-side, nothing was written.
                resp.sendRedirect(req.getContextPath()
                        + "/orders?action=detail&id=" + orderId + "&err=payment_verify");
                return;
            default:
                resp.sendRedirect(req.getContextPath()
                        + "/orders?action=detail&id=" + orderId + "&err=payment_verify");
                return;
        }
    }

    /**
     * Integrity/verification error path — when a (best-effort) order id can be
     * read from the unsigned request we still land on the order detail so the
     * customer sees their order; otherwise the orders list. No secrets, no
     * internals in the redirect.
     */
    private void sendVerifyError(HttpServletRequest req, HttpServletResponse resp,
            Map<String, String> params) throws IOException {
        long orderId = VnpayUtil.parseTxnRef(params.get("vnp_TxnRef"));
        if (orderId > 0) {
            resp.sendRedirect(req.getContextPath()
                    + "/orders?action=detail&id=" + orderId + "&err=payment_verify");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/orders?err=payment_verify");
    }

    private static long parseLong(String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
