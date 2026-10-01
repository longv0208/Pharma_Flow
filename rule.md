# PharmaFlow Project Coding Rules

> This file merges the generic coding rulebook (old `rule.md` §1–§86) with the
> team-specific Servlet/JSP conventions from `CursorRULE latest.md`.
> Where both sources cover the same topic, **CursorRULE wins** — it is the more
> specific instruction for this codebase.

## 1. Purpose

Project-wide coding rulebook for PharmaFlow — defines **how code is organized,
written, modified, tested, reviewed and AI-generated** so all contributors
follow one consistent style.

Not a functional requirements document — does not invent features.

- Requirements answer WHAT
- Architecture answers HOW major parts are designed
- This file answers HOW implementation work must be carried out consistently

All developers and AI coding assistants must read and follow this file before
creating, modifying, refactoring, testing, or reviewing code.

---

# 2. Rule Scope

Applies to: new features, bug fixes, refactoring, backend, frontend, API,
database access, migrations, validation, business logic, error handling,
logging, security, external integrations, testing, file organization, naming,
documentation, code review, AI-generated code.

Applies unless a newer explicit project decision overrides it.

---

# 3. Source of Truth and Priority

1. Explicit instruction in the current task
2. Current approved functional requirements / SRS
3. Current approved non-functional requirements
4. Architecture documentation
5. Database design / ERD
6. API specification
7. This `rule.md`
8. Existing code

If two higher-priority sources conflict, do not silently guess. Identify the
conflict before making a major implementation decision.

---

# 4. Tech Stack (locked)

- Java 17
- Jakarta EE 10 — **`jakarta.*` packages only, never `javax.*`**
- Servlet/JSP (Model-2), JSTL 3.x, JDBC + MySQL Connector/J
- Tomcat 10.x
- NetBeans Java Web project (Ant) — `src/java` sources, `web/` docroot
- MySQL 8, schema `pharmaflow` (source of truth: `../db_pharma.sql`)

Future scope (approved additions only): AI forecast module, Google
authentication. Do not pre-build abstractions for them.

---

# 5. AI Mandatory Workflow

Before writing code:
1. Read `rule.md`
2. Read the current task carefully
3. Inspect relevant existing files before creating new ones
4. Identify affected module(s), public contracts, DB entities, tests
5. Search for reusable implementations before introducing duplicates
6. Determine whether task requires schema/API/public-interface changes
7. Detect conflicts or missing info that would materially affect correctness

During implementation:
- Make the smallest coherent change that fully solves the task
- Preserve unrelated behavior
- Follow the existing approved architecture
- Reuse existing components
- Avoid speculative features and large unrelated refactors
- Keep names and patterns consistent

After implementation:
1. Review changed files for consistency
2. Run/update relevant tests when tooling is available
3. Check imports, compile errors, lint errors, dead code
4. Verify public behavior still matches requirements
5. Summarize important changes and remaining limitations

**Never claim tests passed unless they were actually executed.**

---

# 6. AI Decision Rules

When multiple implementation choices are possible:

1. Prefer the pattern already used consistently in the project
2. Prefer the simpler implementation
3. Prefer explicit code over clever code
4. Prefer maintainability over micro-optimization
5. Prefer standard framework mechanisms over custom infrastructure
6. Prefer local changes over system-wide changes when both solve the problem

Do not introduce a new framework, library, design pattern, or infrastructure
layer unless it clearly solves a real project need.

---

# 7. General Coding Principles

Priority order: Correctness → Readability → Consistency → Maintainability →
Testability → Simplicity.

Follow: KISS, DRY (meaningful duplication only), YAGNI, Separation of Concerns,
Single Responsibility, Fail Fast.

Do not over-engineer a student project. Do not create abstractions "for the
future."

---

# 8. Project Structure (actual)

```
src/java/
├── controller/    *Servlet (HttpServlet subclasses; @WebServlet URLs).
│                  All authentication concerns live in ONE AuthenController
│                  at /authen?action={login|register|logout}.
├── dao/           *DAO extends DBContext — JDBC + getFromResultSet
├── db/            DBContext.java — single shared JDBC config
├── model/         *entity + *Type enums (mirror DB tables); derived view
│                  helpers (isInStock, isPurchasable, getDisplayBadge) may
│                  live on the entity as pure getters used by JSP EL
└── util/          PasswordUtil (salted SHA-256 "salt:hash") — only added
                   when actually needed
```

**Deliberately absent** (kept out per YAGNI for this project size):
- No `dto/` package — JSPs read `model.*` entities directly
- No `service/` package — orchestration logic lives in the owning servlet;
  move to a service only when 2+ servlets share the same workflow
- No `listener/` package — DAOs manage their own connections; add a
  `ServletContextListener` only when real app-scoped state needs booting
- No `util/` package — add only when a real shared helper is needed
- No JSON API — pages are JSP-rendered; introduce `util/JsonUtil` and
  `/api/*` servlets only when a real consumer needs them

## Authentication (current mechanism)

- **Session-based auth** via `HttpSession` — no JWT, no Spring Security.
- `AuthenController` (`/authen?action=…`) handles login/register/logout.
- Session keys: `currentUser` (User entity), `currentUserRole` (role name).
- Login identifier = email OR username (single input field, `OR` query).
- Password storage: `PasswordUtil.hash(plain)` → `"salt:hash"` (base64,
  SHA-256). `PasswordUtil.verify(plain, stored)` for comparison — timing-safe
  via `MessageDigest.isEqual`.
- Role → landing: `CUSTOMER→/home`, `OWNER_ADMIN→/admin`,
  `PHARMACIST→/staff`, `SALES_STAFF→/pos`.
- Inactive accounts are denied at login (`status='INACTIVE'`).
- Role is **forced server-side** to CUSTOMER during public registration —
  request params are never trusted to set it.

web/
├── WEB-INF/web.xml         Servlet 6.0 descriptor + error pages only
├── WEB-INF/views/          JSPs — reachable ONLY via RequestDispatcher.forward
├── WEB-INF/jspf/           shared fragments (header, footer, product-card)
├── WEB-INF/lib/            runtime jars bundled into WAR (mysql + jstl)
├── css/, js/, images/
└── index.jsp               redirects to /home
```

Jars that are **compile-only** (e.g. `jakarta.servlet-api.jar`) live in
`lib/` at project root — **never** in `web/WEB-INF/lib/`, because NetBeans
copies that whole folder into the WAR.

---

# 9. Layer Responsibilities

## Controller (Servlet)

Does: parse path/query/body params, request-format validation, call DAO,
forward to JSP or write JSON. May hold page-specific orchestration (split
lists, pick sub-lists) — anything beyond that belongs in a shared service.

Does NOT: contain business rules that span resources, embed SQL.

## Service (only when actually needed)

Introduce a service only when 2+ servlets share the same workflow or a single
servlet grows business logic that no longer fits cleanly. For now this
project does not use a service layer.

## DAO

Reads/writes persisted data. Extends `DBContext` and exposes
`getFromResultSet(rs)` for row-mapping reuse. Does NOT contain business
rules, return HTTP responses, or hold UI logic.

## JSP

Renders UI from attributes set by servlet. Does NOT call DAO/DB directly,
reimplement authorization, or hold business logic.

Flow:

```
Servlet → DAO → DBContext → MySQL
          (JSP reads entity attributes directly)
```

---

# 10. Dependency Rules

One-directional: controller → dao → db.
No circular deps. No lower-level module depending on a higher-level one.
DAOs are instantiated per-request (`new CategoryDAO()`, `new ProductDAO()`)
because each carries its own connection lifecycle — do not share DAO instances
across requests or threads.

---

# 11–15. Naming

- PascalCase for classes. Suffixes: `Servlet`, `Service`, `DAO`, `DTO`/`Response`, `Listener`, `Util`, `Exception`, `Type` (enum).
- Verbs for methods: `findProductById`, `validateOrder`, `calculateTotal`.
- Booleans: `is…`, `has…`, `can…`, `exists…`.
- No vague names (`data`, `temp`, `obj`, `handler2`).
- Descriptive variables, reasonable abbreviations only (`id`, `dto`, `url`, `api`).
- Enums for finite state sets (`ProductType.OTC`, `ProductStatus.ACTIVE`). No magic strings/numbers for domain states.
- File names: Java = PascalCase, JSP = kebab-case, CSS/JS = kebab-case.

---

# 16. DTO Rules

Default: JSPs read `model.*` entities directly — no separate DTO layer.
Introduce a dedicated `*Response` / `*Request` class only when:

- The entity carries fields the public contract must not leak (passwords,
  internal status, cost price, …), or
- Multiple consumers need different field subsets, or
- The API shape diverges from the entity shape (joined fields, computed
  aggregates the entity cannot carry cleanly).

Entity-side derived view helpers (`isInStock`, `isPurchasable`,
`getDisplayBadge`) are the alternative — keep them pure, no I/O, no DB.

---

# 17. Entity Rules (CursorRULE)

- Entities mirror DB tables one-to-one.
- **Always use wrapper types** — `Integer`, `Long`, `Boolean`, `BigDecimal` —
  never `int`/`long`/`boolean` primitives.
- **Date/time fields → `java.sql.Date` / `java.sql.Timestamp`**
  (not `java.time.*` and not `java.util.Date`).
- **Never embed a related entity** — store the FK id as a wrapper:

```java
// BAD
private Role role;

// GOOD
private Integer roleId;
```

- **Lombok**: allowed and preferred for entity boilerplate
  (`@Data @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor @ToString`)
  **only when** the Lombok jar is added to `nbproject` classpath and the
  annotation processor is configured. Until the team adds it, write plain
  getters/setters — do NOT use Lombok imports without configuring the build.

```java
// Reference shape (when Lombok is configured):
@ToString @Builder @Data @AllArgsConstructor @NoArgsConstructor
public class Account {
    private Integer id;
    private String  username;
    // ...
    private Integer roleId;   // not: private Role role;
}
```

---

# 18. Mapping Rules

DAO maps `ResultSet → entity` inline via a dedicated `getFromResultSet(rs)`
method (see §20). Entities flow straight to JSP/JSON — no second mapping hop.

---

# 19. Validation

Request validation (required fields, length, format, range) at the controller
edge — parse `?limit=`/`?category=`/`?action=` once, throw
`IllegalArgumentException` on bad input → `400`. Backend is authoritative —
frontend checks are UX only.

Derived business rules that are pure functions of an entity's own fields
(e.g. `Product.isPurchasable()` = OTC + online_sale_allowed + in-stock) live
**on the entity** as derived getters. When a rule needs to coordinate
multiple entities or be reused across servlets, promote it to a service —
introduce `service/` then, not before.

---

# 20. DAO Rules (CursorRULE)

Every DAO **extends `DBContext`** and follows this contract:

1. **Always open a connection** via `getConnection()` (inherited).
2. **Always close resources** via `closeResources()` in a `finally` block.
3. **Always provide a `getFromResultSet(ResultSet rs)` method** so row-mapping
   is reused across queries in the same DAO.
4. Use `PreparedStatement` for every query — never concatenate user input
   into SQL.

Reference shape:

```java
public class AccountDAO extends DBContext {

    public Account getFromResultSet(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getInt("id"));
        // ...
        return a;
    }

    public int insert(Account account) {
        String sql = "INSERT INTO account (...) VALUES (?, ?, ...)";
        try {
            connection = getConnection();
            statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, account.getUsername());
            // ...
            int rows = statement.executeUpdate();
            if (rows == 0) throw new SQLException("Insert failed, no rows affected.");
            resultSet = statement.getGeneratedKeys();
            return resultSet.next() ? resultSet.getInt(1) : -1;
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return -1;
        } finally {
            closeResources();
        }
    }
}
```

---

# 21. DBContext (CursorRULE) — THE single DB entry point

- One file: `src/java/db/DBContext.java`.
- Holds `protected Connection connection`, `protected PreparedStatement statement`, `protected ResultSet resultSet` for subclasses.
- Constructor: `Class.forName("com.mysql.cj.jdbc.Driver")` then open initial connection with hardcoded `url`/`username`/`password` fields.
- `getConnection()` returns a fresh `Connection` per call — subclasses may either use the inherited `connection` field or call `getConnection()` directly.
- `closeResources()` closes resultSet → statement → connection, in that order, ignoring nulls.

```java
public class DBContext {
    protected Connection connection;
    protected ResultSet resultSet;
    protected PreparedStatement statement;

    public DBContext() {
        try {
            String username = "root";
            String password = "";
            String url = "jdbc:mysql://127.0.0.1:3306/pharmaflow?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(DBContext.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Connection getConnection() { return new DBContext().connection; }
    public void closeResources() { /* close rs → stmt → conn */ }
}
```

> **Note:** CursorRULE's hardcoded-credentials pattern conflicts with generic
> §30 below. For this project CursorRULE wins — team template requires it.
> Do NOT introduce `db.properties` or environment-variable indirection unless
> the team explicitly changes this rule.

---

# 22. Servlet / Controller Rules (CursorRULE)

## URL configuration

**Always use `@WebServlet` annotation** — never configure servlets in `web.xml`.
`web.xml` is reserved for `welcome-file-list`, `error-page`, filters, and
listeners only.

## Action dispatching

- `doGet` / `doPost` read an `action` request parameter and `switch` on it.
- **Every `case` calls a dedicated private method** — never write logic
  directly inside a `case`.
- CRUD on one resource lives in ONE controller (e.g. `ManageAccountServlet`
  handles `action=list|create|edit|deactivate` — do not split into
  `CreateAccountServlet` + `EditAccountServlet` + …).
- All authentication concerns live in ONE `AuthenController`.

```java
@WebServlet(name = "ManageAccountServlet", urlPatterns = {"/manage-account"})
public class ManageAccountServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) action = "list";
        switch (action) {
            case "list":       handleList(req, resp);       break;
            case "edit":       handleEditForm(req, resp);   break;
            case "deactivate": handleDeactivate(req, resp); break;
            default:           handleList(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) action = "";
        switch (action) {
            case "create": handleCreate(req, resp); break;
            case "update": handleUpdate(req, resp); break;
            default:       resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException { /* ... */ }
}
```

## Error handling in servlets

- `try/catch` around every service call that can throw.
- On error: log via `getServletContext().log(msg, e)` and either forward to
  error JSP (HTML endpoint) or write JSON error via `JsonUtil` (API endpoint).
- Never let a `ServletException` bubble up without context — wrap with the
  original request URI.

---

# 23. API Error Response (when an API exists)

```json
{
  "timestamp": "2026-10-02T10:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/example"
}
```

This shape applies when `/api/*` endpoints are introduced. For now the
project is JSP-only — no JSON emitter exists.

# 24. API Success Response (when an API exists)

```json
{ "data": [ ... ], "count": 3 }
```

Do not introduce alternate wrappers.

# 25. HTTP Rules

| Method | Use |
|--------|-----|
| GET    | Retrieve |
| POST   | Create / command |
| PUT    | Replace |
| PATCH  | Partial update |
| DELETE | Delete/deactivate |

Status codes: `200` `201` `204` `400` `401` `403` `404` `409` `422` `500`.
Do not return `200` for every outcome.

# 26. REST Endpoint Naming

Resource-oriented plural nouns: `/api/products`, `/api/products/{id}`.
Action-style routes are **allowed for page-facing controllers only**
(`/manage-account?action=edit`) per §22 — not for `/api/*`.

# 27. Database Access Rules

- All DB access through DAO layer — never from Servlet/JSP directly.
- `PreparedStatement` only.
- Avoid loading unnecessary columns.
- Pagination/filtering for potentially large lists.

# 28. Transaction Rules

Use transactions when multiple writes form one atomic operation — set
`connection.setAutoCommit(false)`, execute all statements, then `commit()` or
`roll back` in `finally`. Keep the boundary inside the DAO method for now;
introduce a service when a transaction spans multiple DAOs. Do not wrap
read-only operations in transactions.

# 29. Query Performance

Avoid N+1. Prefer a single `LEFT JOIN` + aggregate over per-row queries. Do
not load entire tables when `LIMIT` suffices.

# 30. Pagination (CursorRULE)

## JSP side

Build the base URL once with `<c:url>` + `<c:param>`, then append `&page=N`:

```jsp
<c:url value="/manage-account" var="paginationUrl">
    <c:param name="action" value="list" />
    <c:if test="${not empty param.role}">   <c:param name="role"   value="${param.role}" />   </c:if>
    <c:if test="${not empty param.status}"> <c:param name="status" value="${param.status}" /> </c:if>
    <c:if test="${not empty param.search}"> <c:param name="search" value="${param.search}" /> </c:if>
</c:url>

<nav aria-label="Page navigation">
    <ul class="pagination justify-content-center">
        <c:if test="${currentPage > 1}">
            <li class="page-item">
                <a class="page-link" href="${paginationUrl}&page=${currentPage - 1}">&laquo;</a>
            </li>
        </c:if>
        <c:forEach begin="1" end="${totalPages}" var="i">
            <li class="page-item ${currentPage == i ? 'active' : ''}">
                <a class="page-link" href="${paginationUrl}&page=${i}">${i}</a>
            </li>
        </c:forEach>
        <c:if test="${currentPage < totalPages}">
            <li class="page-item">
                <a class="page-link" href="${paginationUrl}&page=${currentPage + 1}">&raquo;</a>
            </li>
        </c:if>
    </ul>
</nav>
```

## Servlet side

```java
private void handleListWithFilters(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    String roleFilter = req.getParameter("role");
    // ...

    int page = 1, pageSize = 10;
    String pageStr = req.getParameter("page");
    if (pageStr != null && !pageStr.isEmpty()) {
        try { page = Math.max(1, Integer.parseInt(pageStr)); }
        catch (NumberFormatException e) { page = 1; }
    }

    AccountDAO dao = new AccountDAO();
    List<Account> accounts = dao.findAccountsWithFilters(roleFilter, ..., page, pageSize);
    int total = dao.getTotalFilteredAccounts(roleFilter, ...);
    int totalPages = (int) Math.ceil((double) total / pageSize);

    req.setAttribute("accounts", accounts);
    req.setAttribute("currentPage", page);
    req.setAttribute("totalPages", totalPages);
    req.setAttribute("roleFilter", roleFilter);   // re-render filter state
    req.getRequestDispatcher("/WEB-INF/views/manage-account.jsp").forward(req, resp);
}
```

---

# 31. JSP Rules (CursorRULE)

- **Bootstrap** for layout/components
- **JSTL** (`jakarta.tags.core`, `jakarta.tags.fmt`, `jakarta.tags.functions`) for control flow
- **EL** (`${...}`) for values
- **Never put `${...}` inside inline `<script>` blocks** — JSP EL and JS
  template syntax collide. Use **string concatenation** instead:

```jsp
<%-- BAD — ${} inside JS gets evaluated by JSP, not browser --%>
<script>
    var url = `${ctx}/api/items/${id}`;
</script>

<%-- GOOD — classic concatenation, EL is safe because it's inside a string literal --%>
<script>
    var url = '${pageContext.request.contextPath}/api/items/' + itemId;
    function confirmDeactivate(id) {
        if (confirm('Deactivate this account?')) {
            window.location.href = '${pageContext.request.contextPath}/manage-account?action=deactivate&id=' + id;
        }
    }
</script>
```

- Page-specific JSPs under `WEB-INF/views/`; shared fragments under
  `WEB-INF/jspf/` (`header.jspf`, `footer.jspf`, `product-card.jspf`).
- JSPs under `WEB-INF/` are reachable ONLY via `RequestDispatcher.forward`
  from a servlet — never linked directly.

---

# 32. Sensitive Data / Secrets

Generic rule: no secrets in code. **Project exception:** `DBContext.java`
holds DB credentials per CursorRULE — acknowledged, do not refactor without
team approval. Never commit `.env`, API keys, JWT secrets, OAuth client
secrets, payment credentials anywhere else.

# 33. Logging

- `java.util.logging.Logger` for DAO/service errors.
- `getServletContext().log(...)` in servlets/listeners.
- Levels: `INFO` significant events, `WARNING` recoverable, `SEVERE` failures.
- Never `System.out.println` in committed code (except inside `DBContext.main`
  for connection self-test).
- Do not log passwords, tokens, or full credentials.

# 34. Comments

Explain **why**, not what. No `// increment counter`. No commented-out dead
code — git already keeps history.

# 35. TODO

Specific and bounded: `// TODO: replace local cache when shared cache lands`.
No TODOs for functionality the current task must deliver.

# 36–38. Size / Responsibility / Duplication

One clear purpose per function and per class. No god-classes mixing HTTP+SQL+
business+UI. Before copying code, decide whether to reuse, extract, or accept
small local duplication. No generic utils for 1–2 trivial repeated lines.

# 39. Utilities

`util/` holds generic technical helpers (`JsonUtil`). No business workflows in
utility classes.

# 40. Configuration

Outside `DBContext` (the CursorRULE exception), environment-dependent values
belong in config, not code. Do not silently default production-sensitive
values.

# 41–43. External / Async

Wrap external systems behind a dedicated client/service. Controllers never
call third-party SDKs directly. Async only when there's a real need — no
queues/schedulers "just in case." AI forecast + Google auth are the only
approved future integrations.

# 44. Date and Time

Entity fields use `java.sql.Date` / `java.sql.Timestamp` (CursorRULE).
Display formatting in JSP via `<fmt:formatDate>`.

# 45. State Changes

Finite state transitions validated in service layer. No arbitrary status
assignment from request input. Terminal states do not move backward unless
explicitly permitted.

# 46. Null Handling

Validate required values early. Return empty collections (`List.of()`,
`new ArrayList<>()`), never `null` collections.

# 47–49. Frontend

Shared JSP fragments for layout; keep page-specific logic near the page.
API calls from JSP-side JS go through one `fetch` helper — no scattered raw
calls. Reuse established layouts, buttons, table styles, empty states.

# 50–52. UI Errors / Loading

User-visible feedback for failed actions — never silent. Distinguish field /
business / permission / not-found / network errors where useful. Prevent
duplicate submissions on async actions (disable button while processing).

# 53–59. Testing

- Arrange / Act / Assert.
- Names describe behavior: `shouldReturnForbiddenWhenUserLacksPermission()`.
- No dependence on system time, randomness, internet, real external services,
  or test order.
- Mock DAOs for service tests; mock services for servlet tests.
- Integration tests verify boundaries (Servlet→Service→DAO→DB) — not every
  unit scenario.

# 60–65. Refactor / Compat / Files / Deletes

- No unrelated large refactors bundled with feature work.
- Check usages before changing public contracts (API shape, shared DTO,
  entity field, URL pattern).
- Before creating a file: check equivalent exists, right module, right name.
- Before deleting: search references, tests, routes, build scripts, JSP
  includes.
- Keep diffs focused — no formatting churn on unrelated files.

# 66–68. Format / Imports / Dead Code

- Remove unused imports.
- No wildcard imports.
- No obsolete duplicate implementations left behind.
- No commented-out old code.

# 69. Documentation

Update `README.md` + `plans/<plan>/phase-status.md` when public behavior,
setup, or DB schema changes. No full requirements duplicated in code comments.

# 70. Dependencies

Before adding a jar: check equivalent exists, verify necessity, prefer stable
versions compatible with Jakarta EE 10. Do not add unused dependencies.

# 71. Build

After significant changes, run NetBeans **Clean & Build**. Do not knowingly
leave compile errors. AI must not claim "build passes" unless it ran.

# 72. Lint

Fix errors introduced by the change. Do not expand scope to every pre-existing
warning.

# 73. Error Messages

Clear, actionable, safe. Never expose SQL, stack traces, internal class
names, secrets, or provider credentials to end users.

# 74–76. Idempotency / Concurrency / DB Constraints

- Consider duplicate side effects for retryable ops.
- Use DB constraints (unique, FK, check) for stable invariants — do not rely
  solely on frontend validation.

# 77–79. Versioning / Flags / Mock Data

- No new API version unless required.
- No feature flags without need.
- No fake data wired into services/servlets to fake the page — if DB is
  empty, render the empty state, don't invent records.

# 80. Review Checklist

- [ ] Task fully addressed
- [ ] No unrelated behavior changed
- [ ] Naming consistent
- [ ] Layer boundaries respected
- [ ] No duplicate implementations
- [ ] Validation in the right layer
- [ ] Auth enforced where required
- [ ] Error handling consistent
- [ ] Transactions where atomic writes needed
- [ ] No secrets exposed
- [ ] Tests added/updated where practical
- [ ] Build/lint run
- [ ] No debug output / commented code
- [ ] Imports clean
- [ ] Docs updated if public behavior changed

# 81. AI Prohibited

- Invent features/requirements
- Rewrite architecture unasked
- Unnecessary enterprise patterns
- Libraries to avoid writing simple code
- Duplicate existing services/components
- Bypass validation/security to pass a test
- Disable failing tests
- Catch-and-ignore exceptions
- Hard-code secrets **outside the DBContext exception**
- Fake success responses
- Claim tests/build ran without running them
- Rename many files for style alone
- Replace working code wholesale when a focused fix suffices
- Speculative "future-proof" infrastructure
- Hidden fallbacks that change requirements

# 82. Ambiguity

Minor + project pattern resolves it → follow pattern.
Affects business/persisted data/auth/API/destructive ops → ask or state the
assumption before proceeding. Personal preference is not a requirement.

# 83. Poor Existing Code

Preserve public behavior, implement the change cleanly, refactor only the
minimum necessary, note larger tech debt separately.

# 84. Change Summary

After coding, briefly state: what changed, important files, checks run,
unresolved issues. No file-by-file narration unless asked.

# 85. Rule Maintenance

This file changes only when the team intentionally changes project-wide
implementation standards. Feature rules belong in requirements docs.

# 86. Final Principle

**Consistency > personal preference.** For every task:

```
Understand project → Read requirements → Follow architecture
→ Follow rule.md → Smallest correct change → Validate → Leave cleaner
```
