import { useState, useEffect } from 'react';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Activity } from 'lucide-react';

const generateData = () => {
    const data = [];
    const now = new Date();
    for (let i = 20; i >= 0; i--) {
        const time = new Date(now.getTime() - i * 1000);
        data.push({
            time: time.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' }),
            allowed: Math.floor(Math.random() * 500) + 200,
            blocked: Math.floor(Math.random() * 50),
        });
    }
    return data;
};

export function TrafficChart() {
    const [data, setData] = useState(generateData());

    useEffect(() => {
        const interval = setInterval(() => {
            setData(current => {
                const now = new Date();
                const newPoint = {
                    time: now.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' }),
                    allowed: Math.floor(Math.random() * 500) + 200,
                    blocked: Math.floor(Math.random() * 50) + (Math.random() > 0.9 ? 100 : 0), // Occasional spike
                };
                return [...current.slice(1), newPoint];
            });
        }, 1000);

        return () => clearInterval(interval);
    }, []);

    return (
        <Card className="bg-card/40 backdrop-blur-sm border-border/50">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
                <div className="space-y-1">
                    <CardTitle className="text-lg font-medium flex items-center gap-2">
                        <Activity className="w-5 h-5 text-blue-500" />
                        Live Traffic Monitor
                    </CardTitle>
                    <p className="text-xs text-muted-foreground">Real-time request throughput (1s granularity)</p>
                </div>
                <div className="flex gap-2">
                    <Badge variant="outline" className="border-green-500/50 text-green-500 bg-green-500/10">Allowed</Badge>
                    <Badge variant="outline" className="border-red-500/50 text-red-500 bg-red-500/10">Blocked</Badge>
                </div>
            </CardHeader>
            <CardContent>
                <div className="h-[300px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={data}>
                            <defs>
                                <linearGradient id="colorAllowed" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#22c55e" stopOpacity={0.3} />
                                    <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
                                </linearGradient>
                                <linearGradient id="colorBlocked" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                                    <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
                            <XAxis
                                dataKey="time"
                                stroke="hsl(var(--muted-foreground))"
                                fontSize={12}
                                tickLine={false}
                                axisLine={false}
                            />
                            <YAxis
                                stroke="hsl(var(--muted-foreground))"
                                fontSize={12}
                                tickLine={false}
                                axisLine={false}
                                tickFormatter={(value) => `${value}`}
                            />
                            <Tooltip
                                contentStyle={{
                                    backgroundColor: 'hsl(var(--card))',
                                    borderColor: 'hsl(var(--border))',
                                    borderRadius: 'var(--radius)'
                                }}
                                itemStyle={{ color: 'hsl(var(--foreground))' }}
                            />
                            <Area
                                type="monotone"
                                dataKey="allowed"
                                stroke="#22c55e"
                                strokeWidth={2}
                                fillOpacity={1}
                                fill="url(#colorAllowed)"
                                isAnimationActive={false}
                            />
                            <Area
                                type="monotone"
                                dataKey="blocked"
                                stroke="#ef4444"
                                strokeWidth={2}
                                fillOpacity={1}
                                fill="url(#colorBlocked)"
                                isAnimationActive={false}
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>
            </CardContent>
        </Card>
    );
}
