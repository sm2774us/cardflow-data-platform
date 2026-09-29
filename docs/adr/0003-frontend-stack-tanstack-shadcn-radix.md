# ADR-0003: Frontend stack — React 18 + TanStack (Query/Table/Router) + shadcn-style/Radix + Zustand + Motion
**Status:** Accepted

## Decision
* **Server state:** TanStack Query (caching, polling, retries). **Client state:** Zustand (theme, language) only.
* **Routing:** TanStack Router with typed search params (transaction filters live in the URL, so views are shareable).
* **Tables:** TanStack Table (headless) rendered with owned shadcn-style components.
* **UI:** Radix primitives (dialog, tabs, slot) + Tailwind + class-variance-authority; components are copied into `src/components/ui` and owned by the repo.
* **Motion** for restrained transitions; **Recharts** for charts; SSE hook for the live stream.
* React **18.3** and TypeScript **5.6** are pinned per project requirements.

## Consequences
+ Accessible by default, no heavyweight design-system lock-in, strict type-safety end to end.
− More code to own than an off-the-shelf kit; i18n strings are draft-quality and need native review.
