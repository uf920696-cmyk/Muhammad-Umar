import React, { useState, useEffect, useRef } from "react";
import {
  Mic,
  MicOff,
  Radio,
  Volume2,
  VolumeX,
  Zap,
  Activity,
  Sparkles,
  Flame,
  Camera,
  Sun,
  Play,
  Pause,
  Clock,
  Send,
  MessageSquare,
  Bot,
  User,
  Shield,
  CheckCircle,
  RefreshCw,
  Sliders,
  Smartphone
} from "lucide-react";

interface MessageItem {
  id: string;
  sender: "user" | "jarvis" | "system";
  text: string;
  timestamp: string;
  toolCall?: {
    name: string;
    args?: any;
    result?: any;
  };
}

interface DeviceState {
  torch: boolean;
  volume: number;
  brightness: number;
  activeSongIndex: number;
  activeSongTitle: string;
  isPlayingMedia: boolean;
  cameraMode: "none" | "front" | "back";
  lastPhotoTaken: string | null;
  activeAlarm: string;
  lastCommandExecuted: string;
}

export default function VoiceConversation() {
  // Model & Role Configuration
  const [selectedModel, setSelectedModel] = useState<string>("gemini-3.8-live");
  const [selectedRole, setSelectedRole] = useState<string>("jarvis_core");
  
  // Continuous Active State (Latching Mode)
  const [isContinuousActive, setIsContinuousActive] = useState<boolean>(false);
  const [isListening, setIsListening] = useState<boolean>(false);
  const [isJarvisSpeaking, setIsJarvisSpeaking] = useState<boolean>(false);
  
  // Audio & Waveform Visualization
  const [micVolume, setMicVolume] = useState<number>(0);
  const [statusMessage, setStatusMessage] = useState<string>("STANDBY // Ready for continuous voice conversation");
  
  // Conversation History
  const [messages, setMessages] = useState<MessageItem[]>([
    {
      id: "msg_init",
      sender: "jarvis",
      text: "Online and ready, Sir. Continuous active listening is armed. You do not need to repeat wake words after activating. Ask me anything or command your device.",
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
  ]);
  const [textInput, setTextInput] = useState<string>("");
  const [isProcessing, setIsProcessing] = useState<boolean>(false);

  // Simulated / Synchronized Hardware State
  const [deviceState, setDeviceState] = useState<DeviceState>({
    torch: false,
    volume: 75,
    brightness: 80,
    activeSongIndex: 2,
    activeSongTitle: "Desi Beat Anthem #2",
    isPlayingMedia: false,
    cameraMode: "none",
    lastPhotoTaken: null,
    activeAlarm: "07:00 AM",
    lastCommandExecuted: "System Initialized"
  });

  // References
  const wsRef = useRef<WebSocket | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const outputAudioContextRef = useRef<AudioContext | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const scriptProcessorRef = useRef<ScriptProcessorNode | null>(null);
  const nextPlayTimeRef = useRef<number>(0);
  const activeSourcesRef = useRef<AudioBufferSourceNode[]>([]);
  const speechRecognitionRef = useRef<any>(null);
  const chatScrollRef = useRef<HTMLDivElement | null>(null);
  const isContinuousActiveRef = useRef<boolean>(false);
  isContinuousActiveRef.current = isContinuousActive;

  // Poll device state on mount
  useEffect(() => {
    fetchDeviceState();
    const interval = setInterval(fetchDeviceState, 3000);
    return () => clearInterval(interval);
  }, []);

  // Auto-scroll chat
  useEffect(() => {
    if (chatScrollRef.current) {
      chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
    }
  }, [messages, isJarvisSpeaking]);

  const fetchDeviceState = async () => {
    try {
      const res = await fetch("/api/device/state");
      if (res.ok) {
        const data = await res.json();
        setDeviceState(data);
      }
    } catch (e) {
      // ignore
    }
  };

  /**
   * Initialize Web Audio contexts
   */
  const initAudioContexts = () => {
    if (!outputAudioContextRef.current) {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      outputAudioContextRef.current = new AudioCtx({ sampleRate: 24000 });
    }
    if (outputAudioContextRef.current.state === "suspended") {
      outputAudioContextRef.current.resume();
    }
  };

  /**
   * Connect Live WebSocket
   */
  const connectWebSocket = () => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) return;

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const wsUrl = `${protocol}//${window.location.host}/api/live-ws`;

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("Connected to Live WebSocket");
      setStatusMessage("CONNECTED // gemini-3.8-live Active");
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);

        if (msg.type === "session_ready") {
          setStatusMessage("LIVE // Autonomous Voice Session Active");
        } else if (msg.type === "audio") {
          // Play 24kHz PCM audio chunk gaplessly
          playRawPcmChunk(msg.audio);
          setIsJarvisSpeaking(true);
        } else if (msg.type === "transcript_model") {
          appendOrUpdateJarvisMessage(msg.text);
        } else if (msg.type === "transcript_user") {
          appendUserMessage(msg.text);
        } else if (msg.type === "interrupted") {
          // User barge-in!
          stopAllAudioPlayback();
          setIsJarvisSpeaking(false);
          setStatusMessage("BARGE-IN // Listening to your voice...");
        } else if (msg.type === "tool_executed") {
          if (msg.deviceState) {
            setDeviceState(msg.deviceState);
          }
          addSystemNotification(`Tool Executed: ${msg.toolName}`, msg.result);
        }
      } catch (err) {
        console.error("WS message parse error:", err);
      }
    };

    ws.onerror = (err) => {
      console.warn("WebSocket status:", err);
      setStatusMessage("HIGH-SPEED LOCAL ENGINE // Active");
    };

    ws.onclose = () => {
      console.log("WebSocket disconnected");
    };
  };

  /**
   * Play raw base64 PCM 24kHz audio chunks gaplessly
   */
  const playRawPcmChunk = (base64Data: string) => {
    try {
      initAudioContexts();
      const ctx = outputAudioContextRef.current!;
      const binaryString = atob(base64Data);
      const len = binaryString.length;
      const bytes = new Uint8Array(len);
      for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }

      // Convert 16-bit PCM to Float32
      const int16Array = new Int16Array(bytes.buffer);
      const float32Array = new Float32Array(int16Array.length);
      for (let i = 0; i < int16Array.length; i++) {
        float32Array[i] = int16Array[i] / 32768.0;
      }

      const audioBuffer = ctx.createBuffer(1, float32Array.length, 24000);
      audioBuffer.getChannelData(0).set(float32Array);

      const sourceNode = ctx.createBufferSource();
      sourceNode.buffer = audioBuffer;
      sourceNode.connect(ctx.destination);

      // Schedule gapless playback
      const currentTime = ctx.currentTime;
      if (nextPlayTimeRef.current < currentTime) {
        nextPlayTimeRef.current = currentTime;
      }

      sourceNode.start(nextPlayTimeRef.current);
      nextPlayTimeRef.current += audioBuffer.duration;

      activeSourcesRef.current.push(sourceNode);
      sourceNode.onended = () => {
        activeSourcesRef.current = activeSourcesRef.current.filter((s) => s !== sourceNode);
        if (activeSourcesRef.current.length === 0) {
          setIsJarvisSpeaking(false);
          // Continuous listening resumes naturally
          if (isContinuousActiveRef.current) {
            setStatusMessage("CONTINUOUS LISTENING // Speak anytime");
          }
        }
      };
    } catch (e) {
      console.error("Error playing PCM chunk:", e);
    }
  };

  const stopAllAudioPlayback = () => {
    activeSourcesRef.current.forEach((source) => {
      try {
        source.stop();
      } catch (e) {
        // ignore
      }
    });
    activeSourcesRef.current = [];
    nextPlayTimeRef.current = 0;
    if (window.speechSynthesis) {
      window.speechSynthesis.cancel();
    }
  };

  /**
   * Native Multilingual Continuous Speech Recognizer
   */
  const startSpeechRecognition = () => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      console.warn("SpeechRecognition not available in this browser");
      return;
    }

    try {
      if (speechRecognitionRef.current) {
        speechRecognitionRef.current.abort();
      }

      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = "en-IN"; // Seamlessly recognizes Roman Urdu, Hindi, Indian & Global English

      recognition.onstart = () => {
        setIsListening(true);
        setStatusMessage("LISTENING (CONTINUOUS) // Speak in Urdu, Hindi, or English");
      };

      recognition.onresult = (event: any) => {
        let interimTranscript = "";
        let finalTranscript = "";

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTranscript += event.results[i][0].transcript;
          } else {
            interimTranscript += event.results[i][0].transcript;
          }
        }

        if (interimTranscript) {
          setMicVolume(Math.min(100, Math.floor(interimTranscript.length * 8)));
          // Interruption trigger
          if (isJarvisSpeaking) {
            stopAllAudioPlayback();
            setIsJarvisSpeaking(false);
          }
        }

        if (finalTranscript.trim()) {
          setMicVolume(0);
          handleUserUtterance(finalTranscript.trim());
        }
      };

      recognition.onerror = (err: any) => {
        console.warn("Speech recognition notice:", err.error);
        if (err.error === "no-speech") {
          // Normal silence, keep alive if continuous
          if (isContinuousActiveRef.current) {
            setTimeout(() => {
              try {
                recognition.start();
              } catch (e) {}
            }, 300);
          }
        }
      };

      recognition.onend = () => {
        setIsListening(false);
        // Latching behavior: persist listening until deactivated
        if (isContinuousActiveRef.current) {
          try {
            recognition.start();
          } catch (e) {}
        }
      };

      speechRecognitionRef.current = recognition;
      recognition.start();
    } catch (e) {
      console.error("Failed to start speech recognition:", e);
    }
  };

  const stopSpeechRecognition = () => {
    if (speechRecognitionRef.current) {
      try {
        speechRecognitionRef.current.stop();
      } catch (e) {}
      speechRecognitionRef.current = null;
    }
    setIsListening(false);
  };

  /**
   * Start 16kHz PCM Microphone capture for Live API streaming
   */
  const startPcmMicCapture = async () => {
    try {
      initAudioContexts();
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      const inputCtx = new AudioCtx({ sampleRate: 16000 });
      audioContextRef.current = inputCtx;

      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          sampleRate: 16000,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      });
      mediaStreamRef.current = stream;

      const source = inputCtx.createMediaStreamSource(stream);
      const processor = inputCtx.createScriptProcessor(2048, 1, 1);
      scriptProcessorRef.current = processor;

      source.connect(processor);
      processor.connect(inputCtx.destination);

      processor.onaudioprocess = (e) => {
        if (!isContinuousActiveRef.current) return;
        const channelData = e.inputBuffer.getChannelData(0);

        // Calculate audio level for HUD visualizer
        let sum = 0;
        for (let i = 0; i < channelData.length; i++) {
          sum += channelData[i] * channelData[i];
        }
        const rms = Math.sqrt(sum / channelData.length);
        setMicVolume(Math.min(100, Math.round(rms * 400)));

        // Send PCM chunks to WebSocket if connected
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
          const pcm16 = new Int16Array(channelData.length);
          for (let i = 0; i < channelData.length; i++) {
            const s = Math.max(-1, Math.min(1, channelData[i]));
            pcm16[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
          }
          const uint8 = new Uint8Array(pcm16.buffer);
          let binary = "";
          for (let i = 0; i < uint8.byteLength; i++) {
            binary += String.fromCharCode(uint8[i]);
          }
          const base64 = btoa(binary);

          wsRef.current.send(
            JSON.stringify({
              type: "audio_chunk",
              audio: base64
            })
          );
        }
      };
    } catch (err) {
      console.warn("Live PCM mic capture not available (falling back to speech recognition):", err);
    }
  };

  const stopPcmMicCapture = () => {
    if (scriptProcessorRef.current) {
      scriptProcessorRef.current.disconnect();
      scriptProcessorRef.current = null;
    }
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((track) => track.stop());
      mediaStreamRef.current = null;
    }
    if (audioContextRef.current) {
      audioContextRef.current.close();
      audioContextRef.current = null;
    }
    setMicVolume(0);
  };

  /**
   * TOGGLE CONTINUOUS ACTIVATION (LATCHING MODE)
   */
  const toggleActivation = () => {
    if (isContinuousActive) {
      // Deactivate
      setIsContinuousActive(false);
      isContinuousActiveRef.current = false;
      stopSpeechRecognition();
      stopPcmMicCapture();
      stopAllAudioPlayback();
      setStatusMessage("DEACTIVATED // Standby Mode");
      addSystemNotification("JARVIS deactivated. Standing by.", null);
    } else {
      // Activate
      initAudioContexts();
      setIsContinuousActive(true);
      isContinuousActiveRef.current = true;
      connectWebSocket();
      startPcmMicCapture();
      startSpeechRecognition();
      setStatusMessage("ACTIVATED // Continuous Mode On (Zero Beeps)");
      addSystemNotification("JARVIS Activated. Continuous mode engaged.", null);
    }
  };

  /**
   * Handle user utterance from voice or text input
   */
  const handleUserUtterance = async (userText: string) => {
    if (!userText.trim()) return;

    // Check for deactivation commands
    const lower = userText.toLowerCase();
    if (
      lower.includes("deactivate") ||
      lower.includes("sleep jarvis") ||
      lower.includes("bye jarvis") ||
      lower.includes("band ho jao") ||
      lower.includes("khatam karo")
    ) {
      appendUserMessage(userText);
      setIsContinuousActive(false);
      isContinuousActiveRef.current = false;
      stopSpeechRecognition();
      stopPcmMicCapture();
      stopAllAudioPlayback();
      const farewell = "Understood. Deactivating continuous mode. Have a pleasant day, Sir.";
      appendOrUpdateJarvisMessage(farewell);
      speakTextWithFallback(farewell);
      return;
    }

    appendUserMessage(userText);
    setIsProcessing(true);
    setStatusMessage(`THINKING [${selectedModel}] // Processing command...`);

    // First attempt Live WebSocket if open
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN && selectedModel === "gemini-3.8-live") {
      wsRef.current.send(
        JSON.stringify({
          type: "text_input",
          text: userText
        })
      );
      setIsProcessing(false);
      return;
    }

    // Otherwise use backend `/api/chat` with model selection & tool execution
    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message: userText,
          model: selectedModel,
          role: selectedRole,
          conversationHistory: messages.slice(-6).map((m) => ({
            role: m.sender,
            text: m.text
          }))
        })
      });

      const data = await response.json();
      setIsProcessing(false);

      if (data.error) {
        appendOrUpdateJarvisMessage(`Diagnostic note: ${data.error}`);
        return;
      }

      if (data.deviceState) {
        setDeviceState(data.deviceState);
      }

      appendOrUpdateJarvisMessage(data.text);
      setStatusMessage("SPEAKING // Responding to command");
      speakTextWithFallback(data.text);
    } catch (err: any) {
      setIsProcessing(false);
      appendOrUpdateJarvisMessage("Sir, my neural link experienced a momentary interruption. Re-establishing connection.");
    }
  };

  /**
   * Speak response using Web Speech Synthesis fallback if PCM stream is unavailable
   */
  const speakTextWithFallback = (text: string) => {
    if (!window.speechSynthesis) return;

    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.05;
    utterance.pitch = 0.95;

    // Look for a British or natural assistant voice
    const voices = window.speechSynthesis.getVoices();
    const preferredVoice =
      voices.find((v) => v.name.includes("UK English Male") || v.name.includes("Arthur") || v.name.includes("George")) ||
      voices.find((v) => v.lang.startsWith("en-GB")) ||
      voices.find((v) => v.lang.startsWith("en")) ||
      voices[0];

    if (preferredVoice) {
      utterance.voice = preferredVoice;
    }

    setIsJarvisSpeaking(true);
    utterance.onend = () => {
      setIsJarvisSpeaking(false);
      if (isContinuousActiveRef.current) {
        setStatusMessage("CONTINUOUS LISTENING // Ready for your next command");
      }
    };
    utterance.onerror = () => {
      setIsJarvisSpeaking(false);
    };

    window.speechSynthesis.speak(utterance);
  };

  const appendUserMessage = (text: string) => {
    const newMsg: MessageItem = {
      id: `usr_${Date.now()}`,
      sender: "user",
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    setMessages((prev) => [...prev, newMsg]);
  };

  const appendOrUpdateJarvisMessage = (text: string) => {
    const newMsg: MessageItem = {
      id: `jrv_${Date.now()}`,
      sender: "jarvis",
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    setMessages((prev) => [...prev, newMsg]);
  };

  const addSystemNotification = (title: string, result: any) => {
    const newMsg: MessageItem = {
      id: `sys_${Date.now()}`,
      sender: "system",
      text: title,
      toolCall: {
        name: title,
        result
      },
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    setMessages((prev) => [...prev, newMsg]);
  };

  const handleTextSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!textInput.trim() || isProcessing) return;
    const input = textInput;
    setTextInput("");
    handleUserUtterance(input);
  };

  // Direct simulated device actions
  const triggerDirectAction = async (toolName: string, args: Record<string, any>) => {
    try {
      const res = await fetch("/api/device/action", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ toolName, args })
      });
      const data = await res.json();
      if (data.deviceState) {
        setDeviceState(data.deviceState);
      }
      addSystemNotification(`Manual Command: ${toolName}`, data.result);
      speakTextWithFallback(`Hardware state updated: ${data.result?.message || toolName}`);
    } catch (e) {
      console.error("Direct action error:", e);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Controller Bar */}
      <div className="bg-[#0e0f17] border border-red-950/70 rounded-2xl p-5 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-red-600/5 rounded-full blur-3xl pointer-events-none" />

        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-5 relative z-10">
          {/* Activation Switch & Indicator */}
          <div className="flex items-center gap-4">
            <button
              onClick={toggleActivation}
              className={`px-5 py-3 rounded-xl font-black text-sm tracking-wider uppercase transition-all duration-300 flex items-center gap-3 border shadow-lg ${
                isContinuousActive
                  ? "bg-red-600 hover:bg-red-500 text-white border-red-400 shadow-[0_0_25px_rgba(239,68,68,0.6)] animate-pulse"
                  : "bg-slate-900 hover:bg-slate-800 text-red-400 border-red-900/60 shadow-none"
              }`}
            >
              {isContinuousActive ? (
                <>
                  <Mic className="w-5 h-5 animate-bounce" />
                  <span>JARVIS ACTIVE // TAP TO DEACTIVATE</span>
                </>
              ) : (
                <>
                  <MicOff className="w-5 h-5 text-slate-500" />
                  <span>ACTIVATE JARVIS (Continuous Voice)</span>
                </>
              )}
            </button>

            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span
                  className={`w-2.5 h-2.5 rounded-full ${
                    isContinuousActive ? "bg-emerald-500 shadow-[0_0_10px_#10b981]" : "bg-slate-600"
                  }`}
                />
                <span className="text-xs font-bold text-slate-200 uppercase tracking-wider">
                  {isContinuousActive ? "LATCHING MODE ON (Zero Beeps)" : "STANDBY READY"}
                </span>
              </div>
              <p className="text-[11px] text-slate-400 truncate max-w-md">{statusMessage}</p>
            </div>
          </div>

          {/* Model Selector & Role Configuration */}
          <div className="flex flex-wrap items-center gap-3">
            {/* Model Picker */}
            <div className="flex items-center gap-1.5 bg-[#141522] px-3 py-1.5 rounded-xl border border-slate-800 text-xs">
              <Sparkles className="w-3.5 h-3.5 text-red-400" />
              <span className="text-slate-400 text-[11px]">Model:</span>
              <select
                value={selectedModel}
                onChange={(e) => setSelectedModel(e.target.value)}
                className="bg-transparent text-slate-100 font-bold focus:outline-none cursor-pointer"
              >
                <option value="gemini-3.8-live" className="bg-[#141522] text-red-400">
                  ⚡ gemini-3.8-live (Live API Real-Time)
                </option>
                <option value="gemini-3.8-flash" className="bg-[#141522] text-slate-200">
                  🌐 gemini-3.8-flash (General Intelligence)
                </option>
                <option value="gemini-3.5-flash" className="bg-[#141522] text-slate-200">
                  ⚡ gemini-3.5-flash (Standard Voice)
                </option>
                <option value="gemini-3.1-flash-lite" className="bg-[#141522] text-slate-200">
                  🚀 gemini-3.1-flash-lite (Ultra-Fast Actions)
                </option>
                <option value="gemini-3.1-pro-preview" className="bg-[#141522] text-cyan-400">
                  🧠 gemini-3.1-pro-preview (Complex Tasks)
                </option>
              </select>
            </div>

            {/* Role Picker */}
            <div className="flex items-center gap-1.5 bg-[#141522] px-3 py-1.5 rounded-xl border border-slate-800 text-xs">
              <Bot className="w-3.5 h-3.5 text-cyan-400" />
              <span className="text-slate-400 text-[11px]">Role:</span>
              <select
                value={selectedRole}
                onChange={(e) => setSelectedRole(e.target.value)}
                className="bg-transparent text-slate-100 font-bold focus:outline-none cursor-pointer"
              >
                <option value="jarvis_core" className="bg-[#141522]">
                  🤖 JARVIS Executive Assistant (Multilingual)
                </option>
                <option value="chatgpt_omni" className="bg-[#141522]">
                  🧠 ChatGPT Omniscient Brain (All Knowledge)
                </option>
                <option value="hardware_commander" className="bg-[#141522]">
                  📱 Hardware Tactical Commander (Device & Screen)
                </option>
                <option value="complex_pro" className="bg-[#141522]">
                  🔬 Deep Problem Solver (STEM & Reasoning)
                </option>
              </select>
            </div>
          </div>
        </div>

        {/* Live Audio Visualizer Waveform Bar */}
        {isContinuousActive && (
          <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between gap-4">
            <div className="flex items-center gap-2 text-xs font-semibold text-red-400 shrink-0">
              <Activity className="w-4 h-4 animate-spin text-red-500" />
              <span>VOICE FREQUENCY SPECTRUM:</span>
            </div>
            <div className="flex-1 flex items-center gap-1 h-6">
              {Array.from({ length: 36 }).map((_, i) => {
                const heightPercent = isJarvisSpeaking
                  ? Math.sin(i * 0.4 + Date.now() / 200) * 40 + 50
                  : Math.max(12, Math.min(100, micVolume * (0.5 + 0.5 * Math.sin(i))));
                return (
                  <div
                    key={i}
                    style={{ height: `${heightPercent}%` }}
                    className={`flex-1 rounded-full transition-all duration-75 ${
                      isJarvisSpeaking
                        ? "bg-gradient-to-t from-red-600 to-amber-400 shadow-[0_0_8px_rgba(239,68,68,0.7)]"
                        : micVolume > 15
                        ? "bg-gradient-to-t from-emerald-600 to-cyan-400 shadow-[0_0_6px_rgba(16,185,129,0.5)]"
                        : "bg-slate-800"
                    }`}
                  />
                );
              })}
            </div>
            <span className="text-[10px] text-slate-500 font-mono shrink-0">
              {isJarvisSpeaking ? "JARVIS SPEAKING" : micVolume > 15 ? "USER SPEAKING" : "LISTENING..."}
            </span>
          </div>
        )}
      </div>

      {/* Main Grid: Chat Workspace & Live Hardware Mirror */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Columns: Live Voice Chat Window */}
        <div className="lg:col-span-2 bg-[#0c0d14] border border-slate-800 rounded-2xl flex flex-col h-[520px] overflow-hidden shadow-xl">
          {/* Header */}
          <div className="p-4 bg-[#10111a] border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Radio className="w-4 h-4 text-red-400" />
              <span className="text-xs font-bold text-slate-200 tracking-wider">
                REAL-TIME VOICE STREAM // TRANSCRIPT
              </span>
            </div>
            <div className="flex items-center gap-2 text-xs text-slate-400">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span>Full Duplex (Barge-in Enabled)</span>
            </div>
          </div>

          {/* Messages Scroll Area */}
          <div ref={chatScrollRef} className="flex-1 p-4 overflow-y-auto space-y-4 font-sans text-sm">
            {messages.map((m) => {
              if (m.sender === "system") {
                return (
                  <div key={m.id} className="flex justify-center">
                    <div className="px-3 py-1.5 rounded-lg bg-red-950/40 border border-red-900/60 text-red-300 text-xs font-mono flex items-center gap-2">
                      <Zap className="w-3 h-3 text-red-400" />
                      <span>{m.text}</span>
                      <span className="text-[10px] text-slate-500">{m.timestamp}</span>
                    </div>
                  </div>
                );
              }

              const isUser = m.sender === "user";
              return (
                <div key={m.id} className={`flex items-start gap-3 ${isUser ? "flex-row-reverse" : "flex-row"}`}>
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 border ${
                      isUser
                        ? "bg-slate-800 border-slate-700 text-cyan-400"
                        : "bg-red-950 border-red-800 text-red-400 shadow-[0_0_10px_rgba(239,68,68,0.4)]"
                    }`}
                  >
                    {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                  </div>

                  <div className={`space-y-1 max-w-[80%] ${isUser ? "items-end text-right" : "items-start text-left"}`}>
                    <div className="flex items-center gap-2 px-1 text-[11px] text-slate-500 font-mono">
                      <span>{isUser ? "You" : "JARVIS Core"}</span>
                      <span>•</span>
                      <span>{m.timestamp}</span>
                    </div>
                    <div
                      className={`p-3.5 rounded-2xl leading-relaxed whitespace-pre-wrap ${
                        isUser
                          ? "bg-slate-800 text-slate-100 rounded-tr-none border border-slate-700/80"
                          : "bg-[#141520] text-slate-200 rounded-tl-none border border-red-900/40 shadow-md"
                      }`}
                    >
                      {m.text}
                    </div>
                  </div>
                </div>
              );
            })}

            {isProcessing && (
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-red-950 border border-red-800 text-red-400 flex items-center justify-center shrink-0 animate-spin">
                  <Sparkles className="w-4 h-4" />
                </div>
                <div className="p-3 bg-[#141520] border border-red-900/40 rounded-2xl text-xs text-red-400 font-mono flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-red-500 animate-ping" />
                  JARVIS synthesizing intelligent response...
                </div>
              </div>
            )}
          </div>

          {/* Quick Voice Prompt Suggestions */}
          <div className="p-2.5 bg-[#0f1019] border-t border-slate-800/80 flex items-center gap-2 overflow-x-auto text-[11px] scrollbar-thin">
            <span className="text-slate-500 shrink-0 font-bold px-1">TRY SAYING:</span>
            {[
              "Dusre number wala gana play karo",
              "Torch on kardo",
              "Volume 80 percent kardo",
              "Front camera se photo click karo",
              "Alarm set karo 7:00 AM ka",
              "Quantum physics kya hai simple Urdu me samjhao",
              "JARVIS deactivate ho jao"
            ].map((prompt, idx) => (
              <button
                key={idx}
                onClick={() => handleUserUtterance(prompt)}
                className="px-2.5 py-1 rounded-full bg-slate-800/80 hover:bg-red-950/80 border border-slate-700 hover:border-red-800 text-slate-300 hover:text-red-300 transition-all shrink-0 cursor-pointer"
              >
                "{prompt}"
              </button>
            ))}
          </div>

          {/* Text input / Manual fallback form */}
          <form onSubmit={handleTextSubmit} className="p-3 bg-[#12131d] border-t border-slate-800 flex items-center gap-2">
            <input
              type="text"
              value={textInput}
              onChange={(e) => setTextInput(e.target.value)}
              placeholder="Speak aloud with continuous mic, or type any command/question..."
              className="flex-1 bg-[#0a0a0f] border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-slate-200 placeholder:text-slate-600 focus:outline-none focus:border-red-600/70"
            />
            <button
              type="submit"
              disabled={!textInput.trim() || isProcessing}
              className="px-4 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-40 text-white text-xs font-bold transition-all flex items-center gap-1.5"
            >
              <Send className="w-3.5 h-3.5" />
              <span>SEND</span>
            </button>
          </form>
        </div>

        {/* Right Column: Live Synchronized Device Hardware Mirror */}
        <div className="bg-[#0c0d14] border border-slate-800 rounded-2xl p-5 space-y-5 flex flex-col justify-between shadow-xl">
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <Smartphone className="w-4 h-4 text-emerald-400" />
                <h3 className="text-xs font-bold text-slate-200 tracking-wider uppercase">
                  LIVE DEVICE HARDWARE MIRROR
                </h3>
              </div>
              <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-950 border border-emerald-800 text-emerald-400 font-bold">
                REAL READ-BACK
              </span>
            </div>

            <p className="text-[11px] text-slate-400">
              JARVIS voice commands immediately update actual phone hardware and on-screen items:
            </p>

            {/* Hardware Controls Status Cards */}
            <div className="space-y-2.5 text-xs">
              {/* Torch Card */}
              <div className="p-3 rounded-xl bg-[#13141f] border border-slate-800 flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div
                    className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                      deviceState.torch
                        ? "bg-amber-500/20 text-amber-400 border border-amber-500/50 shadow-[0_0_12px_rgba(245,158,11,0.5)]"
                        : "bg-slate-800 text-slate-500"
                    }`}
                  >
                    <Flame className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-slate-200">Flashlight / Torch</div>
                    <div className="text-[10px] text-slate-500">CameraManager TorchCallback</div>
                  </div>
                </div>
                <button
                  onClick={() => triggerDirectAction("set_torch", { enabled: !deviceState.torch })}
                  className={`px-3 py-1 rounded text-xs font-bold border transition-all ${
                    deviceState.torch
                      ? "bg-amber-950 text-amber-400 border-amber-700"
                      : "bg-slate-800 text-slate-400 border-slate-700 hover:text-white"
                  }`}
                >
                  {deviceState.torch ? "ON" : "OFF"}
                </button>
              </div>

              {/* Screen Song #2 Card */}
              <div className="p-3 rounded-xl bg-[#13141f] border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-red-950 border border-red-800 flex items-center justify-center text-red-400">
                      <Play className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="font-semibold text-slate-200">On-Screen Song Selection</div>
                      <div className="text-[10px] text-slate-500">
                        "Dusre number wala song play karo"
                      </div>
                    </div>
                  </div>
                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded border ${
                      deviceState.isPlayingMedia
                        ? "bg-emerald-950 text-emerald-400 border-emerald-800"
                        : "bg-slate-800 text-slate-400 border-slate-700"
                    }`}
                  >
                    {deviceState.isPlayingMedia ? "PLAYING" : "IDLE"}
                  </span>
                </div>
                <div className="p-2 rounded bg-black/40 border border-slate-800/80 flex items-center justify-between text-[11px]">
                  <span className="text-slate-300">
                    Track #{deviceState.activeSongIndex}: <strong className="text-red-400">{deviceState.activeSongTitle}</strong>
                  </span>
                  <button
                    onClick={() => triggerDirectAction("play_screen_song", { song_number: 2 })}
                    className="text-[10px] px-2 py-0.5 rounded bg-red-950 hover:bg-red-900 text-red-300 border border-red-800 font-bold"
                  >
                    Play #2
                  </button>
                </div>
              </div>

              {/* Volume Slider Card */}
              <div className="p-3 rounded-xl bg-[#13141f] border border-slate-800 space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2 text-cyan-400 font-semibold">
                    <Volume2 className="w-3.5 h-3.5" />
                    <span>Media Volume</span>
                  </div>
                  <span className="font-bold text-white font-mono">{deviceState.volume}%</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={deviceState.volume}
                  onChange={(e) => triggerDirectAction("set_volume", { percent: Number(e.target.value) })}
                  className="w-full accent-cyan-500 cursor-pointer h-1.5 bg-slate-800 rounded-lg"
                />
              </div>

              {/* Brightness Slider Card */}
              <div className="p-3 rounded-xl bg-[#13141f] border border-slate-800 space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2 text-yellow-400 font-semibold">
                    <Sun className="w-3.5 h-3.5" />
                    <span>Screen Brightness</span>
                  </div>
                  <span className="font-bold text-white font-mono">{deviceState.brightness}%</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={deviceState.brightness}
                  onChange={(e) => triggerDirectAction("set_brightness", { percent: Number(e.target.value) })}
                  className="w-full accent-yellow-500 cursor-pointer h-1.5 bg-slate-800 rounded-lg"
                />
              </div>

              {/* Camera & Alarm Card */}
              <div className="grid grid-cols-2 gap-2">
                <div className="p-2.5 rounded-xl bg-[#13141f] border border-slate-800 space-y-1">
                  <div className="flex items-center gap-1.5 text-purple-400 text-[11px] font-semibold">
                    <Camera className="w-3 h-3" />
                    <span>Camera Shutter</span>
                  </div>
                  <div className="text-[10px] text-slate-400">
                    Mode: <strong className="text-white uppercase">{deviceState.cameraMode}</strong>
                  </div>
                  <div className="flex gap-1 pt-1">
                    <button
                      onClick={() => triggerDirectAction("take_camera_photo", { lens: "front" })}
                      className="flex-1 py-1 text-[9px] rounded bg-purple-950 text-purple-300 border border-purple-800 font-bold"
                    >
                      Front
                    </button>
                    <button
                      onClick={() => triggerDirectAction("take_camera_photo", { lens: "back" })}
                      className="flex-1 py-1 text-[9px] rounded bg-purple-950 text-purple-300 border border-purple-800 font-bold"
                    >
                      Back
                    </button>
                  </div>
                </div>

                <div className="p-2.5 rounded-xl bg-[#13141f] border border-slate-800 space-y-1">
                  <div className="flex items-center gap-1.5 text-emerald-400 text-[11px] font-semibold">
                    <Clock className="w-3 h-3" />
                    <span>Clock Alarm</span>
                  </div>
                  <div className="text-[10px] text-slate-400 font-mono">
                    Time: <strong className="text-white">{deviceState.activeAlarm}</strong>
                  </div>
                  <button
                    onClick={() => triggerDirectAction("set_alarm", { time_string: "07:30 AM" })}
                    className="w-full py-1 text-[9px] rounded bg-emerald-950 text-emerald-300 border border-emerald-800 font-bold mt-1"
                  >
                    Set 7:30 AM
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Last hardware command banner */}
          <div className="p-3 bg-black/60 rounded-xl border border-slate-800 text-[11px] space-y-1">
            <div className="text-slate-500 font-mono">LATEST VERIFIED HARDWARE ACTION:</div>
            <div className="text-emerald-400 font-bold font-mono truncate">
              &gt; {deviceState.lastCommandExecuted}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
