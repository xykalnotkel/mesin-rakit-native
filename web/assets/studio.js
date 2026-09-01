(function () {
  const GW = 14, GH = 6;
  const PAINTS = [
    ["#E2E8F0", "Putih"], ["#94A3B8", "Abu"], ["#1F2937", "Abu Gelap"], ["#0F172A", "Hitam"],
    ["#DC2626", "Merah"], ["#EA580C", "Oranye"], ["#FACC15", "Kuning"], ["#16A34A", "Hijau"],
    ["#0891B2", "Cyan"], ["#2563EB", "Biru"], ["#7C3AED", "Ungu"], ["#DB2777", "Pink"]
  ];
  const S = {
    kind: "body", name: "Desain baru", author: "", color: 4,
    nodes: [], tubes: [], fmat: "besi", strokes: [], partcat: "body",
    tool: "brush", sel: -1, size: 18
  };

  const board = document.getElementById("board");
  const ctx = board.getContext("2d");
  const paintLayer = document.createElement("canvas");
  const pctx = paintLayer.getContext("2d");
  const partLayer = document.createElement("canvas");
  const xctx = partLayer.getContext("2d");

  function fit() {
    const r = board.getBoundingClientRect();
    const dpr = Math.min(2, window.devicePixelRatio || 1);
    const w = Math.max(640, Math.floor(r.width * dpr));
    const h = Math.max(360, Math.floor(w * 0.52));
    if (board.width !== w) {
      const keep = document.createElement("canvas");
      keep.width = paintLayer.width; keep.height = paintLayer.height;
      keep.getContext("2d").drawImage(paintLayer, 0, 0);
      board.width = w; board.height = h;
      paintLayer.width = w; paintLayer.height = h;
      partLayer.width = w; partLayer.height = h;
      pctx.drawImage(keep, 0, 0, w, h);
    }
  }

  function toast(t) {
    const el = document.getElementById("toast");
    el.textContent = t; el.style.display = "block";
    clearTimeout(toast._t); toast._t = setTimeout(() => el.style.display = "none", 2000);
  }
  function bersih(s) { return String(s || "").replace(/[\n=]/g, " ").slice(0, 48); }
  function fmt(n) { return (Math.round(n * 10000) / 10000).toFixed(4); }
  function hexA(hex, a) {
    const n = parseInt(hex.slice(1), 16);
    return "rgba(" + ((n >> 16) & 255) + "," + ((n >> 8) & 255) + "," + (n & 255) + "," + a + ")";
  }

  /* ----- siluet motor realistis ----- */
  function layout() {
    const w = board.width, h = board.height;
    const x0 = w * 0.06, y0 = h * 0.10, bw = w * 0.88, bh = h * 0.82;
    const X = t => x0 + t * bw, Y = t => y0 + t * bh;
    return { w, h, X, Y, x0, y0, bw, bh };
  }

  function bodyPath(c, L) {
    const { X, Y } = L;
    c.beginPath();
    /* buntut + jok */
    c.moveTo(X(0.10), Y(0.48));
    c.quadraticCurveTo(X(0.06), Y(0.42), X(0.12), Y(0.38));
    c.quadraticCurveTo(X(0.22), Y(0.34), X(0.32), Y(0.36));
    /* tangki */
    c.quadraticCurveTo(X(0.42), Y(0.28), X(0.54), Y(0.30));
    c.quadraticCurveTo(X(0.64), Y(0.32), X(0.68), Y(0.40));
    /* fairing / kepala */
    c.quadraticCurveTo(X(0.78), Y(0.34), X(0.86), Y(0.42));
    c.quadraticCurveTo(X(0.90), Y(0.50), X(0.84), Y(0.58));
    /* cover samping + bawah mesin */
    c.quadraticCurveTo(X(0.72), Y(0.66), X(0.58), Y(0.64));
    c.quadraticCurveTo(X(0.46), Y(0.70), X(0.34), Y(0.62));
    c.quadraticCurveTo(X(0.22), Y(0.60), X(0.10), Y(0.48));
    c.closePath();
  }

  function drawBike(c, L, painted) {
    const { X, Y } = L;
    /* lantai bengkel */
    c.fillStyle = "#0a1018";
    c.fillRect(0, 0, L.w, L.h);
    c.strokeStyle = "rgba(34,211,238,0.05)";
    c.lineWidth = 1;
    for (let i = 0; i < 12; i++) {
      c.beginPath(); c.moveTo(0, (i / 11) * L.h); c.lineTo(L.w, (i / 11) * L.h); c.stroke();
    }

    function wheel(cx, cy, r) {
      c.save();
      c.beginPath(); c.arc(cx, cy, r, 0, Math.PI * 2);
      c.fillStyle = "#11161d"; c.fill();
      c.strokeStyle = "#2a323c"; c.lineWidth = r * 0.14; c.stroke();
      c.beginPath(); c.arc(cx, cy, r * 0.62, 0, Math.PI * 2);
      c.strokeStyle = "#8a97a8"; c.lineWidth = r * 0.08; c.stroke();
      c.strokeStyle = "rgba(195,204,216,0.55)"; c.lineWidth = 1.2;
      for (let i = 0; i < 8; i++) {
        const a = i * Math.PI / 4;
        c.beginPath(); c.moveTo(cx, cy);
        c.lineTo(cx + Math.cos(a) * r * 0.58, cy + Math.sin(a) * r * 0.58); c.stroke();
      }
      c.beginPath(); c.arc(cx, cy, r * 0.12, 0, Math.PI * 2);
      c.fillStyle = "#c3ccd8"; c.fill();
      c.restore();
    }

    const rr = L.bh * 0.16, rf = L.bh * 0.15;
    const rx = X(0.26), ry = Y(0.72);
    const fx = X(0.76), fy = Y(0.72);
    wheel(rx, ry, rr); wheel(fx, fy, rf);

    /* knalpot */
    c.strokeStyle = "#9aa7b8"; c.lineWidth = L.bh * 0.035; c.lineCap = "round";
    c.beginPath(); c.moveTo(X(0.48), Y(0.62)); c.quadraticCurveTo(X(0.30), Y(0.78), X(0.14), Y(0.70)); c.stroke();
    c.fillStyle = "#b9c4d4";
    roundRect(c, X(0.08), Y(0.66), L.bw * 0.08, L.bh * 0.08, 6); c.fill();

    /* mesin */
    const eg = c.createLinearGradient(X(0.40), Y(0.48), X(0.58), Y(0.70));
    eg.addColorStop(0, "#8a97a8"); eg.addColorStop(1, "#3a4350");
    c.fillStyle = eg;
    roundRect(c, X(0.42), Y(0.50), L.bw * 0.16, L.bh * 0.18, 8); c.fill();
    c.fillStyle = "#2a323c";
    for (let i = 0; i < 5; i++) roundRect(c, X(0.435), Y(0.52 + i * 0.028), L.bw * 0.13, L.bh * 0.012, 2), c.fill();

    /* fork + stang */
    c.strokeStyle = "#c3ccd8"; c.lineWidth = L.bh * 0.028;
    c.beginPath(); c.moveTo(fx, fy - rf * 0.2); c.lineTo(X(0.72), Y(0.28)); c.stroke();
    c.beginPath(); c.moveTo(X(0.68), Y(0.26)); c.lineTo(X(0.80), Y(0.30)); c.stroke();

    /* rangka tulang (kalau mode body, sebagai bayangan) */
    if (S.kind !== "frame") {
      c.strokeStyle = "rgba(124,135,152,0.55)"; c.lineWidth = L.bh * 0.02;
      c.beginPath();
      c.moveTo(X(0.72), Y(0.32)); c.lineTo(X(0.52), Y(0.38));
      c.lineTo(X(0.34), Y(0.42)); c.lineTo(rx, ry);
      c.moveTo(X(0.52), Y(0.38)); c.lineTo(X(0.50), Y(0.58));
      c.stroke();
    }

    /* body: cat di atas siluet */
    c.save();
    bodyPath(c, L);
    c.clip();
    if (painted) c.drawImage(paintLayer, 0, 0);
    else {
      const g = c.createLinearGradient(X(0.2), Y(0.3), X(0.8), Y(0.65));
      g.addColorStop(0, PAINTS[S.color][0]);
      g.addColorStop(1, "#1a2230");
      c.fillStyle = g; c.fill();
    }
    c.restore();

    /* outline body */
    bodyPath(c, L);
    c.strokeStyle = "rgba(220,230,242,0.35)"; c.lineWidth = 1.6; c.stroke();

    /* lampu */
    c.beginPath(); c.arc(X(0.86), Y(0.46), L.bh * 0.035, 0, Math.PI * 2);
    c.fillStyle = "#f5e6a8"; c.fill();
  }

  function roundRect(c, x, y, w, h, r) {
    c.beginPath();
    c.moveTo(x + r, y); c.arcTo(x + w, y, x + w, y + h, r);
    c.arcTo(x + w, y + h, x, y + h, r); c.arcTo(x, y + h, x, y, r);
    c.arcTo(x, y, x + w, y, r); c.closePath();
  }

  function world(n, L) { return { x: L.X((n.x + 0.85) / 1.7), y: L.Y((1.15 - n.y) / 1.05) }; }
  function unworld(px, py, L) {
    return { x: ((px - L.x0) / L.bw) * 1.7 - 0.85, y: 1.15 - ((py - L.y0) / L.bh) * 1.05 };
  }

  function drawFrame(L) {
    drawBike(ctx, L, true);
    ctx.lineCap = "round"; ctx.lineJoin = "round";
    S.tubes.forEach(t => {
      const a = S.nodes[t.a], b = S.nodes[t.b]; if (!a || !b) return;
      const p = world(a, L), q = world(b, L);
      ctx.strokeStyle = "#3a4350"; ctx.lineWidth = 10;
      ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(q.x, q.y); ctx.stroke();
      const g = ctx.createLinearGradient(p.x, p.y, q.x, q.y);
      g.addColorStop(0, "#c3ccd8"); g.addColorStop(0.5, "#7c8798"); g.addColorStop(1, "#c3ccd8");
      ctx.strokeStyle = g; ctx.lineWidth = 5;
      ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(q.x, q.y); ctx.stroke();
    });
    S.nodes.forEach((n, i) => {
      const p = world(n, L);
      ctx.beginPath(); ctx.arc(p.x, p.y, i === S.sel ? 8 : 5.5, 0, Math.PI * 2);
      ctx.fillStyle = n.kind === "headstock" ? "#22d3ee"
        : n.kind === "engine" ? "#f59e0b"
        : n.kind === "axleR" ? "#ef4444"
        : n.kind === "seat" ? "#a855f7" : "#e2e8f0";
      ctx.fill();
      if (i === S.sel) { ctx.strokeStyle = "#fff"; ctx.lineWidth = 2; ctx.stroke(); }
    });
  }

  function drawPart(L) {
    ctx.fillStyle = "#0a1018"; ctx.fillRect(0, 0, L.w, L.h);
    /* meja kerja */
    const g = ctx.createLinearGradient(0, L.h * 0.7, 0, L.h);
    g.addColorStop(0, "#16233a"); g.addColorStop(1, "#070c14");
    ctx.fillStyle = g; ctx.fillRect(0, L.h * 0.62, L.w, L.h * 0.38);
    ctx.strokeStyle = "rgba(34,211,238,0.12)";
    ctx.strokeRect(L.w * 0.18, L.h * 0.10, L.w * 0.64, L.h * 0.52);
    ctx.drawImage(partLayer, 0, 0);
  }

  function draw() {
    fit();
    const L = layout();
    if (S.kind === "frame") drawFrame(L);
    else if (S.kind === "part") drawPart(L);
    else drawBike(ctx, L, true);
  }

  function insideBody(x, y) {
    const L = layout();
    bodyPath(ctx, L);
    return ctx.isPointInPath(x, y);
  }

  /* ----- input kuas ----- */
  let drawing = false;
  function pos(ev) {
    const r = board.getBoundingClientRect();
    const src = ev.touches ? ev.touches[0] : ev;
    return {
      x: (src.clientX - r.left) * board.width / r.width,
      y: (src.clientY - r.top) * board.height / r.height
    };
  }

  function airbrush(layer, x, y, erase) {
    const c = layer.getContext("2d");
    const s = S.size * (board.width / 900);
    c.save();
    if (S.kind === "body") {
      const L = layout();
      bodyPath(c, L); c.clip();
    }
    if (erase) {
      c.globalCompositeOperation = "destination-out";
      const g = c.createRadialGradient(x, y, 0, x, y, s);
      g.addColorStop(0, "rgba(0,0,0,0.85)"); g.addColorStop(1, "rgba(0,0,0,0)");
      c.fillStyle = g;
    } else {
      c.globalCompositeOperation = "source-over";
      const g = c.createRadialGradient(x, y, 0, x, y, s);
      g.addColorStop(0, hexA(PAINTS[S.color][0], 0.55));
      g.addColorStop(1, hexA(PAINTS[S.color][0], 0));
      c.fillStyle = g;
    }
    c.beginPath(); c.arc(x, y, s, 0, Math.PI * 2); c.fill();
    c.restore();
  }

  function onDown(p) {
    if (S.kind === "frame") { frameDown(p); draw(); return; }
    drawing = true;
    if (S.kind === "body") airbrush(paintLayer, p.x, p.y, S.tool === "erase");
    else airbrush(partLayer, p.x, p.y, S.tool === "erase");
    draw();
  }
  function onMove(p) {
    if (!drawing) return;
    if (S.kind === "body") airbrush(paintLayer, p.x, p.y, S.tool === "erase");
    else if (S.kind === "part") airbrush(partLayer, p.x, p.y, S.tool === "erase");
    draw();
  }

  function frameDown(p) {
    const L = layout();
    const hit = hitNode(p, L);
    if (hit >= 0) {
      if (S.sel >= 0 && S.sel !== hit) {
        S.tubes.push({ a: S.sel, b: hit, dia: 0.032, thick: 0.002, mat: S.fmat });
      }
      S.sel = hit;
      return;
    }
    const u = unworld(p.x, p.y, L);
    S.nodes.push({ x: u.x, y: u.y, bow: 0, kind: document.getElementById("nk").value });
    S.sel = S.nodes.length - 1;
  }
  function hitNode(p, L) {
    for (let i = 0; i < S.nodes.length; i++) {
      const q = world(S.nodes[i], L);
      if ((q.x - p.x) ** 2 + (q.y - p.y) ** 2 < 18 * 18) return i;
    }
    return -1;
  }

  board.addEventListener("pointerdown", e => { e.preventDefault(); board.setPointerCapture(e.pointerId); onDown(pos(e)); });
  board.addEventListener("pointermove", e => { if (drawing) onMove(pos(e)); });
  board.addEventListener("pointerup", () => { drawing = false; });
  window.addEventListener("resize", () => { fit(); draw(); });

  /* ----- MRPACK tanpa ditampilkan ----- */
  function samplePaint() {
    const L = layout();
    const tmp = document.createElement("canvas");
    tmp.width = board.width; tmp.height = board.height;
    const t = tmp.getContext("2d");
    t.drawImage(paintLayer, 0, 0);
    const map = {};
    const x0 = L.X(0.10), y0 = L.Y(0.28), x1 = L.X(0.88), y1 = L.Y(0.66);
    const cw = (x1 - x0) / GW, ch = (y1 - y0) / GH;
    for (let r = 0; r < GH; r++) for (let c = 0; c < GW; c++) {
      const x = x0 + (c + 0.5) * cw, y = y0 + (r + 0.5) * ch;
      const pix = t.getImageData(Math.floor(x), Math.floor(y), 1, 1).data;
      if (pix[3] < 40) continue;
      let best = 0, bd = 1e9;
      for (let i = 0; i < PAINTS.length; i++) {
        const n = parseInt(PAINTS[i][0].slice(1), 16);
        const d = (pix[0] - ((n >> 16) & 255)) ** 2 + (pix[1] - ((n >> 8) & 255)) ** 2 + (pix[2] - (n & 255)) ** 2;
        if (d < bd) { bd = d; best = i; }
      }
      map[c + "," + r] = best;
    }
    return map;
  }

  function encode() {
    S.name = document.getElementById("nama").value;
    S.author = document.getElementById("author").value;
    const lines = ["MRPACK1", "v=1", "kind=" + S.kind, "name=" + bersih(S.name)];
    if (S.author) lines.push("author=" + bersih(S.author));
    lines.push("color=" + S.color);
    const paint = samplePaint();
    const keys = Object.keys(paint);
    if (keys.length) lines.push("paint=" + keys.map(k => k + ":" + paint[k]).join(";"));
    if (S.nodes.length) {
      lines.push("fnodes=" + S.nodes.map(n => fmt(n.x) + ":" + fmt(n.y) + ":" + fmt(n.bow || 0) + ":" + n.kind).join(";"));
      lines.push("ftubes=" + S.tubes.map(t => t.a + ":" + t.b + ":" + fmt(t.dia) + ":" + fmt(t.thick) + ":" + t.mat).join(";"));
      lines.push("fmat=" + S.fmat);
    }
    if (S.kind === "part") lines.push("partcat=" + S.partcat);
    return lines.join("\n") + "\n";
  }

  function syncStat() {
    const lengkap = S.nodes.some(n => n.kind === "headstock") && S.nodes.some(n => n.kind === "engine") && S.nodes.some(n => n.kind === "axleR");
    document.getElementById("stat").textContent =
      S.kind === "frame"
        ? (S.nodes.length + " sendi · " + S.tubes.length + " pipa" + (lengkap ? " · rangka siap" : " · masih kurang dudukan"))
        : S.kind === "part" ? "Gambar part di meja kerja, lalu kirim ke studio"
        : "Kuas di bodi motor · 12 warna cat game";
  }

  const sw = document.getElementById("swatches");
  PAINTS.forEach((p, i) => {
    const b = document.createElement("button");
    b.className = "swatch" + (i === S.color ? " on" : "");
    b.style.background = p[0]; b.title = p[1];
    b.onclick = () => { S.color = i; [...sw.children].forEach((x, j) => x.classList.toggle("on", j === i)); draw(); };
    sw.appendChild(b);
  });

  document.getElementById("tabs").addEventListener("click", e => {
    const t = e.target.closest(".tab"); if (!t) return;
    S.kind = t.dataset.kind; drawing = false;
    [...document.getElementById("tabs").children].forEach(x => x.classList.toggle("on", x === t));
    document.getElementById("alat-body").hidden = S.kind === "frame";
    document.getElementById("alat-frame").hidden = S.kind !== "frame";
    document.getElementById("alat-part").hidden = S.kind !== "part";
    syncStat(); draw();
  });
  document.getElementById("t-brush").onclick = () => S.tool = "brush";
  document.getElementById("t-erase").onclick = () => S.tool = "erase";
  document.getElementById("ukuran") && (document.getElementById("ukuran").oninput = e => { S.size = +e.target.value; });
  document.getElementById("fmat").onchange = e => { S.fmat = e.target.value; };
  document.getElementById("nk").onchange = e => { if (S.sel >= 0) { S.nodes[S.sel].kind = e.target.value; draw(); } };
  document.getElementById("partcat").onchange = e => { S.partcat = e.target.value; };
  document.getElementById("hapus-node").onclick = () => {
    if (S.sel < 0) return;
    S.tubes = S.tubes.filter(t => t.a !== S.sel && t.b !== S.sel)
      .map(t => ({ ...t, a: t.a > S.sel ? t.a - 1 : t.a, b: t.b > S.sel ? t.b - 1 : t.b }));
    S.nodes.splice(S.sel, 1); S.sel = -1; draw(); syncStat();
  };
  document.getElementById("clear-part").onclick = () => {
    xctx.clearRect(0, 0, partLayer.width, partLayer.height);
    pctx.clearRect(0, 0, paintLayer.width, paintLayer.height);
    draw();
  };

  document.getElementById("salin").onclick = async () => {
    const teks = encode();
    try { await navigator.clipboard.writeText(teks); toast("Kode desain tersalin — tempel di menu game"); }
    catch (e) { toast("Salin gagal, unduh berkas saja"); }
  };
  document.getElementById("unduh-berkas").onclick = () => {
    const blob = new Blob([encode()], { type: "text/plain" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = (bersih(document.getElementById("nama").value) || "desain") + ".mrpack";
    a.click();
  };
  document.getElementById("kirim").onclick = async () => {
    const status = document.getElementById("kirim-status");
    status.textContent = "Mengirim…";
    try {
      const r = await fetch("/api/kirim", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: document.getElementById("nama").value,
          author: document.getElementById("author").value,
          kind: S.kind, pack: encode(),
          email: document.getElementById("email").value,
          pesan: document.getElementById("pesan").value
        })
      });
      const j = await r.json();
      if (!j.ok) throw new Error(j.error || "gagal");
      status.textContent = "Terkirim ke request@xyspace.my.id";
      toast("Desain terkirim");
    } catch (e) { status.textContent = "Gagal kirim: " + e.message; }
  };

  fit(); draw(); syncStat();
})();
