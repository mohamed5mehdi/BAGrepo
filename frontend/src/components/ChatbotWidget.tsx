import { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  demarrerChatbotSession,
  envoyerChatbotMessage,
  confirmerChatbotDemande,
  getChatbotMessages
} from '../api/services';
import type { ChatResponse, ChatMessage, SlotState } from '../types';
import toast from 'react-hot-toast';
import { useQueryClient } from '@tanstack/react-query';
import ReactMarkdown from 'react-markdown';

interface Props {
  onClose?: () => void;
  onDaCreated?: () => void;
}

export default function ChatbotWidget({ onClose, onDaCreated }: Props) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [isOpen, setIsOpen] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMsg, setInputMsg] = useState('');
  const [slots, setSlots] = useState<SlotState>({});
  const [isComplet, setIsComplet] = useState(false);
  const [isConfirmee, setIsConfirmee] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Initialize session on opening
  useEffect(() => {
    if (isOpen && user && !sessionId) {
      initSession();
    }
  }, [isOpen, user]);

  // Scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  const initSession = async () => {
    if (!user) return;
    setIsLoading(true);
    try {
      const response = await demarrerChatbotSession(user.userId);
      setSessionId(response.data.sessionId);
      setSlots(response.data.slots);
      setIsComplet(response.data.complet);
      setIsConfirmee(response.data.confirmed);

      // Load initial message
      const welcomeMsg: ChatMessage = {
        id: crypto.randomUUID(),
        sessionId: response.data.sessionId,
        role: 'BOT',
        content: response.data.botMessage,
        dateEnvoi: new Date().toISOString()
      };
      setMessages([welcomeMsg]);
    } catch (error: any) {
      console.error('Error starting session', error);
      toast.error('Impossible d\'initialiser l\'assistant IA');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSendMessage = async (textToSend?: string) => {
    const text = textToSend !== undefined ? textToSend : inputMsg.trim();
    if (!text || !sessionId || !user || isLoading) return;

    if (textToSend === undefined) {
      setInputMsg('');
    }

    // Append local user message
    const userLocalMsg: ChatMessage = {
      id: crypto.randomUUID(),
      sessionId,
      role: 'USER',
      content: text,
      dateEnvoi: new Date().toISOString()
    };
    setMessages(prev => [...prev, userLocalMsg]);
    setIsLoading(true);

    try {
      const response = await envoyerChatbotMessage(sessionId, user.userId, text);
      setSlots(response.data.slots);
      setIsComplet(response.data.complet);
      setIsConfirmee(response.data.confirmed);

      // Append bot response
      const botResponseMsg: ChatMessage = {
        id: crypto.randomUUID(),
        sessionId,
        role: 'BOT',
        content: response.data.botMessage,
        dateEnvoi: new Date().toISOString()
      };
      setMessages(prev => [...prev, botResponseMsg]);
    } catch (error: any) {
      console.error('Error sending message', error);
      toast.error('Erreur de communication avec l\'IA');
    } finally {
      setIsLoading(false);
    }
  };

  const handleConfirmSubmit = async () => {
    if (!sessionId || !user || isLoading) return;
    setIsLoading(true);
    try {
      toast.loading('Création de votre demande d\'achat...', { id: 'submit-da-bot' });
      const response = await confirmerChatbotDemande(sessionId, user.userId);
      setIsConfirmee(true);
      
      const botConfirmMsg: ChatMessage = {
        id: crypto.randomUUID(),
        sessionId,
        role: 'BOT',
        content: `🎉 **Félicitations !** Votre demande d'achat a été créée et soumise avec succès sous le numéro **DA-${response.data.id}** ! Vous pouvez suivre son avancement sur votre tableau de bord.`,
        dateEnvoi: new Date().toISOString()
      };
      setMessages(prev => [...prev, botConfirmMsg]);
      toast.success('Demande d\'achat créée avec succès !', { id: 'submit-da-bot' });
      
      onDaCreated?.();
    } catch (error: any) {
      console.error('Error confirming DA', error);
      toast.error('Erreur lors de la confirmation : ' + (error.response?.data || error.message), { id: 'submit-da-bot' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = async () => {
    await handleSendMessage('recommencer');
  };

  if (!user || user.role !== 'EMPLOYE') return null;

  return (
    <div className="fixed bottom-6 right-6 z-[100] font-sans">
      {/* Floating Toggle Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="w-16 h-16 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-700 text-white shadow-2xl flex items-center justify-center hover:scale-110 active:scale-95 transition-all duration-300 relative group"
        >
          <span className="text-2xl animate-pulse">🤖</span>
          <span className="absolute -top-1 -right-1 flex h-4 w-4">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-4 w-4 bg-emerald-500"></span>
          </span>
          <div className="absolute right-20 bg-slate-900 text-white text-xs font-bold px-3 py-1.5 rounded-xl opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap shadow-lg">
            Assistant IA Achat BAG
          </div>
        </button>
      )}

      {/* Chat Window */}
      {isOpen && (
        <div className="w-[420px] h-[580px] bg-white/95 dark:bg-slate-900/95 backdrop-blur-md rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-800/80 flex flex-col overflow-hidden animate-scale-in">
          {/* Header */}
          <div className="px-6 py-4 bg-gradient-to-r from-blue-600 to-indigo-700 text-white flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-white/10 flex items-center justify-center text-xl">
                🤖
              </div>
              <div>
                <h3 className="font-bold text-sm">Assistant IA</h3>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                  <span className="text-[10px] text-blue-100 font-medium">Expert Achat BAG • En ligne</span>
                </div>
              </div>
            </div>
            <button
              onClick={() => {
                setIsOpen(false);
                onClose?.();
              }}
              className="p-1.5 hover:bg-white/10 rounded-full text-blue-100 hover:text-white transition-all"
            >
              ✕
            </button>
          </div>

          {/* Slots State Tracker */}
          <div className="px-5 py-3 bg-slate-50 dark:bg-slate-800/40 border-b border-slate-100 dark:border-slate-800/60 flex flex-wrap gap-2">
            <span className={`text-[10px] px-2 py-1 rounded-lg font-bold border transition-all ${slots.designation ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : 'bg-slate-100 text-slate-400 border-transparent'}`}>
              🏷️ Article
            </span>
            <span className={`text-[10px] px-2 py-1 rounded-lg font-bold border transition-all ${slots.quantite ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : 'bg-slate-100 text-slate-400 border-transparent'}`}>
              🔢 Qté: {slots.quantite || ''}
            </span>
            <span className={`text-[10px] px-2 py-1 rounded-lg font-bold border transition-all ${slots.familyLibelle ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : 'bg-slate-100 text-slate-400 border-transparent'}`}>
              📂 Catégorie
            </span>
            <span className={`text-[10px] px-2 py-1 rounded-lg font-bold border transition-all ${slots.urgence ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : 'bg-slate-100 text-slate-400 border-transparent'}`}>
              ⚠️ Urgence
            </span>
          </div>

          {/* Messages Container */}
          <div className="flex-1 overflow-y-auto p-5 space-y-4">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex gap-3 max-w-[85%] ${msg.role === 'USER' ? 'ml-auto flex-row-reverse' : ''}`}
              >
                {msg.role === 'BOT' && (
                  <div className="w-8 h-8 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-sm flex-shrink-0">
                    🤖
                  </div>
                )}
                <div
                  className={`p-4 rounded-2xl text-xs leading-relaxed shadow-sm prose prose-sm dark:prose-invert max-w-none ${
                    msg.role === 'USER'
                      ? 'bg-blue-600 text-white rounded-tr-none'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 rounded-tl-none border border-slate-50 dark:border-slate-800/30'
                  }`}
                >
                  <ReactMarkdown>{msg.content}</ReactMarkdown>
                </div>
              </div>
            ))}
            {isLoading && (
              <div className="flex gap-3 max-w-[85%]">
                <div className="w-8 h-8 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-sm flex-shrink-0">
                  🤖
                </div>
                <div className="p-4 rounded-2xl text-xs bg-slate-100 dark:bg-slate-800 text-slate-400 rounded-tl-none border border-slate-50 dark:border-slate-800/30 flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-slate-400 animate-bounce"></span>
                  <span className="w-1.5 h-1.5 rounded-full bg-slate-400 animate-bounce [animation-delay:0.2s]"></span>
                  <span className="w-1.5 h-1.5 rounded-full bg-slate-400 animate-bounce [animation-delay:0.4s]"></span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Quick Replies & Suggestions */}
          {!isConfirmee && !isLoading && (
            <div className="px-5 py-2 flex flex-wrap gap-2 bg-slate-50/50 dark:bg-slate-800/20 border-t border-slate-100 dark:border-slate-800/40">
              {isComplet && (
                <>
                  <button onClick={handleConfirmSubmit} className="px-3 py-1.5 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-[11px] font-bold text-emerald-600 hover:bg-emerald-50 transition-all shadow-sm">👍 Oui, soumettre</button>
                  <button onClick={() => handleSendMessage('non')} className="px-3 py-1.5 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-[11px] font-bold text-rose-600 hover:bg-rose-50 transition-all shadow-sm">👎 Non, modifier</button>
                </>
              )}
              {slots.designation && (
                <button onClick={handleReset} className="px-3 py-1.5 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-[11px] font-bold text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-800 transition-all shadow-sm flex items-center gap-1 ml-auto">
                  🔄 Réinitialiser
                </button>
              )}
            </div>
          )}

          {/* Action Input Footer */}
          <div className="p-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/30">
            {isComplet && !isConfirmee ? (
              <div className="flex gap-3">
                <button
                  onClick={handleReset}
                  className="flex-1 py-3 rounded-2xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 bg-white dark:bg-slate-800 text-xs font-bold hover:bg-slate-50 transition-all"
                >
                  Recommencer
                </button>
              </div>
            ) : isConfirmee ? (
              <button
                onClick={() => {
                  setMessages([]);
                  setSessionId(null);
                  setSlots({});
                  setIsComplet(false);
                  setIsConfirmee(false);
                  initSession();
                }}
                className="w-full py-3 rounded-2xl bg-blue-600 text-white text-xs font-bold hover:bg-blue-700 transition-all shadow-lg"
              >
                Créer une autre demande
              </button>
            ) : (
              <div className="relative flex items-center">
                <input
                  type="text"
                  value={inputMsg}
                  onChange={(e) => setInputMsg(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                  placeholder="Posez votre demande ici (ex: 3 ordinateurs)..."
                  disabled={isLoading}
                  className="w-full pl-5 pr-14 py-3.5 rounded-2xl border border-slate-200 dark:border-slate-700/80 bg-white dark:bg-slate-800 text-xs focus:ring-2 focus:ring-blue-500 outline-none transition-all dark:text-white"
                />
                <button
                  onClick={() => handleSendMessage()}
                  disabled={isLoading || !inputMsg.trim()}
                  className="absolute right-2.5 p-2 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:opacity-40 text-white transition-all shadow-md"
                >
                  <span className="text-sm">➔</span>
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
