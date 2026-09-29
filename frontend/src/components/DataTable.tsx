import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from '@tanstack/react-table';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { TBody, TD, TH, THead, TR, Table } from '@/components/ui/table';

interface Props<T> {
  data: T[]; columns: ColumnDef<T, unknown>[];
  total: number; page: number; size: number; onPageChange: (p: number) => void;
  loading?: boolean; empty?: string;
}

/** Headless TanStack Table with server-side pagination — the browser never holds more than one page. */
export function DataTable<T>({ data, columns, total, page, size, onPageChange, loading, empty = 'No results' }: Props<T>) {
  const pageCount = Math.max(1, Math.ceil(total / size));
  const table = useReactTable({ data, columns, getCoreRowModel: getCoreRowModel(), manualPagination: true, pageCount });
  return (
    <div>
      <Table>
        <THead>
          {table.getHeaderGroups().map((hg) => (
            <TR key={hg.id} className="hover:bg-transparent">
              {hg.headers.map((h) => <TH key={h.id}>{flexRender(h.column.columnDef.header, h.getContext())}</TH>)}
            </TR>
          ))}
        </THead>
        <TBody className={loading ? 'opacity-50 transition-opacity' : 'transition-opacity'}>
          {table.getRowModel().rows.length === 0 && (
            <TR><TD colSpan={columns.length} className="py-10 text-center text-muted-foreground">{empty}</TD></TR>
          )}
          {table.getRowModel().rows.map((r) => (
            <TR key={r.id}>{r.getVisibleCells().map((c) => <TD key={c.id}>{flexRender(c.column.columnDef.cell, c.getContext())}</TD>)}</TR>
          ))}
        </TBody>
      </Table>
      <div className="flex items-center justify-between border-t px-3 py-2 text-xs text-muted-foreground">
        <span>{total.toLocaleString()} rows · page {page + 1} of {pageCount}</span>
        <div className="flex gap-1">
          <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)} aria-label="Previous page"><ChevronLeft className="h-4 w-4" /></Button>
          <Button variant="outline" size="sm" disabled={page + 1 >= pageCount} onClick={() => onPageChange(page + 1)} aria-label="Next page"><ChevronRight className="h-4 w-4" /></Button>
        </div>
      </div>
    </div>
  );
}
