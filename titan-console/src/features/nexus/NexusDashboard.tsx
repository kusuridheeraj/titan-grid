import { ChatInterface } from "./ChatInterface";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Cpu, Zap } from "lucide-react";

export function NexusDashboard() {
    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-full animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="lg:col-span-2 flex flex-col h-full">
                <div className="mb-6">
                    <h2 className="text-2xl font-bold tracking-tight">Nexus Operator</h2>
                    <p className="text-muted-foreground">AI-Driven System Management</p>
                </div>
                <ChatInterface />
            </div>

            <div className="space-y-6">
                <Card className="bg-card/40 backdrop-blur-sm border-border/50">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2 text-base">
                            <Zap className="w-4 h-4 text-yellow-500" />
                            Active Recommendations
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="text-sm border-l-2 border-yellow-500 pl-4 py-1">
                            <p className="font-medium">Optimize Cache TTL</p>
                            <p className="text-muted-foreground text-xs mt-1">
                                High read variance detected on `/api/products`. Recommended TTL: 300s.
                            </p>
                            <button className="text-yellow-500 text-xs mt-2 font-medium hover:underline">Apply Fix</button>
                        </div>
                        <div className="text-sm border-l-2 border-blue-500 pl-4 py-1">
                            <p className="font-medium">Scale Down Replica</p>
                            <p className="text-muted-foreground text-xs mt-1">
                                Redis usage is below 20%. Consider removing one read replica to save cost.
                            </p>
                            <button className="text-blue-500 text-xs mt-2 font-medium hover:underline">Review Scaling</button>
                        </div>
                    </CardContent>
                </Card>

                <Card className="bg-card/40 backdrop-blur-sm border-border/50">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2 text-base">
                            <Cpu className="w-4 h-4 text-purple-500" />
                            Model Context
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2 text-xs text-muted-foreground">
                        <div className="flex justify-between">
                            <span>Model:</span>
                            <span className="font-mono text-foreground">Llama 3 (Ollama)</span>
                        </div>
                        <div className="flex justify-between">
                            <span>Latency:</span>
                            <span className="font-mono text-green-500">45ms</span>
                        </div>
                        <div className="flex justify-between">
                            <span>Context Window:</span>
                            <span className="font-mono text-foreground">4096 tokens</span>
                        </div>
                        <div className="flex justify-between">
                            <span>Tools Available:</span>
                            <span className="font-mono text-foreground">4</span>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}
