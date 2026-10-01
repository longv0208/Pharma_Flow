---
title: "Clean Medical redesign — white + green storefront & admin"
description: "Replace the Modern Apothecary (beige/pine) CSS override with a clean white + green medical palette in main.css, plus safe JSP polish."
status: pending
priority: P2
effort: 3h
branch: main
tags: [css, redesign, theme, jsp, admin]
created: 2026-10-02
---

# Clean Medical Redesign

## Goal
Swap the end-of-file **Modern Apothecary** override for a **Clean Medical** theme: white surface + green primary (medical/clean). CSS-only where possible. Keep all markup contracts (`?action=` flows, field names, error keys, class hooks) unchanged.

## File to edit
- `web/css/main.css` — the only file for the theme. Base tokens at top (lines 6-30) stay as the *fallback* layer; all changes go in a NEW override block at the end.
- Mirror to `build/web/css/main.css` after editing (exploded Tomcat deploy).
- JSPs only if a wrapper class is strictly needed — flag each one.

## Step 1 — Font + token remap (replaces lines 1095-1135)
Replace the `@import` at the top of the file (line 4) and the `:root` remap block.
- `@import` → `Figtree:wght@500;600;700` + `Noto+Sans:wght@400;500;600` (drop Fraunces/Instrument/IBM-Plex-Mono). Keep `display=swap`. Offline-safe: the `var(--font-*)` fallback stacks already degrade to system fonts, so no extra work needed.
- New tokens (healthcare palette):
  - `--green-50:#F0FDF4  --green-100:#DCFCE7  --green-200:#BBF7D0  --green-500:#22C55E  --green-600:#16A34A  --green-700:#15803D  --green-900:#14532D`
  - `--blue-700:#0369A1` (info/links accent), `--red-500:#DC2626`, `--amber-500:#D97706`
  - Neutrals → cool slate: `--gray-50:#F8FAFC --gray-100:#F1F5F9 --gray-200:#E2E8F0 --gray-300:#CBD5E1 --gray-500:#64748B --gray-700:#334155 --gray-900:#0F172A`
  - `--surface:#FFFFFF`, `--bg:#F0FDF4` (page tint), `--border:var(--green-200)`
  - `--radius:10px`, `--radius-lg:14px` (slightly rounder), softer `--shadow-sm/md` (low-opacity slate).
  - `--font-display:'Figtree',system-ui`, `--font-body:'Noto Sans',system-ui`, `--font-mono:ui-monospace,SFMono-Regular,Menlo,monospace`.
- Remap legacy `--teal-*` → `--green-*` and `--gray-*` → slate so every base rule (buttons, tables, badges, pager, forms) restyles with **zero selector edits**.
- `body` → `font-family:var(--font-body); background:var(--bg); color:var(--gray-900)`.
- Keep `h1..h3/.brand-name/...` display-font rule but point at `--font-display` (Figtree, not serif).

## Step 2 — Component polish (replaces lines 1136-1345)
Same selector list as current override, restyled. White cards on green-tint page; green is reserved for primary actions/active states (contrast).
- **Header**: `.site-header` white, thin `--border` bottom, subtle shadow. `.brand-mark` green-100 bg + green-700 icon. Nav links slate-700 → green-700 on hover. `.nav-categories-menu` white card.
- **Hero**: `linear-gradient(180deg,#fff, var(--green-50))` + soft radial `--green-100`. `.hero-eyebrow` green-700 uppercase (drop mono/brass). `.hero-offer` white card, green border, soft shadow; `.offer-off`/`.badge-sale` → green-600.
- **Buttons**: `.btn-primary` green-600→700 hover + `translateY(-1px)`; `.btn-secondary` white + green-700 text, green-300 border; `.btn-ghost` green-700, green-50 hover.
- **Cards**: `.product-card`/`.category-tile`/`.promo`/`.admin-card` white bg, `--border` border, `--radius-lg`, hover lift + `--green-200` border. Thumb tints: `otc`=green-50/green-600, `rx`=blue-50/blue-700, `restricted`=red-50/red-500.
- **Filter bar / inputs**: white bg, `--gray-300` border, focus ring `0 0 0 3px var(--green-100)` + green-500 border. Applies to `.filter-bar`, `.form-field`, `.auth-card` inputs.
- **Badges**: `.badge-instock` green-100/green-700, `.badge-rx` blue-100/blue-800, `.badge-restricted` red-100/red-800, `.badge-out-of-stock` gray-200/gray-700. `.status-active` green-100/green-800. Keep `.type-otc/rx/restricted` hue mapping.
- **Product detail**: `.pd-facts` green-50 panel, `--border`, dt uppercase green-700; `.pd-price` green-700.
- **Auth**: `.auth-page` white→green-50 radial; `.auth-card` white, `--border`, soft shadow, `--radius-lg`.
- **Profile**: `.profile-card` white card, green-700 head.
- **Admin**: `.admin-topbar`/`.admin-sidebar`/`.admin-card` white; `.admin-nav-link.current` green-50 bg + green-700 text; `.admin-nav-label` green-700; table `thead` green-50, hairline `--gray-200` rows; `.admin-layout` bg `--bg`.
- **Pager**: `.pager-num.current` green-600 white text; hover green-50.
- **Footer**: `.site-footer` green-900 bg, green-100 text — the one dark surface, anchors readable.
- **Sticky footer**: keep `body{flex}…margin-top:auto` lines verbatim at the very end.

## Step 3 — JSP tweaks (minimal, flag each)
CSS covers ~95%. Only add a class if a selector can't reach (e.g., wrap admin stat numbers, or add `badge-icon` span). Do NOT change field names, `?action=` values, or error keys. Mirror every changed JSP into `build/web/…`.

## Step 4 — Build / mirror / verify
1. Edit `web/css/main.css` (base `@import` + new override block at end).
2. `copy web/css/main.css build/web/css/main.css` (and `build/web/WEB-INF/...` for any touched JSP).
3. Hard-refresh (Ctrl+F5) storefront: home, products (+filter/pager), product-detail, login/register, profile, admin list+form.
4. Contrast check: green-700 text on white = 4.6:1 (pass); green-600 on white = 3.9:1 → use green-700 for small text/links, green-600 only for large text/fills with white text. White on green-600 = 3.3:1 — bump primary button to green-700 or keep green-600 + bold ≥14px; verify.

## Risks / notes
- **Google Fonts offline** → `@import` silently falls back to system stack; acceptable. Optional: self-host woff2 in `web/fonts/` (stretch, not required).
- **Contrast** — #15803D (green-700) is the safe small-text green on white; #22C55E only for fills/large text. Recheck badge text pairs.
- **CSS order** — override MUST stay last in file (after base `.pd-*` rules at ~1035-1094) so cascade wins; sticky-footer block stays after it.
- **No emojis** as icons anywhere; keep existing glyph/text approach.
- **build/web drift** — forgetting the mirror step makes the deploy look unchanged.

## Success criteria
- Palette reads white + green medical; no beige/pine/serif remains.
- All pages render (no broken layout); contrast ≥4.5:1 for body text.
- `build/web` mirrors `web`; zero JSP contract changes.
- `main.css` stays one file; override block self-contained and ≤ ~260 lines.
