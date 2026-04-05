import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { MessageCircle, Send, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useChat } from '@/hooks/use-ai';
import type { ChatMessage } from '@/types/ai';

/**
 * Renders text with markdown links converted to clickable React Router Links.
 * Handles [text](url) patterns, using internal Links for relative URLs
 * and anchor tags for external URLs.
 */
function MessageContent({ content }: Readonly<{ content: string }>) {
  const linkRegex = /\[([^\]]{1,500})\]\(([^)]{1,2000})\)/g;
  const parts: React.ReactNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = linkRegex.exec(content)) !== null) {
    // Add text before the link
    if (match.index > lastIndex) {
      parts.push(content.slice(lastIndex, match.index));
    }

    const linkText = match[1];
    const linkUrl = match[2];

    if (linkUrl.startsWith('/')) {
      parts.push(
        <Link key={match.index} to={linkUrl} className="font-medium text-primary underline">
          {linkText}
        </Link>,
      );
    } else {
      parts.push(
        <a
          key={match.index}
          href={linkUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="font-medium text-primary underline"
        >
          {linkText}
        </a>,
      );
    }

    lastIndex = match.index + match[0].length;
  }

  // Add remaining text
  if (lastIndex < content.length) {
    parts.push(content.slice(lastIndex));
  }

  return <>{parts}</>;
}

/**
 * Floating chat widget that provides AI assistant access from any page.
 * Opens a slide-out panel from the bottom-right with a conversational interface.
 */
export function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [conversationId, setConversationId] = useState<number | null>(null);
  const [aiUnavailable, setAiUnavailable] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const chatMutation = useChat();

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  useEffect(() => {
    if (isOpen) {
      // Small delay to allow panel animation to complete
      const timer = setTimeout(() => inputRef.current?.focus(), 300);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  const handleSend = useCallback(() => {
    const trimmed = inputValue.trim();
    if (trimmed.length === 0 || chatMutation.isPending) return;

    const userMessage: ChatMessage = {
      role: 'user',
      content: trimmed,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputValue('');

    chatMutation.mutate(
      { message: trimmed, conversationId },
      {
        onSuccess: (response) => {
          if (response === null) {
            setAiUnavailable(true);
            return;
          }
          setConversationId(response.conversationId);
          const assistantMessage: ChatMessage = {
            role: 'assistant',
            content: response.message,
            timestamp: response.timestamp,
          };
          setMessages((prev) => [...prev, assistantMessage]);
        },
        onError: () => {
          const errorMessage: ChatMessage = {
            role: 'assistant',
            content: 'Sorry, something went wrong. Please try again.',
            timestamp: new Date().toISOString(),
          };
          setMessages((prev) => [...prev, errorMessage]);
        },
      },
    );
  }, [inputValue, chatMutation, conversationId]);

  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLInputElement>) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        handleSend();
      }
    },
    [handleSend],
  );

  return (
    <>
      {/* Floating trigger button */}
      {!isOpen && (
        <Button
          onClick={() => setIsOpen(true)}
          className="fixed bottom-6 right-6 z-50 h-14 w-14 rounded-full shadow-lg"
          size="icon"
          aria-label="Open AI chat assistant"
        >
          <MessageCircle className="h-6 w-6" />
        </Button>
      )}

      {/* Chat panel */}
      {isOpen && (
        <div className="fixed bottom-6 right-6 z-50 flex h-[32rem] w-[22rem] flex-col overflow-hidden rounded-xl border bg-background shadow-2xl sm:w-96">
          {/* Header */}
          <div className="flex items-center justify-between border-b bg-primary/5 px-4 py-3">
            <div className="flex items-center gap-2">
              <MessageCircle className="h-5 w-5 text-primary" />
              <h2 className="text-sm font-semibold">AI Assistant</h2>
            </div>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={() => setIsOpen(false)}
              aria-label="Close chat"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Messages area */}
          <div className="flex-1 overflow-y-auto px-4 py-3">
            {messages.length === 0 && !aiUnavailable && (
              <div className="flex h-full flex-col items-center justify-center gap-4">
                <p className="text-center text-sm text-muted-foreground">
                  Try asking something like:
                </p>
                <div className="flex flex-col gap-2">
                  {[
                    'What concerts are coming up in New York?',
                    'Find me tickets under $100',
                    'When is the best time to buy tickets?',
                    'What events are good for a date night?',
                  ].map((prompt) => (
                    <button
                      key={prompt}
                      onClick={() => {
                        setInputValue(prompt);
                        inputRef.current?.focus();
                      }}
                      className="rounded-lg border px-3 py-2 text-left text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
                    >
                      {prompt}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {aiUnavailable && (
              <div className="flex h-full items-center justify-center">
                <p className="text-center text-sm text-muted-foreground">
                  AI features are not currently available. Please try again later.
                </p>
              </div>
            )}

            {!aiUnavailable && (
              <div className="space-y-3">
                {messages.map((msg, index) => (
                  <div
                    key={`${msg.role}-${msg.timestamp}-${index}`}
                    className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      data-testid={msg.role === 'user' ? 'user-message' : 'ai-message'}
                      className={`max-w-[80%] rounded-lg px-3 py-2 text-sm ${
                        msg.role === 'user'
                          ? 'bg-primary text-primary-foreground'
                          : 'bg-muted text-foreground'
                      }`}
                    >
                      {msg.role === 'assistant' ? (
                        <MessageContent content={msg.content} />
                      ) : (
                        msg.content
                      )}
                    </div>
                  </div>
                ))}

                {chatMutation.isPending && (
                  <div className="flex justify-start">
                    <div className="flex gap-1 rounded-lg bg-muted px-3 py-2">
                      <span className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground/50 [animation-delay:0ms]" />
                      <span className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground/50 [animation-delay:150ms]" />
                      <span className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground/50 [animation-delay:300ms]" />
                    </div>
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Input area */}
          {!aiUnavailable && (
            <div className="border-t px-4 py-3">
              <div className="flex gap-2">
                <Input
                  ref={inputRef}
                  value={inputValue}
                  onChange={(event) => setInputValue(event.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ask about events..."
                  disabled={chatMutation.isPending}
                  className="text-sm"
                />
                <Button
                  onClick={handleSend}
                  disabled={inputValue.trim().length === 0 || chatMutation.isPending}
                  size="icon"
                  className="shrink-0"
                  aria-label="Send message"
                >
                  <Send className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </div>
      )}
    </>
  );
}
