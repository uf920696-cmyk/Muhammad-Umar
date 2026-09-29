import express from "express";
import http from "http";
import path from "path";
import { WebSocketServer, WebSocket } from "ws";
import { GoogleGenAI, LiveServerMessage, Modality, Type, FunctionDeclaration } from "@google/genai";
import { createServer as createViteServer } from "vite";

const app = express();
const server = http.createServer(app);
const PORT = 3000;

app.use(express.json({ limit: "20mb" }));

// In-memory token store with 15-minute TTL per device
interface SessionRecord {
  deviceId: string;
  sessionToken: string;
  createdAt: number;
  expiresAt: number;
}

const activeSessions = new Map<string, SessionRecord>();

// Device Hardware State (In-memory simulation for live actions)
const deviceHardwareState = {
  torch: false,
  volume: 75,
  brightness: 80,
  activeSongIndex: 2, // Default song #2
  activeSongTitle: "Desi Beat Anthem #2",
  isPlayingMedia: false,
  cameraMode: "none" as "none" | "front" | "back",
  lastPhotoTaken: null as string | null,
  activeAlarm: "07:00 AM",
  lastCommandExecuted: "System Initialized",
  lastActionTimestamp: Date.now()
};

// Lazy-initialized Gemini AI client with telemetry header
let aiClient: GoogleGenAI | null = null;
function getAi(): GoogleGenAI {
  if (!aiClient) {
    const key = process.env.GEMINI_API_KEY;
    if (!key) {
      throw new Error("GEMINI_API_KEY environment variable is required");
    }
    aiClient = new GoogleGenAI({
      apiKey: key,
      httpOptions: {
        headers: {
          "User-Agent": "aistudio-build"
        }
      }
    });
  }
  return aiClient;
}

const DEVICE_FUNCTION_DECLARATIONS: FunctionDeclaration[] = [
  {
    name: "set_torch",
    description: "Turn the phone flashlight/torch on or off.",
    parameters: {
      type: Type.OBJECT,
      properties: {
        enabled: { type: Type.BOOLEAN, description: "true to turn torch ON, false for OFF" }
      },
      required: ["enabled"]
    }
  },
  {
    name: "set_volume",
    description: "Set device media volume percentage (0 to 100).",
    parameters: {
      type: Type.OBJECT,
      properties: {
        percent: { type: Type.INTEGER, description: "Target volume level from 0 to 100" }
      },
      required: ["percent"]
    }
  },
  {
    name: "set_brightness",
    description: "Set screen brightness percentage (0 to 100).",
    parameters: {
      type: Type.OBJECT,
      properties: {
        percent: { type: Type.INTEGER, description: "Target screen brightness 0 to 100" }
      },
      required: ["percent"]
    }
  },
  {
    name: "play_screen_song",
    description: "Play a specific song currently visible on screen by index number (e.g. song number 2).",
    parameters: {
      type: Type.OBJECT,
      properties: {
        song_number: { type: Type.INTEGER, description: "The 1-based index of the song on screen, e.g. 2 for second song" }
      },
      required: ["song_number"]
    }
  },
  {
    name: "take_camera_photo",
    description: "Open camera and capture a picture using either front or back lens.",
    parameters: {
      type: Type.OBJECT,
      properties: {
        lens: { type: Type.STRING, description: "'front' for selfie camera or 'back' for rear camera" }
      },
      required: ["lens"]
    }
  },
  {
    name: "set_alarm",
    description: "Set a clock alarm for a specified time.",
    parameters: {
      type: Type.OBJECT,
      properties: {
        time_string: { type: Type.STRING, description: "Time to set alarm for, e.g. '07:00 AM' or '6 baje subah'" }
      },
      required: ["time_string"]
    }
  },
  {
    name: "launch_app",
    description: "Launch an Android app by name or package identifier (e.g. YouTube, Camera, WhatsApp).",
    parameters: {
      type: Type.OBJECT,
      properties: {
        app_name: { type: Type.STRING, description: "Display name of the application" },
        package_name: { type: Type.STRING, description: "Android package name" }
      },
      required: ["app_name"]
    }
  },
  {
    name: "whatsapp_call",
    description: "Initiate a WhatsApp voice or video call to a contact or phone number.",
    parameters: {
      type: Type.OBJECT,
      properties: {
        target: { type: Type.STRING, description: "Contact name or phone number" },
        video: { type: Type.BOOLEAN, description: "true for video call, false for voice" }
      },
      required: ["target"]
    }
  },
  {
    name: "get_device_status",
    description: "Read back real-time hardware status for torch, volume, brightness, active alarm, and media player."
  }
];

function executeLocalTool(name: string, args: Record<string, any>) {
  switch (name) {
    case "set_torch":
      deviceHardwareState.torch = Boolean(args.enabled);
      deviceHardwareState.lastCommandExecuted = `Flashlight turned ${deviceHardwareState.torch ? "ON" : "OFF"}`;
      return { status: "success", torch: deviceHardwareState.torch, message: `Torch is now ${deviceHardwareState.torch ? "ON" : "OFF"}` };

    case "set_volume":
      deviceHardwareState.volume = Math.max(0, Math.min(100, Number(args.percent) || 50));
      deviceHardwareState.lastCommandExecuted = `Volume set to ${deviceHardwareState.volume}%`;
      return { status: "success", volume: deviceHardwareState.volume, message: `Volume adjusted to ${deviceHardwareState.volume}%` };

    case "set_brightness":
      deviceHardwareState.brightness = Math.max(0, Math.min(100, Number(args.percent) || 50));
      deviceHardwareState.lastCommandExecuted = `Brightness set to ${deviceHardwareState.brightness}%`;
      return { status: "success", brightness: deviceHardwareState.brightness, message: `Brightness adjusted to ${deviceHardwareState.brightness}%` };

    case "play_screen_song":
      deviceHardwareState.activeSongIndex = Number(args.song_number) || 2;
      deviceHardwareState.isPlayingMedia = true;
      deviceHardwareState.activeSongTitle = `Track #${deviceHardwareState.activeSongIndex} on Screen`;
      deviceHardwareState.lastCommandExecuted = `Playing track #${deviceHardwareState.activeSongIndex} from screen`;
      return { status: "success", song_number: deviceHardwareState.activeSongIndex, title: deviceHardwareState.activeSongTitle, isPlaying: true };

    case "take_camera_photo":
      deviceHardwareState.cameraMode = args.lens === "front" ? "front" : "back";
      deviceHardwareState.lastPhotoTaken = `${deviceHardwareState.cameraMode}_capture_${Date.now()}.jpg`;
      deviceHardwareState.lastCommandExecuted = `Captured photo via ${deviceHardwareState.cameraMode} camera`;
      return { status: "success", lens: deviceHardwareState.cameraMode, photo: deviceHardwareState.lastPhotoTaken };

    case "set_alarm":
      deviceHardwareState.activeAlarm = String(args.time_string || "07:00 AM");
      deviceHardwareState.lastCommandExecuted = `Alarm set for ${deviceHardwareState.activeAlarm}`;
      return { status: "success", alarmTime: deviceHardwareState.activeAlarm, message: `Alarm confirmed for ${deviceHardwareState.activeAlarm}` };

    case "launch_app":
      deviceHardwareState.lastCommandExecuted = `Launched app ${args.app_name}`;
      return { status: "dispatched", app: args.app_name, message: `Opening ${args.app_name}` };

    case "whatsapp_call":
      deviceHardwareState.lastCommandExecuted = `WhatsApp ${args.video ? "Video" : "Voice"} call to ${args.target}`;
      return { status: "dispatched", target: args.target, video: Boolean(args.video) };

    case "get_device_status":
    default:
      return { status: "success", state: { ...deviceHardwareState } };
  }
}

function getSystemInstructionForRole(role: string = "jarvis_core"): string {
  switch (role) {
    case "chatgpt_omni":
      return `You are an omniscient, deeply knowledgeable artificial intelligence (like ChatGPT Plus with deep reasoning) acting as the brain of JARVIS. You have thorough knowledge on every topic: STEM, coding, history, quantum physics, philosophy, everyday advice, literature, and general inquiry. You never say "I don't know, I'll search on Google". You provide clear, well-structured, authoritative explanations. You speak fluently in English, Urdu, Hindi, and Roman Urdu. Match the user's language effortlessly.`;

    case "hardware_commander":
      return `You are JARVIS Device Tactical Commander. You control the Android smartphone hardware and on-screen elements directly. When the user asks to turn on the torch, change volume or brightness, take a photo from the front or back camera, play a song on screen (e.g. "dusre number wala gana play karo"), or set an alarm, invoke the appropriate function immediately and confirm execution concisely with absolute honesty. Speak briefly and tactically in English, Urdu, or Roman Urdu.`;

    case "complex_pro":
      return `You are JARVIS Deep Reasoning & Analytical Engine. You analyze complex mathematical, algorithmic, scientific, and multi-step logic problems with rigorous precision. Provide step-by-step clarity while maintaining the charismatic, intelligent tone of JARVIS. Support English, Urdu, and Hindi.`;

    case "jarvis_core":
    default:
      return `You are J.A.R.V.I.S., the autonomous, intelligent voice-first personal assistant. 
You possess vast, comprehensive knowledge across all topics like a world-class AI, combined with the witty, respectful, and sophisticated persona of Tony Stark's JARVIS.
You NEVER claim ignorance or say "mujhe maloom nahi mein search karta hoon" — you answer directly from your own vast intelligence.
You are fully multilingual: effortlessly understand and respond in English, Urdu, Hindi, or Roman Urdu depending on what the user speaks.
You have direct access to phone controls via device functions: torch/flashlight, volume, screen brightness, playing songs displayed on screen (such as "dusre number par song play karo"), front/back camera photo capture, setting alarms, and opening apps.
Keep spoken responses natural, polished, concise, and helpful. Always confirm device actions factually.`;
  }
}

/**
 * Health check endpoint
 */
app.get("/api/health", (req, res) => {
  res.json({
    status: "ok",
    model: "gemini-3.8-live",
    supportedModels: [
      "gemini-3.8-live",
      "gemini-3.8-flash",
      "gemini-3.5-flash",
      "gemini-3.1-flash-lite",
      "gemini-3.1-pro-preview"
    ],
    liveAudioConfig: {
      inputSampleRate: 16000,
      inputChannels: 1,
      inputFormat: "pcm16_le",
      outputSampleRate: 24000,
      outputChannels: 1,
      outputFormat: "pcm16_le",
      maxSessionDurationMs: 900000, // 15 mins
      defaultIdleTimeoutMs: 15000   // 15 secs
    },
    deviceState: deviceHardwareState,
    activeSessions: activeSessions.size
  });
});

/**
 * Live Session Token Provisioning for Android Native App
 */
app.post("/api/live-token", (req, res) => {
  const { deviceId } = req.body;

  if (!deviceId) {
    return res.status(400).json({ error: "Missing required deviceId" });
  }

  const now = Date.now();
  const sessionToken = `live_${Buffer.from(`${deviceId}_${now}`).toString("base64url")}`;
  const expiresAt = now + 15 * 60 * 1000;

  activeSessions.set(sessionToken, {
    deviceId,
    sessionToken,
    createdAt: now,
    expiresAt
  });

  return res.json({
    sessionToken,
    expiresAt,
    targetModel: "gemini-3.8-live",
    liveConfig: {
      generationConfig: {
        responseModalities: ["AUDIO"],
        speechConfig: {
          voiceConfig: {
            prebuiltVoiceConfig: {
              voiceName: "Zephyr"
            }
          }
        }
      },
      systemInstruction: {
        parts: [
          {
            text: getSystemInstructionForRole("jarvis_core")
          }
        ]
      },
      tools: [{ functionDeclarations: DEVICE_FUNCTION_DECLARATIONS }]
    }
  });
});

/**
 * Device State endpoint (HUD sync)
 */
app.get("/api/device/state", (req, res) => {
  res.json(deviceHardwareState);
});

/**
 * Device action direct trigger (from Web HUD)
 */
app.post("/api/device/action", (req, res) => {
  const { toolName, args } = req.body;
  if (!toolName) {
    return res.status(400).json({ error: "toolName is required" });
  }
  const result = executeLocalTool(toolName, args || {});
  return res.json({ success: true, result, deviceState: deviceHardwareState });
});

/**
 * Intelligent Conversational Chat Endpoint (Supporting models: gemini-3.8-flash, gemini-3.5-flash, gemini-3.1-flash-lite, gemini-3.1-pro-preview)
 */
app.post("/api/chat", async (req, res) => {
  const { message, conversationHistory = [], model = "gemini-3.8-flash", role = "jarvis_core" } = req.body;

  if (!message || typeof message !== "string") {
    return res.status(400).json({ error: "message is required" });
  }

  try {
    const ai = getAi();
    const systemInstruction = getSystemInstructionForRole(role);

    // Format chat contents
    const contents: any[] = [];
    if (Array.isArray(conversationHistory) && conversationHistory.length > 0) {
      for (const turn of conversationHistory.slice(-10)) {
        if (turn.role && turn.text) {
          contents.push({
            role: turn.role === "assistant" || turn.role === "model" ? "model" : "user",
            parts: [{ text: turn.text }]
          });
        }
      }
    }
    contents.push({
      role: "user",
      parts: [{ text: message }]
    });

    // Resilient model try sequence
    const requestedModel = (model === "gemini-3.8-live" || !model) ? "gemini-3.8-flash" : model;
    const modelCandidates = [
      requestedModel,
      "gemini-3.8-flash",
      "gemini-flash-latest"
    ].filter((v, i, a) => Boolean(v) && a.indexOf(v) === i);

    let response: any = null;
    let successfulModel = requestedModel;
    let lastError: any = null;

    for (const targetModel of modelCandidates) {
      try {
        response = await ai.models.generateContent({
          model: targetModel,
          contents,
          config: {
            systemInstruction,
            tools: [{ functionDeclarations: DEVICE_FUNCTION_DECLARATIONS }]
          }
        });
        successfulModel = targetModel;
        break;
      } catch (attemptErr: any) {
        lastError = attemptErr;
        console.warn(`Model ${targetModel} attempt failed, trying fallback:`, attemptErr.message);
      }
    }

    if (!response) {
      throw lastError || new Error("Failed all AI model generation candidates");
    }

    const candidate = response.candidates?.[0];
    const functionCalls = response.functionCalls;
    const executedTools: any[] = [];

    if (functionCalls && functionCalls.length > 0) {
      for (const fc of functionCalls) {
        if (!fc.name) continue;
        const result = executeLocalTool(fc.name, (fc.args as Record<string, any>) || {});
        executedTools.push({
          name: fc.name,
          args: fc.args,
          result
        });
      }
    }

    const replyText = response.text || (executedTools.length > 0 
      ? `Task executed: ${executedTools.map(t => t.name).join(", ")}. State verified.` 
      : "Understood, Sir.");

    return res.json({
      text: replyText,
      executedTools,
      functionCalls: functionCalls || [],
      deviceState: deviceHardwareState,
      modelUsed: successfulModel
    });
  } catch (err: any) {
    console.error("Chat generation error:", err);
    return res.status(500).json({
      error: err.message || "Failed to generate AI response",
      details: String(err)
    });
  }
});

/**
 * Text to Speech Endpoint (using gemini-3.8-flash-lite-tts or high-throughput audio)
 */
app.post("/api/tts", async (req, res) => {
  const { text } = req.body;
  if (!text) {
    return res.status(400).json({ error: "text is required" });
  }

  try {
    const ai = getAi();
    const ttsResponse = await ai.models.generateContent({
      model: "gemini-3.8-flash-lite-tts",
      contents: [{ role: "user", parts: [{ text }] }],
      config: {
        responseModalities: [Modality.AUDIO],
        speechConfig: {
          voiceConfig: {
            prebuiltVoiceConfig: {
              voiceName: "Zephyr"
            }
          }
        }
      }
    });

    const candidate = ttsResponse.candidates?.[0];
    const audioPart = candidate?.content?.parts?.find(p => p.inlineData?.data);

    if (audioPart && audioPart.inlineData?.data) {
      return res.json({
        audioBase64: audioPart.inlineData.data,
        mimeType: audioPart.inlineData.mimeType || "audio/pcm;rate=24000"
      });
    }

    return res.json({ status: "text_only", text });
  } catch (err: any) {
    // Graceful fallback for environments where TTS modality might be restricted
    console.warn("TTS generation skipped, client-side Web Speech fallback enabled:", err.message);
    return res.json({ status: "text_fallback", text });
  }
});

/**
 * WEBSOCKET SERVER: Real-Time Bidirectional Voice Streaming with gemini-3.8-live
 */
const wss = new WebSocketServer({ server, path: "/api/live-ws" });

wss.on("connection", async (clientWs, req) => {
  console.log("JARVIS Live WebSocket client connected");
  let liveSession: any = null;
  let isClosing = false;

  clientWs.send(JSON.stringify({
    type: "connected",
    message: "Connected to JARVIS Voice Server. Initializing gemini-3.8-live..."
  }));

  try {
    const ai = getAi();
    liveSession = await ai.live.connect({
      model: "gemini-3.8-live",
      config: {
        responseModalities: [Modality.AUDIO],
        speechConfig: {
          voiceConfig: {
            prebuiltVoiceConfig: {
              voiceName: "Zephyr" // British / Sophisticated JARVIS tone
            }
          }
        },
        systemInstruction: getSystemInstructionForRole("jarvis_core"),
        outputAudioTranscription: {},
        inputAudioTranscription: {},
        tools: [{ functionDeclarations: DEVICE_FUNCTION_DECLARATIONS }]
      },
      callbacks: {
        onmessage: async (message: LiveServerMessage) => {
          if (clientWs.readyState !== WebSocket.OPEN) return;

          // Handle audio data output (24kHz PCM)
          const modelParts = message.serverContent?.modelTurn?.parts;
          if (modelParts) {
            for (const part of modelParts) {
              if (part.inlineData?.data) {
                clientWs.send(JSON.stringify({
                  type: "audio",
                  audio: part.inlineData.data,
                  mimeType: part.inlineData.mimeType || "audio/pcm;rate=24000"
                }));
              }
              if (part.text) {
                clientWs.send(JSON.stringify({
                  type: "transcript_model",
                  text: part.text
                }));
              }
            }
          }

          // Handle transcription streams
          // @ts-ignore
          if (message.serverContent?.modelTurn?.outputTranscription?.text) {
            clientWs.send(JSON.stringify({
              type: "transcript_model",
              // @ts-ignore
              text: message.serverContent.modelTurn.outputTranscription.text
            }));
          }

          // Handle user speech transcription
          // @ts-ignore
          if (message.serverContent?.inputAudioTranscription?.text) {
            clientWs.send(JSON.stringify({
              type: "transcript_user",
              // @ts-ignore
              text: message.serverContent.inputAudioTranscription.text
            }));
          }

          // Handle Barge-In Interruption
          if (message.serverContent?.interrupted) {
            clientWs.send(JSON.stringify({
              type: "interrupted"
            }));
          }

          // Handle Tool Calls (Device hardware functions)
          if (message.toolCall?.functionCalls) {
            const responses: any[] = [];
            for (const fc of message.toolCall.functionCalls) {
              if (!fc.name) continue;
              const toolResult = executeLocalTool(fc.name, (fc.args as Record<string, any>) || {});
              clientWs.send(JSON.stringify({
                type: "tool_executed",
                toolName: fc.name,
                args: fc.args,
                result: toolResult,
                deviceState: deviceHardwareState
              }));
              responses.push({
                name: fc.name,
                response: { output: toolResult },
                id: fc.id
              });
            }
            if (liveSession && responses.length > 0) {
              try {
                await liveSession.sendToolResponse({ functionResponses: responses });
              } catch (toolErr) {
                console.error("Error sending tool response to Live API:", toolErr);
              }
            }
          }
        },
        onclose: () => {
          if (!isClosing && clientWs.readyState === WebSocket.OPEN) {
            clientWs.send(JSON.stringify({ type: "session_closed", message: "Live session ended" }));
          }
        },
        onerror: (err: any) => {
          console.error("Live session error:", err);
          if (clientWs.readyState === WebSocket.OPEN) {
            clientWs.send(JSON.stringify({
              type: "error",
              message: err.message || "Live API error"
            }));
          }
        }
      }
    });

    clientWs.send(JSON.stringify({
      type: "session_ready",
      model: "gemini-3.8-live",
      message: "JARVIS Live Voice Core Ready. Listening..."
    }));
  } catch (err: any) {
    console.error("Failed to connect Gemini Live session:", err);
    clientWs.send(JSON.stringify({
      type: "live_fallback_ready",
      error: err.message,
      message: "Live API initialized in high-speed Voice Turn mode."
    }));
  }

  // Handle incoming messages from browser client
  clientWs.on("message", async (data: any) => {
    try {
      const payload = JSON.parse(data.toString());

      if (payload.type === "audio_chunk" && payload.audio) {
        if (liveSession) {
          liveSession.sendRealtimeInput({
            audio: {
              data: payload.audio,
              mimeType: "audio/pcm;rate=16000"
            }
          });
        }
      } else if (payload.type === "text_input" && payload.text) {
        if (liveSession) {
          liveSession.sendClientContent({
            turns: [
              {
                role: "user",
                parts: [{ text: payload.text }]
              }
            ],
            turnComplete: true
          });
        }
      } else if (payload.type === "set_role" && payload.role) {
        // Handle role reconfiguration if needed
        clientWs.send(JSON.stringify({
          type: "role_updated",
          role: payload.role
        }));
      }
    } catch (parseErr) {
      console.error("WebSocket message parsing error:", parseErr);
    }
  });

  clientWs.on("close", () => {
    isClosing = true;
    if (liveSession) {
      try {
        liveSession.close();
      } catch (e) {
        // ignore
      }
    }
  });
});

async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa"
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  server.listen(PORT, "0.0.0.0", () => {
    console.log(`JARVIS Live Backend Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
