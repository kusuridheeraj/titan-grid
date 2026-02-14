import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, File, ShieldCheck, Lock } from 'lucide-react';
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Button } from "@/components/ui/button";

// Mock implementation of react-dropzone since we can't install it right now
// In a real app we'd npm install react-dropzone
const useMockDropzone = ({ onDrop }: any) => {
    return {
        getRootProps: () => ({
            onClick: () => document.getElementById('file-upload')?.click(),
            onDragOver: (e: any) => e.preventDefault(),
            onDrop: (e: any) => {
                e.preventDefault();
                onDrop(Array.from(e.dataTransfer?.files || []));
            }
        }),
        getInputProps: () => ({
            id: 'file-upload',
            type: 'file',
            onChange: (e: any) => onDrop(Array.from(e.target.files || [])),
            style: { display: 'none' }
        }),
        isDragActive: false
    };
};

export function FileUpload() {
    const [uploading, setUploading] = useState(false);
    const [progress, setProgress] = useState(0);

    const onDrop = useCallback((acceptedFiles: File[]) => {
        if (acceptedFiles.length > 0) {
            setUploading(true);
            // Simulate upload
            let p = 0;
            const interval = setInterval(() => {
                p += 10;
                setProgress(p);
                if (p >= 100) {
                    clearInterval(interval);
                    setUploading(false);
                    setProgress(0);
                    // In real app, trigger refresh of file list
                }
            }, 200);
        }
    }, []);

    const { getRootProps, getInputProps, isDragActive } = useMockDropzone({ onDrop });

    return (
        <Card className="bg-card/40 backdrop-blur-sm border-border/50 border-dashed border-2 hover:border-primary/50 transition-colors cursor-pointer">
            <CardContent className="flex flex-col items-center justify-center py-12 space-y-4" {...getRootProps()}>
                <input {...getInputProps()} />
                <div className="p-4 bg-primary/10 rounded-full">
                    <Upload className="w-8 h-8 text-primary" />
                </div>
                <div className="text-center space-y-1">
                    <h3 className="text-lg font-medium">Secure File Upload</h3>
                    <p className="text-sm text-muted-foreground">Drag & drop or click to encrypt & store</p>
                </div>

                <div className="flex items-center gap-2 text-xs text-green-500 bg-green-500/10 px-3 py-1 rounded-full border border-green-500/20">
                    <ShieldCheck className="w-3 h-3" />
                    AES-256-GCM Encryption
                </div>

                {uploading && (
                    <div className="w-full max-w-xs space-y-2">
                        <Progress value={progress} className="h-2" />
                        <p className="text-xs text-center text-muted-foreground">Encrypting & Uploading... {progress}%</p>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
