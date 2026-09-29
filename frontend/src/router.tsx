import { createRootRoute, createRoute, createRouter } from '@tanstack/react-router';
import { Shell } from '@/components/Shell';
import { ArchitecturePage } from '@/pages/ArchitecturePage';
import { AuthorizePage } from '@/pages/AuthorizePage';
import { LivePage } from '@/pages/LivePage';
import { OverviewPage } from '@/pages/OverviewPage';
import { QualityPage } from '@/pages/QualityPage';
import { TransactionsPage } from '@/pages/TransactionsPage';

const rootRoute = createRootRoute({ component: Shell });
const child = (path: string, component: () => JSX.Element) => createRoute({ getParentRoute: () => rootRoute, path, component });

const transactionsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/transactions',
  component: TransactionsPage,
  validateSearch: (s: Record<string, unknown>) => ({
    page: Number.isInteger(Number(s.page)) && Number(s.page) >= 0 ? Number(s.page) : 0,
    decision: typeof s.decision === 'string' && s.decision ? s.decision : undefined,
    q: typeof s.q === 'string' && s.q ? s.q : undefined,
  }),
});

const routeTree = rootRoute.addChildren([
  child('/', OverviewPage), child('/live', LivePage), transactionsRoute,
  child('/quality', QualityPage), child('/authorize', AuthorizePage), child('/architecture', ArchitecturePage),
]);

export const router = createRouter({ routeTree, defaultPreload: 'intent' });

declare module '@tanstack/react-router' {
  interface Register { router: typeof router }
}
