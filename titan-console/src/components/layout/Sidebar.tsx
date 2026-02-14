import { Shield, Lock, Cpu, LayoutDashboard, Terminal, Activity } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

interface SidebarProps {
    activeTab: string;
    onTabChange: (tab: string) => void;
}

export function Sidebar({ activeTab, onTabChange }: SidebarProps) {
    const navItems = [
        { id: "dashboard", label: "Overview", icon: LayoutDashboard },
        { id: "aegis", label: "Aegis Gatekeeper", icon: Shield },
        { id: "cryptex", label: "Cryptex Vault", icon: Lock },
        { id: "nexus", label: "Nexus Operator", icon: Cpu },
    ];

    return (
        <div className="w-64 border-r bg-card/50 backdrop-blur-xl h-screen flex flex-col">
            <div className="p-6 border-b border-border/50">
                <h1 className="text-xl font-bold bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent flex items-center gap-2">
                    <Activity className="w-6 h-6 text-blue-500" />
                    TITAN GRID
                </h1>
                <p className="text-xs text-muted-foreground mt-1">System Control</p>
            </div>

            <nav className="flex-1 p-4 space-y-2">
                {navItems.map((item) => (
                    <Button
                        key={item.id}
                        variant={activeTab === item.id ? "secondary" : "ghost"}
                        className={cn(
                            "w-full justify-start gap-3 transition-all duration-200",
                            activeTab === item.id ? "bg-primary/10 text-primary hover:bg-primary/20" : "text-muted-foreground"
                        )}
                        onClick={() => onTabChange(item.id)}
                    >
                        <item.icon className="w-4 h-4" />
                        {item.label}
                    </Button>
                ))}
            </nav>

            <div className="p-4 border-t border-border/50">
                <div className="bg-muted/50 rounded-lg p-3 text-xs space-y-2">
                    <div className="flex justify-between">
                        <span className="text-muted-foreground">Redis</span>
                        <span className="text-green-500 font-medium">● Online</span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-muted-foreground">Postgres</span>
                        <span className="text-green-500 font-medium">● Online</span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-muted-foreground">Vault</span>
                        <span className="text-green-500 font-medium">● Online</span>
                    </div>
                </div>
            </div>
        </div>
    );
}
