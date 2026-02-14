import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertTriangle, CheckCircle, XCircle } from "lucide-react";

interface ApprovalCardProps {
    id: string;
    action: string;
    params: string;
    onApprove: (id: string) => void;
    onDeny: (id: string) => void;
}

export function HumanApprovalCard({ id, action, params, onApprove, onDeny }: ApprovalCardProps) {
    return (
        <Card className="border-yellow-500/50 bg-yellow-500/5 overflow-hidden animate-in zoom-in-95 duration-300">
            <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-yellow-500 flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4" />
                    Approval Required
                </CardTitle>
            </CardHeader>
            <CardContent className="text-sm space-y-2">
                <p className="font-medium">Nexus wants to execute:</p>
                <div className="bg-background/50 p-2 rounded-md font-mono text-xs border border-border/50">
                    {action}({params})
                </div>
                <p className="text-muted-foreground text-xs">
                    Ref ID: <span className="font-mono">{id}</span>
                </p>
            </CardContent>
            <CardFooter className="flex gap-2 justify-end bg-yellow-500/10 p-2">
                <Button
                    size="sm"
                    variant="ghost"
                    className="hover:bg-red-500/20 hover:text-red-500 h-7 text-xs"
                    onClick={() => onDeny(id)}
                >
                    <XCircle className="w-3 h-3 mr-1" />
                    Deny
                </Button>
                <Button
                    size="sm"
                    className="bg-green-500 hover:bg-green-600 text-white h-7 text-xs"
                    onClick={() => onApprove(id)}
                >
                    <CheckCircle className="w-3 h-3 mr-1" />
                    Approve
                </Button>
            </CardFooter>
        </Card>
    );
}
