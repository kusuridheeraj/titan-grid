import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Download, Trash2, FileLock, Key } from "lucide-react";
import { Badge } from "@/components/ui/badge";

const files = [
    { id: 1, name: "secret-plans.pdf", size: "2.4 MB", date: "2024-02-14 10:30", keyId: "vault-key-01" },
    { id: 2, name: "customer-db-dump.sql", size: "450 MB", date: "2024-02-13 16:45", keyId: "vault-key-02" },
    { id: 3, name: "identity-documents.zip", size: "128 MB", date: "2024-02-12 09:15", keyId: "vault-key-03" },
];

export function FileList() {
    return (
        <div className="rounded-md border border-border/50 bg-card/40 backdrop-blur-sm">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead className="w-[300px]">File Name</TableHead>
                        <TableHead>Size</TableHead>
                        <TableHead>Uploaded</TableHead>
                        <TableHead>Encryption Key</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {files.map((file) => (
                        <TableRow key={file.id}>
                            <TableCell className="font-medium flex items-center gap-2">
                                <FileLock className="w-4 h-4 text-amber-500" />
                                {file.name}
                            </TableCell>
                            <TableCell>{file.size}</TableCell>
                            <TableCell>{file.date}</TableCell>
                            <TableCell>
                                <Badge variant="outline" className="font-mono text-xs flex w-fit items-center gap-1 border-purple-500/30 text-purple-500 bg-purple-500/5">
                                    <Key className="w-3 h-3" />
                                    {file.keyId}
                                </Badge>
                            </TableCell>
                            <TableCell className="text-right">
                                <div className="flex justify-end gap-2">
                                    <Button variant="ghost" size="icon" className="h-8 w-8 text-blue-500 hover:text-blue-600 hover:bg-blue-500/10">
                                        <Download className="w-4 h-4" />
                                    </Button>
                                    <Button variant="ghost" size="icon" className="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-500/10">
                                        <Trash2 className="w-4 h-4" />
                                    </Button>
                                </div>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
}
