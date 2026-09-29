import * as TabsPrimitive from '@radix-ui/react-tabs';
import * as React from 'react';
import { cn } from '@/lib/utils';

export const Tabs = TabsPrimitive.Root;
export const TabsList = ({ className, ...p }: React.ComponentPropsWithoutRef<typeof TabsPrimitive.List>) => (
  <TabsPrimitive.List className={cn('inline-flex h-9 items-center rounded-lg bg-muted p-1', className)} {...p} />
);
export const TabsTrigger = ({ className, ...p }: React.ComponentPropsWithoutRef<typeof TabsPrimitive.Trigger>) => (
  <TabsPrimitive.Trigger
    className={cn('inline-flex items-center rounded-md px-3 py-1 text-sm font-medium text-muted-foreground transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow', className)}
    {...p}
  />
);
export const TabsContent = ({ className, ...p }: React.ComponentPropsWithoutRef<typeof TabsPrimitive.Content>) => (
  <TabsPrimitive.Content className={cn('mt-4 focus-visible:outline-none', className)} {...p} />
);
