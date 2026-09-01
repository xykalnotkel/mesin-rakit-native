module.exports = async function handler(req, res) {
  try {
    const r = await fetch("https://api.github.com/repos/xykalnotkel/mesin-rakit-native/releases/latest", {
      headers: { Accept: "application/vnd.github+json", "User-Agent": "mesin-rakit-web" }
    });
    const d = await r.json();
    const apk = (d.assets || []).find(a => /\.apk$/i.test(a.name));
    if (!apk) {
      res.status(404).send("APK belum tersedia");
      return;
    }
    res.writeHead(302, { Location: apk.browser_download_url, "Cache-Control": "no-store" });
    res.end();
  } catch (e) {
    res.status(502).send("Gagal mengambil rilis");
  }
};
