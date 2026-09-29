import http from "http";

function sendChat(message, conversationHistory = []) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify({
      message,
      conversationHistory,
      model: "gemini-3.8-flash",
      role: "jarvis_core"
    });

    const req = http.request({
      hostname: "localhost",
      port: 3000,
      path: "/api/chat",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(payload)
      },
      timeout: 30000
    }, (res) => {
      let data = "";
      res.on("data", (chunk) => data += chunk);
      res.on("end", () => {
        try {
          const parsed = JSON.parse(data);
          resolve({ status: res.statusCode, body: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, raw: data });
        }
      });
    });

    req.on("error", reject);
    req.on("timeout", () => {
      req.destroy();
      reject(new Error("Timeout"));
    });

    req.write(payload);
    req.end();
  });
}

async function runTests() {
  console.log("=== RUNNING REAL AI AGENT BRAIN TESTS ===\n");

  // Test 1: Pakistan Prime Minister (Question that previously failed with hardcoded 'bohot dilchasp sawal')
  console.log("Test 1: 'Pakistan ka Prime Minister kon hai?'");
  const t1 = await sendChat("Pakistan ka Prime Minister kon hai?");
  console.log("Status:", t1.status);
  console.log("Model Used:", t1.body?.modelUsed);
  console.log("Response:", t1.body?.text);
  console.log("------------------------------------------\n");

  // Test 2: Conceptual Explanation in Roman Urdu (Inflation)
  console.log("Test 2: 'Mujhe samjhao inflation kya hoti hai.'");
  const t2 = await sendChat("Mujhe samjhao inflation kya hoti hai.");
  console.log("Response:", t2.body?.text?.slice(0, 200) + "...");
  console.log("------------------------------------------\n");

  // Test 3: Multi-turn contextual follow-up relying on history!
  console.log("Test 3 (Follow-up relying on context): 'Iska aam aadmi par kya asar parta hai?'");
  const history = [
    { role: "user", text: "Mujhe samjhao inflation kya hoti hai." },
    { role: "assistant", text: t2.body?.text || "Inflation aam taur par mehangai ko kehte hain." }
  ];
  const t3 = await sendChat("Iska aam aadmi par kya asar parta hai?", history);
  console.log("Response:", t3.body?.text?.slice(0, 200) + "...");
  console.log("------------------------------------------\n");

  // Test 4: Open-ended Joke Request in Roman Urdu
  console.log("Test 4: 'Mujhe ek funny joke sunao.'");
  const t4 = await sendChat("Mujhe ek funny joke sunao.");
  console.log("Response:", t4.body?.text?.slice(0, 200) + "...");
  console.log("------------------------------------------\n");

  // Test 5: Technical STEM explanation (RAM vs Storage)
  console.log("Test 5: 'What is the difference between RAM and storage?'");
  const t5 = await sendChat("What is the difference between RAM and storage?");
  console.log("Response:", t5.body?.text?.slice(0, 200) + "...");
  console.log("------------------------------------------\n");

  console.log("=== ALL REAL AI AGENT BRAIN TESTS COMPLETED ===");
}

runTests().catch(console.error);
