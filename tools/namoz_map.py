#!/usr/bin/env python3
"""Open Admin Data district -> namoz-vaqti.uz slug mapping.

  python3 tools/namoz_map.py build            # assets/namoz_region_map.json + namoz_regions_unresolved.json
  python3 tools/namoz_map.py verify [--apply] # real API check of every mapped slug (internet kerak)

build: faqat nom + hudud turi (shahar/tuman/shahar-tumani) bo'yicha ANIQ moslikni oladi.
Ehtimoliy moslik yozilmaydi: bir nechta yoki nol nomzod bo'lsa district unresolved bo'ladi.
verify: har slug uchun API'ga so'rov; today.times ichida 6 vaqt HH:mm bo'lsa VALID.
--apply: VALID bo'lmagan slugni map'dan olib, unresolved'ga ko'chiradi.
"""
import json, re, sys, time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

A = Path(__file__).resolve().parent.parent / "app/src/main/assets"
API = "https://www.namoz-vaqti.uz/index.php?region={}&lang=lotin&period=today&format=json"
KEYS = ["bomdod", "quyosh", "peshin", "asr", "shom", "xufton"]
TIME = re.compile(r"^([01]\d|2[0-3]):[0-5]\d$")
TASHKENT_CITY = "UZB010"

# Faqat imlo farqi bo'lgan bir xil joylar (OAD kaliti -> namoz kaliti).
ALIAS = {
    "sirgali": "sergeli", "shayxontoxur": "shayxontohur", "qiyichirchiq": "quyichirchiq",
    "pskent": "piskent", "marxamat": "marhamat", "shaxrixon": "shahrixon",
    "ellikkala": "ellikqala",
}


def norm(s):
    s = s.lower().replace("ш.", "")
    return re.sub(r"[\s\-'`‘’ʻʼ.]", "", s)


def load():
    a = json.load(open(A / "uzbekistan_regions.json", encoding="utf-8"))
    b = json.load(open(A / "namoz_regions.json", encoding="utf-8"))
    return a, b


def namoz_type(name):
    n = name.strip()
    if n.endswith(" shahri"):
        return "city", norm(n[:-7])
    if n.endswith(" tumani"):
        return "district", norm(n[:-7])
    if n.endswith(" viloyati") or n.endswith("Resp."):
        return "province", norm(n)
    return "bare", norm(n)


def build():
    a, b = load()
    idx = {}
    for r in b:
        t, k = namoz_type(r["name"])
        idx.setdefault((t, k), []).append(r)
    prov = {p["id"]: p["name_local"] for p in a["province"]}
    ok, bad = {}, {}
    for d in sorted(a["district"], key=lambda x: x["id"]):
        name = d["name_local"].strip()
        is_city = name.endswith("ш.")
        key = norm(name)
        key = ALIAS.get(key, key)
        if d["parent_id"] == TASHKENT_CITY:
            order = ["bare"]
        elif is_city:
            order = ["city", "bare"]
        else:
            order = ["district"]
        found = []
        for t in order:
            found = idx.get((t, key), [])
            if found:
                break
        info = {"name": name, "province": prov.get(d["parent_id"], d["parent_id"])}
        if len(found) == 1:
            ok[d["id"]] = {"namozSlug": found[0]["slug"], "namozName": found[0]["name"]}
        else:
            info["reason"] = "ambiguous" if len(found) > 1 else "no_exact_match"
            bad[d["id"]] = info
    write(ok, bad)
    print(f"mapped: {len(ok)}  unresolved: {len(bad)}  total: {len(ok) + len(bad)}")
    slugs = [v["namozSlug"] for v in ok.values()]
    dup = sorted({s for s in slugs if slugs.count(s) > 1})
    print("bir slug bir nechta districtda:", dup or "yo'q")
    for k, v in bad.items():
        print("  UNRESOLVED", k, v["name"], "|", v["province"], "|", v["reason"])


def write(ok, bad):
    for fn, data in (("namoz_region_map.json", ok), ("namoz_regions_unresolved.json", bad)):
        with open(A / fn, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")


def check(slug):
    import urllib.request
    for attempt in range(3):
        try:
            req = urllib.request.Request(API.format(slug), headers={"Accept": "application/json"})
            with urllib.request.urlopen(req, timeout=20) as r:
                t = json.load(r)["today"]["times"]
            if all(isinstance(t.get(k), str) and TIME.match(t[k]) for k in KEYS):
                return slug, True, t["bomdod"]
            return slug, False, "6 vaqt yo'q yoki format noto'g'ri"
        except Exception as e:  # noqa: BLE001
            err = str(e)
            time.sleep(1.5)
    return slug, False, err


def verify(apply):
    ok = json.load(open(A / "namoz_region_map.json", encoding="utf-8"))
    bad = json.load(open(A / "namoz_regions_unresolved.json", encoding="utf-8"))
    a, _ = load()
    dn = {d["id"]: d for d in a["district"]}
    prov = {p["id"]: p["name_local"] for p in a["province"]}
    slugs = sorted({v["namozSlug"] for v in ok.values()})
    with ThreadPoolExecutor(6) as ex:
        res = {s: (v, m) for s, v, m in ex.map(check, slugs)}
    failed = {s for s, (v, _) in res.items() if not v}
    print(f"VALID {len(slugs) - len(failed)} / {len(slugs)}")
    for s in sorted(failed):
        print("  INVALID", s, res[s][1])
    if apply and failed:
        for did in [k for k, v in ok.items() if v["namozSlug"] in failed]:
            d = dn[did]
            bad[did] = {"name": d["name_local"], "province": prov.get(d["parent_id"], ""),
                        "reason": "api_invalid:" + ok[did]["namozSlug"]}
            del ok[did]
        write(ok, bad)
        print("map yangilandi:", len(ok), "mapped,", len(bad), "unresolved")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "build"
    build() if cmd == "build" else verify("--apply" in sys.argv)
