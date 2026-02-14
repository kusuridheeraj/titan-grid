import { TrafficChart } from "./TrafficChart";
import { RateLimitRules } from "./RateLimitRules";

export function AegisDashboard() {
    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold tracking-tight">Aegis Gatekeeper</h2>
                    <p className="text-muted-foreground">Distributed Rate Limiting & Traffic Control</p>
                </div>
            </div>

            <TrafficChart />
            <RateLimitRules />
        </div>
    );
}
