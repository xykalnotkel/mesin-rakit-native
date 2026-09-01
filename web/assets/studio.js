(function () {
  const GW = 14, GH = 6;
  const PAINTS = [
    ["#E2E8F0", "Putih"], ["#94A3B8", "Abu"], ["#1F2937", "Abu Gelap"], ["#0F172A", "Hitam"],
    ["#DC2626", "Merah"], ["#EA580C", "Oranye"], ["#FACC15", "Kuning"], ["#16A34A", "Hijau"],
    ["#0891B2", "Cyan"], ["#2563EB", "Biru"], ["#7C3AED", "Ungu"], ["#DB2777", "Pink"]
  ];
  const S = {
    kind: "body",
    name: "Desain baru",
    author: "",
    color: 8,
    paint: {},
    nodes: [],
    tubes: [],
    fmat: "besi",
    strokes: [],
    partcat: "body",
    tool: "brush",
    sel: -1,
    pending: -1
  };

  const board = document.getElementById("board");
  const ctx = board.getContext("2d");
  const packEl = document.getElementById("pack");

  function toast(t) {
    const el = document.getElementById("toast");
    el.textContent = t; el.style.display = "block";
    clearTimeout(toast._t); toast._t = setTimeout(() => el.style.display = "none", 2200);
  }
  function bersih(s) { return String(s || "").replace(/[\n=]/g, " ").slice(0, 48); }
  function fmt(n) { return (Math.round(n * 10000) / 10000).toFixed(4); }

  function encode() {
    const lines = ["MRPACK1", "v=1", "kind=" + S.kind, "name=" + bersih(S.name)];
    if (S.author) lines.push("author=" + bersih(S.author));
    lines.push("color=" + S.color);
    const paints = Object.keys(S.paint).map(k => k + ":" + S.paint[k]);
    if (paints.length) lines.push("paint=" + paints.join(";"));
    if (S.nodes.length) {
      lines.push("fnodes=" + S.nodes.map(n =>
        fmt(n.x) + ":" + fmt(n.y) + ":" + fmt(n.bow || 0) + ":" + n.kind).join(";"));
      lines.push("ftubes=" + S.tubes.map(t =>
        t.a + ":" + t.b + ":" + fmt(t.dia) + ":" + fmt(t.thick) + ":" + t.mat).join(";"));
      lines.push("fmat=" + S.fmat);
    }
    if (S.kind === "part") {
      lines.push("partcat=" + S.partcat);
      if (S.strokes.length) {
        const paths = S.strokes.map(st => st.map(p => fmt(p.x) + "," + fmt(p.y)).join(" ")).join("|");
        lines.push("paths=" + paths);
        rasterStrokesToPaint();
        const p2 = Object.keys(S.paint).map(k => k + ":" + S.paint[k]);
        if (p2.length) lines.push("paint=" + p2.join(";"));
      }
    }
    return lines.join("\n") + "\n";
  }

  function rasterStrokesToPaint() {
    S.paint = {};
    S.strokes.forEach(st => st.forEach(p => {
      const c = Math.max(0, Math.min(GW - 1, Math.floor(p.x * GW)));
      const r = Math.max(0, Math.min(GH - 1, Math.floor(p.y * GH)));
      S.paint[c + "," + r] = S.color;
    }));
  }

  function syncPack() {
    S.name = document.getElementById("nama").value;
    S.author = document.getElementById("author").value;
    packEl.value = encode();
    const nPipa = S.tubes.length;
    const lengkap = hasKind("headstock") && hasKind("engine") && hasKind("axleR");
    document.getElementById("stat").textContent =
      S.kind === "frame"
        ? ("Simpul " + S.nodes.length + " · pipa " + nPipa + (lengkap ? " · rangka siap" : " · belum lengkap"))
        : S.kind === "part"
          ? ("Usulan part · " + S.strokes.length + " goresan")
          : ("Grid " + GW + "×" + GH + " · " + Object.keys(S.paint).length + " sel dicat");
  }

  function hasKind(k) { return S.nodes.some(n => n.kind === k); }

  /* ---------- gambar ---------- */
  function draw() {
    const w = board.width, h = board.height;
    ctx.fillStyle = "#0a111c"; ctx.fillRect(0, 0, w, h);
    if (S.kind === "body") drawBody(w, h);
    else if (S.kind === "frame") drawFrame(w, h);
    else drawPart(w, h);
  }

  function drawBody(w, h) {
    const pad = 24, gw = (w - pad * 2) / GW, gh = (h - pad * 2) / GH;
    ghostBike(w, h);
    for (let r = 0; r < GH; r++) for (let c = 0; c < GW; c++) {
      const x = pad + c * gw, y = pad + r * gh;
      const k = c + "," + r;
      ctx.fillStyle = S.paint[k] != null ? PAINTS[S.paint[k]][0] : "#101b2d";
      round(x + 2, y + 2, gw - 4, gh - 4, 6);
      ctx.fill();
      ctx.strokeStyle = "#22344f"; ctx.lineWidth = 1; ctx.stroke();
    }
  }

  function drawFrame(w, h) {
    ghostBike(w, h);
    ctx.strokeStyle = "#22344f"; ctx.lineWidth = 1;
    for (let i = 0; i < 8; i++) {
      ctx.beginPath(); ctx.moveTo(0, (i / 7) * h); ctx.lineTo(w, (i / 7) * h); ctx.stroke();
      ctx.beginPath(); ctx.moveTo((i / 7) * w, 0); ctx.lineTo((i / 7) * w, h); ctx.stroke();
    }
    ctx.strokeStyle = "#7c8798"; ctx.lineWidth = 4; ctx.lineCap = "round";
    S.tubes.forEach(t => {
      const a = S.nodes[t.a], b = S.nodes[t.b]; if (!a || !b) return;
      const p = world(a, w, h), q = world(b, w, h);
      ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(q.x, q.y); ctx.stroke();
    });
    S.nodes.forEach((n, i) => {
      const p = world(n, w, h);
      ctx.fillStyle = n.kind === "headstock" ? "#22d3ee"
        : n.kind === "engine" ? "#f59e0b"
        : n.kind === "axleR" ? "#ef4444"
        : n.kind === "seat" ? "#a855f7" : "#dce6f2";
      ctx.beginPath(); ctx.arc(p.x, p.y, i === S.sel ? 9 : 6, 0, Math.PI * 2); ctx.fill();
      if (i === S.sel) { ctx.strokeStyle = "#fff"; ctx.lineWidth = 2; ctx.stroke(); }
    });
  }

  function drawPart(w, h) {
    ctx.strokeStyle = "#22344f";
    ctx.strokeRect(40, 40, w - 80, h - 80);
    ctx.strokeStyle = PAINTS[S.color][0]; ctx.lineWidth = 5; ctx.lineCap = "round"; ctx.lineJoin = "round";
    S.strokes.forEach(st => {
      if (!st.length) return;
      ctx.beginPath();
      st.forEach((p, i) => {
        const x = 40 + p.x * (w - 80), y = 40 + p.y * (h - 80);
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
      });
      ctx.stroke();
    });
  }

  function ghostBike(w, h) {
    ctx.save();
    ctx.strokeStyle = "rgba(34,211,238,0.18)"; ctx.lineWidth = 3;
    const y = h * 0.62, r = 28;
    ctx.beginPath(); ctx.arc(w * 0.28, y, r, 0, Math.PI * 2); ctx.stroke();
    ctx.beginPath(); ctx.arc(w * 0.72, y, r, 0, Math.PI * 2); ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(w * 0.28, y); ctx.lineTo(w * 0.42, h * 0.42);
    ctx.lineTo(w * 0.62, h * 0.40); ctx.lineTo(w * 0.72, y);
    ctx.moveTo(w * 0.42, h * 0.42); ctx.lineTo(w * 0.38, h * 0.32);
    ctx.stroke();
    ctx.restore();
  }

  function round(x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y); ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r); ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r); ctx.closePath();
  }

  function world(n, w, h) {
    return { x: (n.x + 0.85) / 1.7 * w, y: (1.15 - n.y) / 1.05 * h };
  }
  function unworld(px, py, w, h) {
    return { x: px / w * 1.7 - 0.85, y: 1.15 - py / h * 1.05 };
  }

  /* ---------- input ---------- */
  function pos(ev) {
    const r = board.getBoundingClientRect();
    const src = ev.touches ? ev.touches[0] : ev;
    return { x: (src.clientX - r.left) * board.width / r.width, y: (src.clientY - r.top) * board.height / r.height };
  }

  let drawing = false;
  board.addEventListener("pointerdown", e => { e.preventDefault(); drawing = true; board.setPointerCapture(e.pointerId); onDown(pos(e)); });
  board.addEventListener("pointermove", e => { if (!drawing) return; onMove(pos(e)); });
  board.addEventListener("pointerup", () => { drawing = false; S.pending = -1; syncPack(); });

  function onDown(p) {
    if (S.kind === "body") paintAt(p);
    else if (S.kind === "frame") frameDown(p);
    else partDown(p);
    draw();
  }
  function onMove(p) {
    if (S.kind === "body") paintAt(p);
    else if (S.kind === "part" && S.strokes.length) {
      const w = board.width, h = board.height;
      S.strokes[S.strokes.length - 1].push({
        x: clamp((p.x - 40) / (w - 80), 0, 1),
        y: clamp((p.y - 40) / (h - 80), 0, 1)
      });
    }
    draw();
  }

  function paintAt(p) {
    const pad = 24, gw = (board.width - pad * 2) / GW, gh = (board.height - pad * 2) / GH;
    const c = Math.floor((p.x - pad) / gw), r = Math.floor((p.y - pad) / gh);
    if (c < 0 || r < 0 || c >= GW || r >= GH) return;
    const k = c + "," + r;
    if (S.tool === "erase") delete S.paint[k];
    else S.paint[k] = S.color;
  }

  function frameDown(p) {
    const hit = hitNode(p);
    if (hit >= 0) {
      if (S.sel >= 0 && S.sel !== hit) {
        S.tubes.push({ a: S.sel, b: hit, dia: 0.032, thick: 0.002, mat: S.fmat });
        S.sel = hit;
      } else S.sel = hit;
      return;
    }
    const u = unworld(p.x, p.y, board.width, board.height);
    S.nodes.push({ x: u.x, y: u.y, bow: 0, kind: document.getElementById("nk").value });
    S.sel = S.nodes.length - 1;
  }

  function hitNode(p) {
    for (let i = 0; i < S.nodes.length; i++) {
      const q = world(S.nodes[i], board.width, board.height);
      if ((q.x - p.x) ** 2 + (q.y - p.y) ** 2 < 16 * 16) return i;
    }
    return -1;
  }

  function partDown(p) {
    const w = board.width, h = board.height;
    S.strokes.push([{
      x: clamp((p.x - 40) / (w - 80), 0, 1),
      y: clamp((p.y - 40) / (h - 80), 0, 1)
    }]);
  }

  function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }

  /* ---------- ui ---------- */
  const sw = document.getElementById("swatches");
  PAINTS.forEach((p, i) => {
    const b = document.createElement("button");
    b.className = "swatch" + (i === S.color ? " on" : "");
    b.style.background = p[0]; b.title = p[1];
    b.onclick = () => { S.color = i; [...sw.children].forEach((x, j) => x.classList.toggle("on", j === i)); syncPack(); draw(); };
    sw.appendChild(b);
  });

  document.getElementById("tabs").addEventListener("click", e => {
    const t = e.target.closest(".tab"); if (!t) return;
    S.kind = t.dataset.kind;
    [...document.getElementById("tabs").children].forEach(x => x.classList.toggle("on", x === t));
    document.getElementById("alat-body").hidden = S.kind !== "body";
    document.getElementById("alat-frame").hidden = S.kind !== "frame";
    document.getElementById("alat-part").hidden = S.kind !== "part";
    syncPack(); draw();
  });

  document.getElementById("t-brush").onclick = () => S.tool = "brush";
  document.getElementById("t-erase").onclick = () => S.tool = "erase";
  document.getElementById("nama").oninput = syncPack;
  document.getElementById("author").oninput = syncPack;
  document.getElementById("fmat").onchange = e => { S.fmat = e.target.value; if (S.sel >= 0) { /* keep */ } syncPack(); };
  document.getElementById("nk").onchange = e => { if (S.sel >= 0) S.nodes[S.sel].kind = e.target.value; draw(); syncPack(); };
  document.getElementById("partcat").onchange = e => { S.partcat = e.target.value; syncPack(); };
  document.getElementById("hapus-node").onclick = () => {
    if (S.sel < 0) return;
    S.tubes = S.tubes.filter(t => t.a !== S.sel && t.b !== S.sel)
      .map(t => ({ ...t, a: t.a > S.sel ? t.a - 1 : t.a, b: t.b > S.sel ? t.b - 1 : t.b }));
    S.nodes.splice(S.sel, 1); S.sel = -1; draw(); syncPack();
  };
  document.getElementById("clear-part").onclick = () => { S.strokes = []; draw(); syncPack(); };

  document.getElementById("salin").onclick = async () => {
    syncPack();
    try { await navigator.clipboard.writeText(packEl.value); toast("Kode MRPACK1 tersalin"); }
    catch (e) { packEl.select(); document.execCommand("copy"); toast("Kode disalin"); }
  };
  document.getElementById("unduh-berkas").onclick = () => {
    syncPack();
    const blob = new Blob([packEl.value], { type: "text/plain" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = (bersih(S.name) || "desain") + ".mrpack";
    a.click();
  };
  document.getElementById("kirim").onclick = async () => {
    syncPack();
    const status = document.getElementById("kirim-status");
    status.textContent = "Mengirim…";
    try {
      const r = await fetch("/api/kirim", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: S.name, author: S.author, kind: S.kind, pack: packEl.value,
          email: document.getElementById("email").value,
          pesan: document.getElementById("pesan").value
        })
      });
      const j = await r.json();
      if (!j.ok) throw new Error(j.error || "gagal");
      status.textContent = "Terkirim ke request@xyspace.my.id";
      toast("Desain terkirim");
    } catch (e) {
      status.textContent = "Gagal kirim: " + e.message;
    }
  };

  syncPack(); draw();
})();
