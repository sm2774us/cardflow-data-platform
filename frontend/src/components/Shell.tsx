import { Link, Outlet } from '@tanstack/react-router';
import { Activity, Boxes, Gauge, Moon, ShieldCheck, Sun, Table2, Waves } from 'lucide-react';
import { useEffect } from 'react';
import { Select } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { useT, type Lang, type MessageKey } from '@/lib/i18n';
import { useUiStore } from '@/lib/store';

const nav: { to: string; key: MessageKey; icon: typeof Gauge }[] = [
  { to: '/', key: 'overview', icon: Gauge },
  { to: '/live', key: 'live', icon: Waves },
  { to: '/transactions', key: 'transactions', icon: Table2 },
  { to: '/quality', key: 'quality', icon: ShieldCheck },
  { to: '/authorize', key: 'authorize', icon: Activity },
  { to: '/architecture', key: 'architecture', icon: Boxes },
];

export function Shell() {
  const t = useT();
  const { theme, toggleTheme, lang, setLang } = useUiStore();
  useEffect(() => { document.documentElement.classList.toggle('dark', theme === 'dark'); }, [theme]);

  return (
    <div className="flex min-h-screen">
      <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r bg-card p-4 md:flex">
        <div className="mb-6 flex items-center gap-2 px-2">
          <div className="grid h-8 w-8 place-items-center rounded-lg bg-primary text-primary-foreground"><Waves className="h-4 w-4" /></div>
          <div><p className="text-sm font-semibold leading-none">CardFlow</p><p className="mt-1 text-[10px] text-muted-foreground">{t('subtitle')}</p></div>
        </div>
        <nav className="flex flex-1 flex-col gap-1" aria-label="Primary">
          {nav.map(({ to, key, icon: Icon }) => (
            <Link key={to} to={to} activeOptions={{ exact: to === '/' }}
              className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
              activeProps={{ className: 'bg-accent text-accent-foreground font-medium' }}>
              <Icon className="h-4 w-4" />{t(key)}
            </Link>
          ))}
        </nav>
        <div className="flex items-center gap-2">
          <Select aria-label="Language" value={lang} onChange={(e) => setLang(e.target.value as Lang)} className="h-8 text-xs">
            <option value="en">English</option><option value="es">Español</option><option value="pt">Português</option>
          </Select>
          <Button variant="outline" size="icon" className="h-8 w-8 shrink-0" onClick={toggleTheme} aria-label="Toggle theme">
            {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </Button>
        </div>
      </aside>
      <main className="min-w-0 flex-1 p-4 md:p-8">
        <nav className="mb-4 flex gap-1 overflow-x-auto md:hidden" aria-label="Mobile">
          {nav.map(({ to, key }) => (
            <Link key={to} to={to} activeOptions={{ exact: to === '/' }} className="whitespace-nowrap rounded-md px-3 py-1.5 text-xs text-muted-foreground"
              activeProps={{ className: 'bg-accent text-accent-foreground' }}>{t(key)}</Link>
          ))}
        </nav>
        <Outlet />
      </main>
    </div>
  );
}
