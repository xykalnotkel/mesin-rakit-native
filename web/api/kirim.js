module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") return res.status(204).end();
  if (req.method !== "POST") return res.status(405).json({ ok: false, error: "POST only" });

  let body = req.body;
  if (typeof body === "string") {
    try { body = JSON.parse(body); } catch (e) { body = {}; }
  }
  body = body || {};
  const pack = String(body.pack || "");
  if (!pack.includes("MRPACK1")) {
    return res.status(400).json({ ok: false, error: "Bukan berkas MRPACK1" });
  }
  const name = String(body.name || "tanpa nama").slice(0, 80);
  const author = String(body.author || "").slice(0, 80);
  const kind = String(body.kind || "pack").slice(0, 16);
  const pesan = String(body.pesan || "").slice(0, 400);
  const email = String(body.email || "").slice(0, 120);

  const key = process.env.RESEND_API_KEY;
  if (!key) return res.status(500).json({ ok: false, error: "RESEND_API_KEY kosong" });

  const teks =
    "MESIN RAKIT — usulan desain\n" +
    "kind: " + kind + "\n" +
    "name: " + name + "\n" +
    "author: " + author + "\n" +
    "email: " + email + "\n" +
    "pesan: " + pesan + "\n\n" +
    pack;

  const payload = {
    from: "MESIN RAKIT Studio <studio@xyc.my.id>",
    to: ["request@xyspace.my.id"],
    subject: "[MRPACK] " + kind + " — " + name,
    text: teks,
    attachments: [{
      filename: (name.replace(/[^\w\-]+/g, "_") || "desain") + ".mrpack",
      content: Buffer.from(pack, "utf8").toString("base64")
    }]
  };
  if (email && email.includes("@")) payload.reply_to = email;

  const r = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: "Bearer " + key, "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) {
    return res.status(502).json({ ok: false, error: j.message || "Gagal kirim email" });
  }
  return res.status(200).json({ ok: true, id: j.id });
};
