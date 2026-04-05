import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { Skeleton } from '@/components/ui/skeleton';
import { formatCurrency, formatShortDate } from '@/lib/formatters';
import type { PriceHistory } from '@/types/ticket';

interface PriceHistoryChartProps {
  data: PriceHistory[];
  isLoading?: boolean;
}

interface ChartDataPoint {
  date: string;
  price: number;
  multiplier: number;
  supplyRatio: number;
  daysToEvent: number;
}

interface TooltipPayloadEntry {
  value: number;
  payload: ChartDataPoint;
}

interface CustomTooltipProps {
  active?: boolean;
  payload?: TooltipPayloadEntry[];
  label?: string;
}

function CustomTooltip({ active, payload }: Readonly<CustomTooltipProps>) {
  if (!active || !payload || payload.length === 0) {
    return null;
  }

  const data = payload[0].payload;

  return (
    <div className="rounded-lg border bg-background p-3 shadow-lg">
      <p className="text-sm font-medium">{data.date}</p>
      <p className="mt-1 text-sm">
        <span className="text-muted-foreground">Price: </span>
        <span className="font-semibold">{formatCurrency(data.price)}</span>
      </p>
      <p className="text-xs text-muted-foreground">Multiplier: {data.multiplier.toFixed(2)}x</p>
      <p className="text-xs text-muted-foreground">
        Supply: {(data.supplyRatio * 100).toFixed(0)}%
      </p>
      <p className="text-xs text-muted-foreground">
        {data.daysToEvent} day{data.daysToEvent !== 1 ? 's' : ''} to event
      </p>
    </div>
  );
}

/**
 * Line chart showing price trends over time for an event.
 * Uses Recharts for the visualization. Responsive to container width.
 */
export function PriceHistoryChart({ data, isLoading }: Readonly<PriceHistoryChartProps>) {
  if (isLoading) {
    return <Skeleton className="h-[300px] w-full rounded-lg" />;
  }

  if (data.length === 0) {
    return (
      <div className="flex h-[300px] flex-col items-center justify-center rounded-lg border border-dashed p-8 text-center">
        <p className="text-lg font-medium text-muted-foreground">No price history</p>
        <p className="mt-1 text-sm text-muted-foreground">
          Price data will appear as tickets are bought and sold.
        </p>
      </div>
    );
  }

  const chartData: ChartDataPoint[] = [...data]
    .sort((a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime())
    .map((entry) => ({
      date: formatShortDate(entry.recordedAt),
      price: entry.price,
      multiplier: entry.multiplier,
      supplyRatio: entry.supplyRatio,
      daysToEvent: entry.daysToEvent,
    }));

  const prices = chartData.map((d) => d.price);
  const minPrice = Math.floor(Math.min(...prices) * 0.9);
  const maxPrice = Math.ceil(Math.max(...prices) * 1.1);

  return (
    <div className="h-[300px] w-full">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
          <XAxis dataKey="date" tick={{ fontSize: 12 }} className="fill-muted-foreground" />
          <YAxis
            domain={[minPrice, maxPrice]}
            tickFormatter={(value: number) => `$${value}`}
            tick={{ fontSize: 12 }}
            className="fill-muted-foreground"
          />
          <Tooltip content={<CustomTooltip />} />
          <Line
            type="monotone"
            dataKey="price"
            stroke="hsl(var(--primary))"
            strokeWidth={2}
            dot={{ fill: 'hsl(var(--primary))', r: 3 }}
            activeDot={{ r: 5 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
