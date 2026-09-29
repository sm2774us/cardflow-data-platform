import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from '@tanstack/react-router';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { router } from './router';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 2_000, retry: 2, retryDelay: (n) => Math.min(1000 * 2 ** n, 8000), refetchOnWindowFocus: false } },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>,
);
