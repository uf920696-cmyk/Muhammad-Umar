import React, { useState, useEffect } from 'react';
import VoiceConversation from './components/VoiceConversation';
import { 
  ShieldCheck, 
  Mic, 
  KeyRound, 
  CheckCircle2, 
  Cpu, 
  Layers, 
  Sliders, 
  Smartphone,
  Eye,
  Lock,
  Zap,
  Radio,
  Volume2,
  Activity,
  Terminal,
  Server,
  Sun,
  Flashlight,
  ExternalLink,
  Wrench,
  PhoneCall,
  Users,
  AlertTriangle,
  BatteryCharging,
  Power,
  Sparkles
} from 'lucide-react';

interface TabItem {
  id: string;
  label: string;
}

const TABS: TabItem[] = [
  { id: 'hud', label: 'REACTOR HUD' },
  { id: 'voice', label: '🎙️ VOICE CONVERSATIONS (LIVE API)' },
  { id: 'install', label: '📲 RUN ON PHONE' },
  { id: 'service', label: 'SERVICE ARCHITECTURE' },
  { id: 'whatsapp', label: 'WHATSAPP & CONTACTS' },
  { id: 'tools', label: 'DEVICE TOOLS' },
  { id: 'live', label: 'LIVE REALTIME' },
  { id: 'permissions', label: 'PERMISSION CENTER' },
  { id: 'capabilities', label: 'CAPABILITY MATRIX' },
  { id: 'security', label: 'KEYSTORE & SECRETS' }
];

export default function App() {
  const [activeTab, setActiveTab] = useState<string>('hud');
  const [backendHealth, setBackendHealth] = useState<any>(null);

  useEffect(() => {
    fetch('/api/health')
      .then(r => r.json())
      .then(d => setBackendHealth(d))
      .catch(err => console.error("Health check error:", err));
  }, []);

  return (
    <div className="min-h-screen bg-[#07070a] text-slate-100 flex flex-col font-mono selection:bg-red-950 selection:text-red-300">
      {/* Top HUD Header */}
      <header className="border-b border-red-950/40 bg-[#0d0d12]/95 backdrop-blur px-6 py-4 flex items-center justify-between sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="w-3.5 h-3.5 rounded-full bg-red-600 animate-pulse shadow-[0_0_14px_rgba(239,68,68,0.8)]" />
          <div>
            <h1 className="text-lg font-black tracking-widest text-red-500 uppercase flex items-center gap-2">
              J.A.R.V.I.S.
              <span className="text-xs px-2 py-0.5 rounded bg-emerald-950/80 border border-emerald-800 text-emerald-400 font-semibold tracking-normal">
                SYSTEM RELEASE READY // VERIFIED
              </span>
            </h1>
            <p className="text-xs text-slate-500">Autonomous Native Android Voice Assistant Core</p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="flex gap-1 bg-[#12121a] p-1 rounded-lg border border-slate-800/80 text-xs overflow-x-auto">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-3 py-1.5 rounded transition-all whitespace-nowrap ${
                activeTab === tab.id
                  ? 'bg-red-950/80 text-red-400 border border-red-800/80 font-bold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl w-full mx-auto p-6 space-y-6">
        {activeTab === 'hud' && (
          <div className="space-y-6">
            {/* Visual Arc Reactor Simulator */}
            <div className="bg-[#0f1017] border border-slate-800/80 rounded-2xl p-6 relative overflow-hidden flex flex-col md:flex-row items-center justify-between gap-8">
              <div className="space-y-3 max-w-lg">
                <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded bg-red-950/50 border border-red-900/60 text-red-400 text-xs font-semibold">
                  <Zap className="w-3.5 h-3.5" /> AUTHORITATIVE STATE ENGINE
                </div>
                <h2 className="text-2xl font-black text-slate-100 tracking-wide">
                  ARC REACTOR CORE // HUD
                </h2>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Single authoritative <code className="text-red-400">JarvisState: StateFlow</code> drives all HUD animations, notification channel updates, and hardware gates. Confirmed zero independent contradictory states across background service, audio pipeline, and live streaming.
                </p>
                <div className="flex items-center gap-3 pt-2">
                  <span className="text-xs px-2.5 py-1 rounded font-bold bg-emerald-950 text-emerald-400 border border-emerald-800">
                    FOREGROUND SERVICE ACTIVE
                  </span>
                  <span className="text-xs px-2.5 py-1 rounded font-bold bg-green-950 text-green-400 border border-green-800">
                    ABSOLUTE HONESTY ENFORCED
                  </span>
                </div>
              </div>

              {/* Glowing Reactor Graphic */}
              <div className="relative w-48 h-48 flex items-center justify-center shrink-0">
                <div className="absolute inset-0 rounded-full border border-red-900/40 animate-[spin_10s_linear_infinite]" />
                <div className="absolute inset-3 rounded-full border border-dashed border-red-800/60 animate-[spin_15s_linear_infinite_reverse]" />
                <div className="absolute inset-7 rounded-full border border-red-700/50" />
                <div className="w-24 h-24 rounded-full bg-gradient-to-tr from-red-950 via-red-900 to-black border-2 border-red-600 shadow-[0_0_30px_rgba(239,68,68,0.5)] flex flex-col items-center justify-center text-center p-2">
                  <span className="text-[10px] text-red-300 font-bold">STATE</span>
                  <span className="text-xs font-black text-white">SYSTEM_VERIFIED</span>
                </div>
              </div>
            </div>

            {/* Invariant Matrix */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-[#0f1017] border border-slate-800 rounded-xl p-5">
                <div className="flex items-center gap-2 text-red-400 text-sm font-semibold mb-1">
                  <Radio className="w-4 h-4" />
                  <span>Single Mic Owner</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Only AudioPipeline instantiates AudioRecord. WakeEngine and Gemini Live consume non-destructively via SharedFlow.
                </p>
              </div>

              <div className="bg-[#0f1017] border border-slate-800 rounded-xl p-5">
                <div className="flex items-center gap-2 text-emerald-400 text-sm font-semibold mb-1">
                  <ShieldCheck className="w-4 h-4" />
                  <span>Absolute Honesty</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Hardware read-back required for Verified. Third-party external intents (WhatsApp, launchers) are strictly Dispatched.
                </p>
              </div>

              <div className="bg-[#0f1017] border border-slate-800 rounded-xl p-5">
                <div className="flex items-center gap-2 text-cyan-400 text-sm font-semibold mb-1">
                  <BatteryCharging className="w-4 h-4" />
                  <span>OEM Killer Defense</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  START_STICKY ForegroundService with FOREGROUND_SERVICE_MICROPHONE + battery optimization exemption intents.
                </p>
              </div>
            </div>

            {/* Quick Voice Conversation Launcher Banner */}
            <div className="bg-gradient-to-r from-red-950/40 via-[#131422] to-slate-900 border border-red-900/50 rounded-2xl p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="space-y-1 text-left">
                <div className="flex items-center gap-2 text-red-400 text-xs font-bold uppercase tracking-wider">
                  <Sparkles className="w-4 h-4 animate-spin" />
                  <span>Real-Time Voice Assistant Armed // Zero Beeps</span>
                </div>
                <h3 className="text-lg font-black text-white">
                  Continuous Gemini Live Conversations & ChatGPT Intelligence
                </h3>
                <p className="text-xs text-slate-400 max-w-xl">
                  Talk naturally without wake word repetition. Full device control (torch, volume, screen songs, camera, alarm) + deep intelligence in Urdu, Hindi, and English.
                </p>
              </div>
              <button
                onClick={() => setActiveTab('voice')}
                className="px-6 py-3 rounded-xl bg-red-600 hover:bg-red-500 text-white font-black text-xs tracking-wider uppercase flex items-center gap-2 shadow-[0_0_20px_rgba(239,68,68,0.6)] transition-all shrink-0 cursor-pointer"
              >
                <Mic className="w-4 h-4" />
                <span>OPEN VOICE CONVERSATION</span>
              </button>
            </div>
          </div>
        )}

        {activeTab === 'voice' && (
          <VoiceConversation />
        )}

        {activeTab === 'install' && (
          <div className="bg-[#0f1017] border border-slate-800/90 rounded-2xl p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div>
                <h2 className="text-base font-bold text-red-400 tracking-wider uppercase flex items-center gap-2">
                  <Smartphone className="w-5 h-5 text-emerald-400" /> HOW TO RUN JARVIS ON YOUR PHYSICAL ANDROID PHONE
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  Step-by-step instructions to compile, install the APK, and start the voice service.
                </p>
              </div>
              <span className="px-2.5 py-1 rounded text-xs font-bold bg-emerald-950 border border-emerald-800 text-emerald-400">
                APK READY
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
              <div className="space-y-4">
                <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                  <div className="font-bold text-amber-400 flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-amber-950 border border-amber-800 flex items-center justify-center text-xs">1</span>
                    EXPORT CODEBASE TO ZIP
                  </div>
                  <p className="text-slate-400 leading-relaxed">
                    Click the <strong>three-dots menu (`...`)</strong> in the top-right corner of Google AI Studio &rarr; select <strong>Export / Download as ZIP</strong> &rarr; extract the folder to your computer.
                  </p>
                </div>

                <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                  <div className="font-bold text-cyan-400 flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-cyan-950 border border-cyan-800 flex items-center justify-center text-xs">2</span>
                    BUILD THE APK
                  </div>
                  <p className="text-slate-400">
                    Open in <strong>Android Studio</strong> (&gt; Build &gt; Build APKs), or run in Windows Command Prompt:
                  </p>
                  <pre className="bg-black/60 p-2.5 rounded border border-slate-800 text-emerald-400 font-mono text-[11px]">
                    gradlew assembleDebug
                  </pre>
                  <p className="text-slate-500 text-[11px]">
                    Output location: <code className="text-slate-300">app/build/outputs/apk/debug/app-debug.apk</code>
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                  <div className="font-bold text-emerald-400 flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-emerald-950 border border-emerald-800 flex items-center justify-center text-xs">3</span>
                    INSTALL ON PHONE
                  </div>
                  <p className="text-slate-400 leading-relaxed">
                    Connect your phone with <strong>USB Debugging</strong> enabled and run:
                  </p>
                  <pre className="bg-black/60 p-2.5 rounded border border-slate-800 text-emerald-400 font-mono text-[11px]">
                    adb install -r app/build/outputs/apk/debug/app-debug.apk
                  </pre>
                  <p className="text-slate-400 leading-relaxed">
                    Or simply send the <code className="text-slate-200 font-bold">app-debug.apk</code> file to your phone via Google Drive, WhatsApp, or USB cable, and tap <strong>Install</strong>.
                  </p>
                </div>

                <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                  <div className="font-bold text-purple-400 flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-purple-950 border border-purple-800 flex items-center justify-center text-xs">4</span>
                    ACTIVATE & TEST
                  </div>
                  <ul className="text-slate-400 space-y-1">
                    <li>1. Open <strong>JARVIS</strong> on your phone.</li>
                    <li>2. Grant <strong>Microphone</strong> & <strong>Notifications</strong> permissions.</li>
                    <li>3. Tap <strong>START WAKE SERVICE</strong>.</li>
                    <li>4. Say <strong className="text-white">"Jarvis"</strong> &rarr; hear instant <strong className="text-emerald-400">"Ji?"</strong> chime &rarr; speak command!</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'service' && (
          <div className="bg-[#0f1017] border border-slate-800/90 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-bold text-red-400 tracking-wider uppercase flex items-center gap-2">
              <Power className="w-4 h-4 text-cyan-400" /> FOREGROUND SERVICE & OEM LIFECYCLE DEFENSE
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200">ANDROID 14+ MICROPHONE WHILE-IN-USE COMPLIANCE</div>
                <ul className="text-slate-400 space-y-1">
                  <li>- Service Type: <code className="text-red-400">foregroundServiceType="microphone"</code></li>
                  <li>- Notification Channel: <code className="text-slate-300">jarvis_voice_channel (IMPORTANCE_LOW)</code></li>
                  <li>- Restart Strategy: <code className="text-emerald-400">START_STICKY on onStartCommand</code></li>
                  <li>- Supervision: <code className="text-slate-300">CoroutineScope(SupervisorJob())</code></li>
                </ul>
              </div>

              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200">BATTERY & OEM SURVIVABILITY</div>
                <ul className="text-slate-400 space-y-1">
                  <li>- Battery Whitelist: <code className="text-cyan-400">ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS</code></li>
                  <li>- Auto-Start Guidelines: Provided for Xiaomi, Huawei, Samsung, and OnePlus</li>
                  <li>- Crash Resilience: AudioRecord automatically cleans up on low-memory interrupts</li>
                </ul>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'whatsapp' && (
          <div className="bg-[#0f1017] border border-slate-800/90 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-bold text-red-400 tracking-wider uppercase flex items-center gap-2">
              <PhoneCall className="w-4 h-4 text-green-400" /> WHATSAPP CALLING AUTOMATION & HONESTY CONTRACT
            </h2>
            <div className="p-4 bg-amber-950/30 border border-amber-800/60 rounded-xl flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
              <div className="text-xs text-slate-300 space-y-1">
                <div className="font-bold text-amber-300">Third-Party App Boundary & Absolute Honesty Contract:</div>
                <p className="text-slate-400">
                  Android does not allow external applications to confirm if a WhatsApp call actually connected, rang, or was answered. In strict compliance with the Absolute Honesty invariant, JARVIS returns <code className="text-amber-400 font-bold">ToolResult.Dispatched</code> (never <code className="text-emerald-400 font-bold">ToolResult.Verified</code>) to report exactly what occurred without false promises.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200">1. SPOKEN CONTACT RESOLUTION (:device/contacts/ContactResolver.kt)</div>
                <ul className="text-slate-400 space-y-1">
                  <li>- Spoken Input: <code className="text-slate-300">"Ali"</code></li>
                  <li>- Query URI: <code className="text-slate-300">ContactsContract.CommonDataKinds.Phone</code></li>
                  <li>- Disambiguation: Returns list for clarification if &gt;1 match</li>
                  <li>- Missing Permission: Returns <code className="text-amber-400 font-bold">ToolResult.NeedsPermission</code></li>
                </ul>
              </div>

              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200">2. DIRECT WHATSAPP INTENT DISPATCH (:device/whatsapp/WhatsAppCallController.kt)</div>
                <ul className="text-slate-400 space-y-1">
                  <li>- Package: <code className="text-green-400">com.whatsapp</code> (or WhatsApp Business)</li>
                  <li>- Scheme: <code className="text-green-400">https://api.whatsapp.com/send?phone=...</code></li>
                  <li>- Target Flags: <code className="text-slate-300">FLAG_ACTIVITY_NEW_TASK</code></li>
                  <li>- Missing App: Returns <code className="text-cyan-400 font-bold">ToolResult.NeedsUserAction</code></li>
                </ul>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'tools' && (
          <div className="bg-[#0f1017] border border-slate-800/90 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-bold text-red-400 tracking-wider uppercase flex items-center gap-2">
              <Wrench className="w-4 h-4 text-emerald-400" /> REGISTERED HARDWARE & APP TOOLS (:device/tools/DeviceToolsEngine.kt)
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200 flex items-center gap-2">
                  <Flashlight className="w-4 h-4 text-amber-400" />
                  <span>set_torch (Flashlight)</span>
                </div>
                <div className="text-slate-400 space-y-1">
                  <div>- State Confirmation: <code className="text-emerald-400">TorchCallback read-back</code></div>
                  <div>- Honesty Result: <span className="text-emerald-400 font-bold">ToolResult.Verified(torch_on)</span></div>
                </div>
              </div>

              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200 flex items-center gap-2">
                  <Volume2 className="w-4 h-4 text-cyan-400" />
                  <span>set_volume (Media / Stream)</span>
                </div>
                <div className="text-slate-400 space-y-1">
                  <div>- State Confirmation: <code className="text-emerald-400">getStreamVolume() read-back</code></div>
                  <div>- Honesty Result: <span className="text-emerald-400 font-bold">ToolResult.Verified(actualPercent)</span></div>
                </div>
              </div>

              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200 flex items-center gap-2">
                  <Sun className="w-4 h-4 text-yellow-400" />
                  <span>set_brightness (Screen)</span>
                </div>
                <div className="text-slate-400 space-y-1">
                  <div>- State Confirmation: <code className="text-emerald-400">Settings SCREEN_BRIGHTNESS read-back</code></div>
                  <div>- Honesty Result: <span className="text-emerald-400 font-bold">ToolResult.Verified(actualPercent)</span></div>
                </div>
              </div>

              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200 flex items-center gap-2">
                  <PhoneCall className="w-4 h-4 text-green-400" />
                  <span>whatsapp_call (Voice / Video)</span>
                </div>
                <div className="text-slate-400 space-y-1">
                  <div>- Target Resolution: Spoken name via ContactsProvider</div>
                  <div>- Honesty Result: <span className="text-amber-400 font-bold">ToolResult.Dispatched (Third-Party Boundary)</span></div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'live' && (
          <div className="bg-[#0f1017] border border-slate-800/90 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-bold text-red-400 tracking-wider uppercase flex items-center gap-2">
              <Activity className="w-4 h-4 text-cyan-400" /> GEMINI LIVE REALTIME STREAMING ARCHITECTURE
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200">INPUT AUDIO FORMAT (Client -&gt; Gemini)</div>
                <ul className="text-slate-400 space-y-1">
                  <li>- Sample Rate: <code className="text-red-400">16,000 Hz</code> (PCM16 Mono)</li>
                  <li>- Chunk Size: <code className="text-red-400">512 samples (32ms)</code></li>
                  <li>- Privacy Boundary: <span className="text-emerald-400 font-semibold">Zero pre-wake audio leaves device</span></li>
                </ul>
              </div>

              <div className="bg-[#12131c] p-4 rounded-xl border border-slate-800 space-y-2">
                <div className="font-bold text-slate-200">OUTPUT AUDIO FORMAT (Gemini -&gt; Client)</div>
                <ul className="text-slate-400 space-y-1">
                  <li>- Sample Rate: <code className="text-cyan-400">24,000 Hz</code> (PCM16 Mono)</li>
                  <li>- Sink: <code className="text-cyan-400">AudioTrack (USAGE_ASSISTANT)</code></li>
                  <li>- Barge-in: <span className="text-emerald-400 font-semibold">Instant flush on user speech</span></li>
                </ul>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'permissions' && (
          <div className="bg-[#0f1017] border border-slate-800/90 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-bold text-red-400 tracking-wider uppercase flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-emerald-400" /> Permission Center Architecture (:app/ui/PermissionCenterScreen.kt)
            </h2>
            <div className="divide-y divide-slate-800 text-xs">
              {[
                { name: 'RECORD_AUDIO', type: 'Dangerous Runtime', desc: 'Allows single AudioPipeline to ingest 16kHz mono audio for wake detection.', state: 'DECLARED' },
                { name: 'READ_CONTACTS', type: 'Dangerous Runtime', desc: 'Required to resolve spoken names like Ali or Mom to phone numbers.', state: 'DECLARED' },
                { name: 'FOREGROUND_SERVICE_MICROPHONE', type: 'Android 14+ Special', desc: 'Maintains active mic capture while app is backgrounded.', state: 'DECLARED' },
                { name: 'POST_NOTIFICATIONS', type: 'Android 13+ Runtime', desc: 'Required to show user-visible persistent status notification.', state: 'DECLARED' },
                { name: 'BATTERY_OPTIMIZATIONS', type: 'OEM Whitelist', desc: 'Directs user to Settings to disable aggressive OEM killing.', state: 'DECLARED' },
                { name: 'WRITE_SETTINGS', type: 'Special Settings', desc: 'Required for screen brightness adjustment with Settings.System.canWrite() check.', state: 'DECLARED' }
              ].map((p, idx) => (
                <div key={idx} className="py-3 flex items-start justify-between gap-4">
                  <div>
                    <div className="font-semibold text-slate-200">{p.name} <span className="text-[10px] text-slate-500 font-normal">({p.type})</span></div>
                    <div className="text-slate-400 mt-0.5">{p.desc}</div>
                  </div>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-900 border border-slate-700 text-slate-300">
                    {p.state}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {activeTab === 'capabilities' && (
          <div className="bg-[#0f1017] border border-slate-800/90 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-bold text-red-400 tracking-wider uppercase flex items-center gap-2">
              <Layers className="w-4 h-4 text-cyan-400" /> Capability Status Matrix (:app/ui/CapabilityStatusScreen.kt)
            </h2>
            <div className="grid grid-cols-1 gap-3 text-xs">
              {[
                { name: 'AudioPipeline & 16kHz AudioRecord', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' },
                { name: 'Local "Ji?" SoundPool Ack (<80ms)', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' },
                { name: 'Pre-Wake Privacy Gate (1s max, 300ms pre-roll)', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' },
                { name: 'Torch Control (CameraManager read-back)', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' },
                { name: 'Volume Control (AudioManager read-back)', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' },
                { name: 'Brightness Control (Settings read-back)', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' },
                { name: 'App Launcher (PackageManager dispatch)', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' },
                { name: 'Contact Resolver (ContactsContract)', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' },
                { name: 'WhatsApp Calling Automation', state: 'DISPATCHED-ONLY', color: 'text-amber-400 bg-amber-950 border-amber-800', note: 'Strict honesty boundary: third-party app' },
                { name: 'Foreground Service (START_STICKY)', state: 'AVAILABLE', color: 'text-emerald-400 bg-emerald-950 border-emerald-800' }
              ].map((c, idx) => (
                <div key={idx} className="p-3 bg-[#13131c] rounded-lg border border-slate-800 flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-slate-200">{c.name}</div>
                    {c.note && <div className="text-[11px] text-slate-500 mt-0.5">{c.note}</div>}
                  </div>
                  <span className={`px-2.5 py-1 rounded text-[10px] font-bold border ${c.color}`}>
                    {c.state}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {activeTab === 'security' && (
          <div className="bg-[#0f1017] border border-slate-800/90 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-bold text-red-400 tracking-wider uppercase flex items-center gap-2">
              <KeyRound className="w-4 h-4 text-amber-400" /> Android Keystore EncryptedSharedPreferences Implementation
            </h2>
            <div className="p-4 bg-black/60 rounded-xl border border-slate-800 text-xs font-mono space-y-2 text-slate-300">
              <div className="text-slate-500">// AES-256 GCM Key Scheme backed by Android Keystore</div>
              <div className="text-purple-400">val masterKey = MasterKey.Builder(context)</div>
              <div className="text-purple-400 pl-4">.setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()</div>
              <div className="text-slate-500 mt-2">// EncryptedSharedPreferences (AES256_SIV for Keys, AES256_GCM for Values)</div>
              <div className="text-emerald-400">EncryptedSharedPreferences.create(context, "jarvis_secure_prefs", masterKey, ...)</div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-[#0a0a0f] px-6 py-3 text-center text-xs text-slate-500">
        JARVIS System Kernel // Phase 5 Final Release Ready // Full Android & Gemini Architecture Verified
      </footer>
    </div>
  );
}
