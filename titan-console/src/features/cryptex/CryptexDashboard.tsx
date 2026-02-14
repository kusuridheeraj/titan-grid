import { FileUpload } from "./FileUpload";
import { FileList } from "./FileList";

export function CryptexDashboard() {
    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold tracking-tight">Cryptex Vault</h2>
                    <p className="text-muted-foreground">Zero-Trust Secure Storage</p>
                </div>
            </div>

            <FileUpload />

            <div className="space-y-2">
                <h3 className="text-lg font-medium">Encrypted Files</h3>
                <FileList />
            </div>
        </div>
    );
}
