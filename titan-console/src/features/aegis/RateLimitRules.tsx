import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MoreHorizontal, Plus, Settings } from "lucide-react";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";

const initialRules = [
    { id: 1, pattern: "/api/test/limited", limit: 100, window: "60s", type: "IP", status: "Active" },
    { id: 2, pattern: "/api/test/strict", limit: 10, window: "60s", type: "IP", status: "Active" },
    { id: 3, pattern: "/api/admin/*", limit: 50, window: "300s", type: "TOKEN", status: "Active" },
    { id: 4, pattern: "/api/public/*", limit: 1000, window: "60s", type: "IP", status: "Monitoring" },
];

export function RateLimitRules() {
    return (
        <Card className="bg-card/40 backdrop-blur-sm border-border/50">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
                <div className="space-y-1">
                    <CardTitle className="text-lg font-medium flex items-center gap-2">
                        <Settings className="w-5 h-5 text-purple-500" />
                        Rate Limit Rules
                    </CardTitle>
                    <p className="text-xs text-muted-foreground">Configure dynamic rate limits per endpoint</p>
                </div>
                <Button size="sm" className="bg-primary/20 text-primary hover:bg-primary/30 border-primary/20">
                    <Plus className="w-4 h-4 mr-2" />
                    New Rule
                </Button>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Endpoint Pattern</TableHead>
                            <TableHead>Limit</TableHead>
                            <TableHead>Window</TableHead>
                            <TableHead>Type</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead className="text-right">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {initialRules.map((rule) => (
                            <TableRow key={rule.id}>
                                <TableCell className="font-mono text-xs">{rule.pattern}</TableCell>
                                <TableCell>{rule.limit}</TableCell>
                                <TableCell>{rule.window}</TableCell>
                                <TableCell>
                                    <Badge variant="outline" className="text-xs font-normal">{rule.type}</Badge>
                                </TableCell>
                                <TableCell>
                                    <Badge
                                        variant="secondary"
                                        className={rule.status === 'Active' ? 'bg-green-500/10 text-green-500' : 'bg-yellow-500/10 text-yellow-500'}
                                    >
                                        {rule.status}
                                    </Badge>
                                </TableCell>
                                <TableCell className="text-right">
                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button variant="ghost" className="h-8 w-8 p-0">
                                                <span className="sr-only">Open menu</span>
                                                <MoreHorizontal className="h-4 w-4" />
                                            </Button>
                                        </DropdownMenuTrigger>
                                        <DropdownMenuContent align="end">
                                            <DropdownMenuItem>Edit Rule</DropdownMenuItem>
                                            <DropdownMenuItem className="text-red-500">Delete Rule</DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    );
}
