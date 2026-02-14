import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Activity, ShieldAlert, FileKey, Cpu } from "lucide-react";

export function Overview() {
    const stats = [
        { title: "Total Requests", value: "1.2M", change: "+12%", icon: Activity, color: "text-blue-500" },
        { title: "Threats Blocked", value: "423", change: "+5%", icon: ShieldAlert, color: "text-red-500" },
        { title: "Files Encrypted", value: "892", change: "+24%", icon: FileKey, color: "text-amber-500" },
        { title: "AI Actions", value: "56", change: "+8%", icon: Cpu, color: "text-purple-500" },
    ];

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {stats.map((stat, i) => (
                    <Card key={i} className="bg-card/40 backdrop-blur-sm border-border/50">
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium text-muted-foreground">
                                {stat.title}
                            </CardTitle>
                            <stat.icon className={`h-4 w-4 ${stat.color}`} />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">{stat.value}</div>
                            <p className="text-xs text-muted-foreground mt-1">
                                <span className="text-green-500 font-medium">{stat.change}</span> from last month
                            </p>
                        </CardContent>
                    </Card>
                ))}
            </div>

            {/* Placeholder for more content */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <Card className="col-span-2 bg-card/40 border-border/50 h-[400px] flex items-center justify-center text-muted-foreground">
                    Real-time Traffic Chart (Coming Soon)
                </Card>
                <Card className="bg-card/40 border-border/50 h-[400px] flex items-center justify-center text-muted-foreground">
                    Latest Alerts (Coming Soon)
                </Card>
            </div>
        </div>
    );
}
