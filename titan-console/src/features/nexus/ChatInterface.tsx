import { useState, useRef, useEffect } from "react";
import { Send, Cpu, User, Bot } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { HumanApprovalCard } from "./HumanApprovalCard";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";

interface Message {
    id: string;
    role: "user" | "assistant";
    content: string;
    approvalRequest?: {
        id: string;
        action: string;
        params: string;
    };
}

export function ChatInterface() {
    const [messages, setMessages] = useState<Message[]>([
        { id: "1", role: "assistant", content: "Nexus Operator online. Monitoring system vitals. How can I assist you?" }
    ]);
    const [input, setInput] = useState("");
    const [isTyping, setIsTyping] = useState(false);
    const scrollRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [messages]);

    const handleSend = () => {
        if (!input.trim()) return;

        const userMsg: Message = { id: Date.now().toString(), role: "user", content: input };
        setMessages(prev => [...prev, userMsg]);
        setInput("");
        setIsTyping(true);

        // Simulate AI response
        setTimeout(() => {
            setIsTyping(false);
            const isDangerous = userMsg.content.toLowerCase().includes("ban") || userMsg.content.toLowerCase().includes("delete");

            let aiMsg: Message;
            if (isDangerous) {
                aiMsg = {
                    id: (Date.now() + 1).toString(),
                    role: "assistant",
                    content: "I can proceed with that, but this action requires authorization.",
                    approvalRequest: {
                        id: "DOC-8821",
                        action: "ban_ip",
                        params: "ip='192.168.1.55', duration='24h'"
                    }
                };
            } else {
                aiMsg = {
                    id: (Date.now() + 1).toString(),
                    role: "assistant",
                    content: "Analyzing system logs... No anomalies detected in the last hour. Efficiency is at 98%."
                };
            }
            setMessages(prev => [...prev, aiMsg]);
        }, 1500);
    };

    const handleApprove = (id: string) => {
        setMessages(prev => [...prev, {
            id: Date.now().toString(),
            role: "assistant",
            content: `Action ${id} authorized. Executing command... Done.`
        }]);
    };

    const handleDeny = (id: string) => {
        setMessages(prev => [...prev, {
            id: Date.now().toString(),
            role: "assistant",
            content: `Action ${id} denied. Operation cancelled.`
        }]);
    };

    return (
        <div className="flex flex-col h-[calc(100vh-12rem)] max-h-[800px] border border-border/50 rounded-lg bg-card/40 backdrop-blur-sm shadow-xl overflow-hidden">
            {/* Chat Header */}
            <div className="p-4 border-b border-border/50 bg-muted/20 flex items-center gap-2">
                <Cpu className="w-5 h-5 text-purple-500" />
                <h3 className="font-medium">Nexus Direct Link</h3>
                <span className="text-xs text-green-500 ml-auto flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                    Online
                </span>
            </div>

            {/* Message List */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4" ref={scrollRef}>
                {messages.map((msg) => (
                    <div key={msg.id} className={cn("flex gap-3 max-w-[80%]", msg.role === "user" ? "ml-auto flex-row-reverse" : "")}>
                        <div className={cn("w-8 h-8 rounded-full flex items-center justify-center shrink-0", msg.role === "user" ? "bg-primary text-primary-foreground" : "bg-purple-500 text-white")}>
                            {msg.role === "user" ? <User className="w-5 h-5" /> : <Bot className="w-5 h-5" />}
                        </div>

                        <div className="space-y-2">
                            <div className={cn("p-3 rounded-lg text-sm", msg.role === "user" ? "bg-primary text-primary-foreground" : "bg-muted/50 border border-border/50")}>
                                {msg.content}
                            </div>

                            {msg.approvalRequest && (
                                <HumanApprovalCard
                                    {...msg.approvalRequest}
                                    onApprove={handleApprove}
                                    onDeny={handleDeny}
                                />
                            )}
                        </div>
                    </div>
                ))}

                {isTyping && (
                    <div className="flex gap-3">
                        <div className="w-8 h-8 rounded-full bg-purple-500 text-white flex items-center justify-center shrink-0">
                            <Bot className="w-5 h-5" />
                        </div>
                        <div className="bg-muted/50 border border-border/50 p-3 rounded-lg flex items-center gap-1">
                            <span className="w-2 h-2 bg-foreground/40 rounded-full animate-bounce [animation-delay:-0.3s]" />
                            <span className="w-2 h-2 bg-foreground/40 rounded-full animate-bounce [animation-delay:-0.15s]" />
                            <span className="w-2 h-2 bg-foreground/40 rounded-full animate-bounce" />
                        </div>
                    </div>
                )}
            </div>

            {/* Input Area */}
            <div className="p-4 border-t border-border/50 bg-background/50 backdrop-blur-sm">
                <div className="flex gap-2">
                    <Input
                        placeholder="Type a command (e.g., 'Analyze traffic', 'Ban IP 1.2.3.4')..."
                        className="flex-1 bg-background/50"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && handleSend()}
                    />
                    <Button onClick={handleSend} disabled={isTyping}>
                        <Send className="w-4 h-4" />
                    </Button>
                </div>
            </div>
        </div>
    );
}
