# Pharma_Flow

Pharmacy management + online store. Plain Servlet/JSP on Tomcat 10, MySQL 8.

## Stack

- Java 17, Jakarta EE 10 — `jakarta.*` namespace
- JSP 3.x + JSTL 3.x, JDBC via MySQL Connector/J
- NetBeans Java Web project (Ant)

## Layout

```
├── lib/                           compile-only jars (NOT deployed)
│   └── jakarta.servlet-api.jar    provided by Tomcat at runtime
├── nbproject/                     NetBeans web project descriptor
├── src/java/
│   ├── controller/                HomeServlet, AuthenController,
│   │                              AdminServlet, StaffServlet, PosServlet
│   ├── dao/                       CategoryDAO, ProductDAO, UserDAO (extends DBContext)
│   ├── db/                        DBContext — edit credentials here
│   ├── model/                     Category, Product, ProductType, User
│   └── util/                      PasswordUtil (salted SHA-256)
└── web/                           webroot
    ├── WEB-INF/web.xml
    ├── WEB-INF/views/             JSPs (servlet-forwarded only)
    │   ├── home.jsp, error.jsp
    │   ├── auth/{login,register}.jsp
    │   ├── admin/dashboard.jsp    (stub)
    │   ├── staff/dashboard.jsp    (stub)
    │   └── pos/dashboard.jsp      (stub)
    ├── WEB-INF/jspf/              header, footer, product-card fragments
    ├── WEB-INF/lib/               runtime jars bundled into WAR (mysql + jstl)
    ├── css/, js/, images/
    └── index.jsp                  redirects to /home
```

## DB config

Single file: `src/java/db/DBContext.java` — edit `DB_URL` / `DB_USER` / `DB_PWD` directly.
Pattern follows the team's DBContext template (fresh `Connection` per call, driver loaded in ctor).

## Setup

1. JDK 17+, Tomcat 10+, MySQL 8.
2. Import schema: `mysql -u root -p < ../db_pharma.sql`
3. Place jars:
   - `lib/jakarta.servlet-api.jar` — copy from `$CATALINA_HOME/lib/servlet-api.jar` (compile-only, already present)
   - `web/WEB-INF/lib/mysql-connector-j.jar` — `com.mysql:mysql-connector-j:8.x`
   - `web/WEB-INF/lib/jakarta.servlet.jsp.jstl-api.jar` — `jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0`
   - `web/WEB-INF/lib/jakarta.servlet.jsp.jstl.jar` — `org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1`
4. Edit `db/DBContext.java` credentials if not root/blank.

Important: keep `jakarta.servlet-api.jar` out of `web/WEB-INF/lib/` — anything physically
in that folder is copied into the WAR, and shipping Tomcat's servlet API inside
the webapp breaks classloading.

## Build & run

Open in NetBeans → Clean & Build → produces `dist/Pharma_Flow.war` → deploy on Tomcat 10.

Open `http://localhost:8080/Pharma_Flow/`.

## Endpoints

| Path | Type | Purpose |
|------|------|---------|
| `/` | HTML | redirects to `/home` |
| `/home` | HTML | customer home page |
| `/authen?action=login` | HTML | sign-in form / POST login |
| `/authen?action=register` | HTML | register form / POST register |
| `/authen?action=logout` | — | invalidate session, redirect /home |
| `/admin` | HTML | OWNER_ADMIN landing (session-gated stub) |
| `/staff` | HTML | PHARMACIST landing (session-gated stub) |
| `/pos` | HTML | SALES_STAFF landing (session-gated stub) |

## Notes

- JSPs under `WEB-INF/views/` only reachable via servlet forward.
- Read-only catalog: no auth, no writes — no CSRF needed.
- Featured/HealthCare split is positional (first 8 by `product_id`, split 4+4) —
  schema has no "health care" flag.
- Purchasable rule: `product_type='OTC'` AND `online_sale_allowed=1` AND in stock.
  RX/RESTRICTED products render but with disabled Add-to-Cart.
- No JSON API for now — only JSP-rendered pages. Add `util/JsonUtil` when an API is needed.
- Auth: session-based, `currentUser` in `HttpSession`. Login accepts email or username.
  Public registration always forces `role=CUSTOMER` and `status=ACTIVE` server-side.
  Passwords stored as `salt:hash` (SHA-256) via `util/PasswordUtil` — never plain text.
