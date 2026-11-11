#!/usr/bin/env python3
"""HIDOYAT — Qur'on ma'lumotini tayyorlash va tekshirish.

  python3 tools/quran_data.py fetch      # QuranEnc.com'dan yuklab app/src/main/assets/quran/ ga yozadi
  python3 tools/quran_data.py verify     # assets (yoki berilgan papka) ichidagi fayllarni tekshiradi
  python3 tools/quran_data.py selftest   # tekshiruv mantig'ini sun'iy (Qur'on bo'lmagan) ma'lumot bilan sinaydi

Fayllar (ilova ichidagi QuranTextFormat bilan bir xil format, har qatorda  sura|oyat|matn):
  quran_ar.txt        arabcha matn
  quran_uz.txt        Muhammad Sodiq tarjimasi
  quran_uz_notes.txt  manbadagi izohlar (faqat izohi bor oyatlar uchun)

Matn o'zgartirilmaydi: faqat chetlaridagi bo'sh joy olinadi, "\\" va yangi qator escape qilinadi.
Hamma narsa avval vaqtinchalik papkaga yoziladi, TO'LIQ tekshiriladi (114 sura, 6236 oyat,
arabcha va tarjima kalitlari bir xil) va faqat shundan keyin assets/quran almashtiriladi.
"""
import http.client, json, pathlib, re, shutil, sys, tempfile, time, urllib.request, urllib.error

ROOT = pathlib.Path(__file__).resolve().parent.parent
CATALOG = ROOT / "app/src/main/java/uz/hidoyat/app/feature/quran/QuranCatalog.kt"
OUT = ROOT / "app/src/main/assets/quran"
KEY = "uzbek_sadiq"
TRANSLATOR = "Muhammad Sodiq"
VERSION = "1.0.1"
API = "https://quranenc.com/api/v1/translation/sura/%s/%d"
USER_AGENT = "HIDOYAT-QuranApp/1.0"
FILES = ("quran_ar.txt", "quran_uz.txt", "quran_uz_notes.txt")
FONT = ROOT / "app/src/main/assets/fonts/quran_arabic.ttf"
PAUSE = 0.25  # QuranEnc serveriga ortiqcha yuk bermaslik uchun so'rovlar orasidagi pauza (soniya)


def counts():
    found = re.findall(
        r'SurahInfo\((\d+), "[^"]+", "[^"]+", (\d+), Revelation', CATALOG.read_text(encoding="utf-8"))
    c = {int(n): int(a) for n, a in found}
    assert len(c) == 114 and sum(c.values()) == 6236, "Katalog buzilgan"
    assert list(c) == list(range(1, 115)), "Katalog raqamlari ketma-ket emas"
    return c


def escape(t):
    return t.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n")


def unescape(t):
    out, i = [], 0
    while i < len(t):
        if t[i] == "\\" and i + 1 < len(t) and t[i + 1] in "n\\":
            out.append("\n" if t[i + 1] == "n" else "\\")
            i += 2
        else:
            out.append(t[i])
            i += 1
    return "".join(out)


def header(kind):
    return (f"# source: QuranEnc.com\n# translation_key: {KEY}\n# translator: {TRANSLATOR}\n"
            f"# version: {VERSION}\n# url: https://quranenc.com\n# content: {kind}\n")


def parse_file(path, partial=False):
    """Faylni o'qiydi va qat'iy tekshiradi. Qaytaradi: (meta, {(sura, oyat): matn})."""
    c = counts()
    meta, seen = {}, {}
    for lineno, raw in enumerate(pathlib.Path(path).read_text(encoding="utf-8-sig").splitlines(), 1):
        line = raw.rstrip("\r")
        if not line.strip():
            continue
        if line.startswith("#"):
            k, _, v = line[1:].partition(":")
            if v:
                meta[k.strip()] = v.strip()
            continue
        parts = line.split("|", 2)
        if len(parts) != 3:
            raise SystemExit(f"{path}:{lineno}: noto'g'ri format")
        try:
            s, a = int(parts[0]), int(parts[1])
        except ValueError:
            raise SystemExit(f"{path}:{lineno}: raqam noto'g'ri")
        text = unescape(parts[2].strip())
        if s not in c or not 1 <= a <= c[s]:
            raise SystemExit(f"{path}:{lineno}: diapazondan tashqari {s}:{a}")
        if not text:
            raise SystemExit(f"{path}:{lineno}: bo'sh matn {s}:{a}")
        if (s, a) in seen:
            raise SystemExit(f"{path}:{lineno}: takrorlangan {s}:{a}")
        seen[(s, a)] = text
    if not partial:
        missing = [(s, a) for s in c for a in range(1, c[s] + 1) if (s, a) not in seen]
        if missing:
            raise SystemExit(f"{path}: to'liq emas, yetishmayotgan oyatlar: {len(missing)} (birinchisi {missing[0]})")
    return meta, seen


def verify_dir(d):
    d = pathlib.Path(d)
    ar_meta, ar = parse_file(d / FILES[0])
    uz_meta, uz = parse_file(d / FILES[1])
    if set(ar) != set(uz):
        raise SystemExit("Arabcha va tarjima fayllaridagi oyatlar bir-biriga mos emas")
    notes_path = d / FILES[2]
    if not notes_path.exists():
        # Ilova ham shuni talab qiladi (QuranRepository): izohlar jimgina yo'qolmasin
        raise SystemExit(f"{FILES[2]} yo'q: footnotes fayli majburiy (bo'sh bo'lsa ham sarlavha bilan mavjud bo'lishi kerak)")
    _, notes = parse_file(notes_path, partial=True)
    if not set(notes) <= set(uz):
        raise SystemExit("Izohlar fayli mavjud bo'lmagan oyatga ishora qiladi")
    for k in ("source", "translation_key", "translator", "version", "url"):
        if not uz_meta.get(k):
            raise SystemExit(f"{FILES[1]}: sarlavhada '{k}' yo'q (QuranEnc manbasi/versiyasi saqlanmagan)")
    if uz_meta["translation_key"] != KEY:
        raise SystemExit(f"{FILES[1]}: tarjima kaliti {uz_meta['translation_key']!r}, kutilgan {KEY!r}")
    print(f"OK  {d}: 114 sura, {len(ar)} oyat, {len(notes)} izoh; {uz_meta['source']} {uz_meta['translation_key']} v{uz_meta['version']}")
    if not notes:
        print("OGOHLANTIRISH: izohlar (footnotes) 0 ta. Manbada izoh bo'lmasa — normal; bo'lsa — yuklash xatosi.")
    # Basmala: ilova UI 1-oyat matnini (1:1) sarlavha sifatida qayta ishlatadi. Manba x:1 ga basmalani
    # qo'shib yuborgan suralar bo'lsa, ilova ularda sarlavhani takrorlamaydi (QuranData.showsBasmalaHeader).
    nows = lambda s: "".join(s.split())
    bas = nows(ar[(1, 1)])
    merged = [s for s in range(2, 115) if s != 9 and nows(ar[(s, 1)]).startswith(bas)]
    print(f"INFO basmala: x:1 matnida basmala bor suralar: {len(merged)} ta" + (f" {merged[:10]}" if merged else ""))
    if not FONT.exists():
        print(f"OGOHLANTIRISH: Qur'on shrifti yo'q (app/src/main/assets/fonts/quran_arabic.ttf); ilova tizim Serif shriftiga tushadi.")


def fetch_sura(s, expected):
    last = None
    for attempt in range(3):
        try:
            req = urllib.request.Request(API % (KEY, s), headers={"Accept": "application/json", "User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=30) as r:
                if r.status != 200:
                    raise SystemExit(f"{s}: HTTP {r.status}")
                data = json.load(r)["result"]
            got = {}
            for it in data:
                if str(it.get("sura", s)) != str(s):
                    raise SystemExit(f"{s}: javobda boshqa sura {it.get('sura')}")
                for k in ("aya", "arabic_text", "translation"):
                    if it.get(k) is None:
                        raise SystemExit(f"{s}: '{k}' maydoni yo'q yoki null")
                a = int(it["aya"])
                ar, tr = str(it["arabic_text"]).strip(), str(it["translation"]).strip()
                if a in got or not 1 <= a <= expected or not ar or not tr:
                    raise SystemExit(f"{s}:{a} — noto'g'ri javob")
                got[a] = (ar, tr, (it.get("footnotes") or "").strip())
            if len(got) != expected:
                raise SystemExit(f"{s}-sura to'liq kelmadi: {len(got)} / {expected}")
            return got
        except (OSError, http.client.HTTPException, ValueError, KeyError, TypeError) as e:
            # OSError ichiga URLError/HTTPError/TimeoutError/ConnectionError kiradi; ValueError — JSONDecodeError va UnicodeDecodeError
            last = e
            time.sleep(1.5 * (attempt + 1))
    raise SystemExit(f"{s}-sura yuklanmadi: {last}")


def fetch():
    c = counts()
    ar, uz, notes = [header("Arabic text")], [header("Translation")], [header("Footnotes")]
    for s in range(1, 115):
        got = fetch_sura(s, c[s])
        time.sleep(PAUSE)
        for a in range(1, c[s] + 1):
            ar.append(f"{s}|{a}|{escape(got[a][0])}\n")
            uz.append(f"{s}|{a}|{escape(got[a][1])}\n")
            if got[a][2]:
                notes.append(f"{s}|{a}|{escape(got[a][2])}\n")
        print(f"{s}/114", end="\r", flush=True)
    # Vaqtinchalik papka assets ICHIDA emas (qoldiq APK'ga tushib qolmasin); ROOT bilan bir fayl tizimida
    tmp = pathlib.Path(tempfile.mkdtemp(prefix=".quran_tmp_", dir=ROOT))
    old = OUT.with_name(OUT.name + ".old")
    try:
        for name, lines in zip(FILES, (ar, uz, notes)):
            (tmp / name).write_text("".join(lines), encoding="utf-8")
        verify_dir(tmp)  # to'liq tekshiruvdan o'tmasa assets tegilmaydi
        OUT.parent.mkdir(parents=True, exist_ok=True)
        if old.exists():
            shutil.rmtree(old)
        if OUT.exists():
            OUT.rename(old)  # eski nusxa zaxirada: almashtirish muvaffaqiyatsiz bo'lsa qaytariladi
        try:
            tmp.rename(OUT)
        except BaseException:
            if old.exists() and not OUT.exists():
                old.rename(OUT)
            raise
        if old.exists():
            shutil.rmtree(old)
    except BaseException:
        shutil.rmtree(tmp, ignore_errors=True)
        raise
    print("Yozildi:", OUT)
    verify_dir(OUT)


def selftest():
    """Sun'iy (Qur'on EMAS) ma'lumot bilan tekshiruvchining to'g'ri ishlashini sinaydi."""
    c = counts()
    with tempfile.TemporaryDirectory() as t:
        d = pathlib.Path(t)
        rows = [(s, a) for s in c for a in range(1, c[s] + 1)]
        (d / FILES[0]).write_text(header("t") + "".join(f"{s}|{a}|ar-{s}-{a}\n" for s, a in rows), encoding="utf-8")
        (d / FILES[1]).write_text(header("t") + "".join(f"{s}|{a}|tr {s}:{a} | pipe \\\\ ok\n" for s, a in rows), encoding="utf-8")
        (d / FILES[2]).write_text(header("t") + "1|1|izoh\\nikkinchi qator\n", encoding="utf-8")
        verify_dir(d)
        assert parse_file(d / FILES[2], partial=True)[1][(1, 1)] == "izoh\nikkinchi qator"
        for name, mutate in (
            ("yetishmayotgan oyat", lambda x: x.replace("1|2|", "#1|2|", 1)),
            ("takror", lambda x: x + "1|1|x\n"),
            ("diapazondan tashqari", lambda x: x + "1|8|x\n"),
            ("bo'sh matn", lambda x: x.replace("1|1|ar-1-1", "1|1|", 1)),
        ):
            p = d / FILES[0]
            original = p.read_text(encoding="utf-8")
            p.write_text(mutate(original), encoding="utf-8")
            try:
                verify_dir(d)
            except SystemExit:
                print(f"OK  salbiy sinov rad etildi: {name}")
            else:
                raise AssertionError(f"tekshiruv o'tkazib yubordi: {name}")
            p.write_text(original, encoding="utf-8")
    fetch_roundtrip_test()
    print("selftest OK")


def fetch_roundtrip_test():
    """fetch() ni mahalliy soxta server bilan sinaydi (sun'iy ma'lumot, internet kerak emas):
    footnotes null/bo'sh/matnli, 'null' so'zi matnda, | va \\ belgilari, atomik almashtirish va eski nusxani saqlash."""
    import http.server, threading
    c = counts()

    class H(http.server.BaseHTTPRequestHandler):
        def log_message(self, *a): pass
        def do_GET(self):
            s = int(self.path.rstrip("/").split("/")[-1])
            items = []
            for a in range(1, c[s] + 1):
                note = None if a % 3 else ("izoh " + str(a) if a % 2 else "")
                items.append({"id": 1, "sura": str(s), "aya": str(a), "arabic_text": f" ar-{s}-{a} ",
                              "translation": f"tr {s}:{a} | a\\b null", "footnotes": note})
            body = json.dumps({"result": items}).encode()
            self.send_response(200); self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body)

    srv = http.server.ThreadingHTTPServer(("127.0.0.1", 0), H)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    global API, OUT, PAUSE, ROOT
    saved = (API, OUT, PAUSE, ROOT)
    with tempfile.TemporaryDirectory() as t:
        ROOT = pathlib.Path(t); OUT = ROOT / "assets/quran"; PAUSE = 0
        API = f"http://127.0.0.1:{srv.server_address[1]}/%s/%d"
        try:
            (OUT).mkdir(parents=True); (OUT / "marker.txt").write_text("eski")
            fetch()
            verify_dir(OUT)
            assert not (OUT / "marker.txt").exists() and not OUT.with_name("quran.old").exists(), "almashtirish/tozalash"
            assert not list(ROOT.glob(".quran_tmp_*")), "vaqtinchalik papka qolib ketdi"
            _, notes = parse_file(OUT / FILES[2], partial=True)
            _, uz = parse_file(OUT / FILES[1])
            assert all(v != "null" for v in notes.values()), "footnotes null -> 'null' bo'lib qoldi"
            assert len(notes) == sum(1 for s in c for a in range(1, c[s] + 1) if a % 3 == 0 and a % 2 == 1), "izohlar soni"
            assert uz[(2, 5)] == "tr 2:5 | a\\b null", "tarjima o'zgargan: " + uz[(2, 5)]
            print("OK  fetch roundtrip: 114 sura, izohlar (null/bo'sh/matnli), atomik almashtirish")
            # Server uzilsa: eski ma'lumot tegilmaydi
            srv.shutdown(); srv.server_close()
            before = (OUT / FILES[1]).read_text(encoding="utf-8")
            try:
                fetch_sura.__globals__["time"].sleep = lambda x: None
                fetch()
            except SystemExit:
                pass
            assert (OUT / FILES[1]).read_text(encoding="utf-8") == before, "xatoda eski ma'lumot buzildi"
            assert not list(ROOT.glob(".quran_tmp_*")), "xatodan keyin vaqtinchalik papka qoldi"
            print("OK  server uzilganda eski ma'lumot saqlandi")
        finally:
            API, OUT, PAUSE, ROOT = saved


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    if cmd == "fetch":
        fetch()
    elif cmd == "verify":
        verify_dir(sys.argv[2] if len(sys.argv) > 2 else OUT)
    elif cmd == "selftest":
        selftest()
    else:
        print(__doc__)
