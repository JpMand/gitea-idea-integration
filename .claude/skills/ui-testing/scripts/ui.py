#!/usr/bin/env python3
"""Tiny Remote-Robot + xdotool driver for the sandbox IDE on DISPLAY=:99.

  ui.py find  XPATH                 list matching visible components (with screen bounds)
  ui.py click XPATH [idx] [dx dy]   real mouse click (center, or offset from top-left)
  ui.py dclick XPATH [idx]          double-click
  ui.py rclick XPATH [idx] [dx dy]  right-click
  ui.py ctexts XPATH [idx]          rendered text fragments of a component with screen coords
  ui.py clicktext XPATH TEXT [nth]  click on rendered text (dclicktext / rclicktext too)
  ui.py tree [ROOT_REGEX] [grep]    dump visible Swing tree; subtree of first line matching ROOT_REGEX
  ui.py texts [ROOT_XPATH]          just the visible texts under ROOT, one per line
  ui.py js SCRIPT                   run JS (Rhino) in IDE, print String result
  ui.py cjs XPATH SCRIPT            run JS with `component` bound to first match
  ui.py shot NAME                   screenshot -> $GITEA_UI_STATE/shots/NAME.png
  ui.py key KEYS...                 xdotool key
  ui.py type TEXT                   xdotool type
  ui.py wait XPATH [timeout]        wait until visible
"""
import json, os, re, subprocess, sys, time, urllib.request
from html.parser import HTMLParser

R = "http://127.0.0.1:8082"
S = os.environ.get("GITEA_UI_STATE", os.path.expanduser("~/.gitea-ui"))
ENV = dict(os.environ, DISPLAY=":99")


def post(path, body):
    req = urllib.request.Request(R + path, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.loads(r.read())


def find(xpath, visible=True):
    r = post("/xpath/components", {"xpath": xpath, "onlyVisible": visible})
    if r.get("status") != "SUCCESS":
        raise SystemExit("find failed: %s" % r.get("message") or r)
    return r["elementList"]


def screen(e):
    """Replace parent-relative x/y with on-screen coordinates."""
    r = js('var p=component.getLocationOnScreen(); "" + p.x + "," + p.y + "," + component.getWidth() + "," + component.getHeight()', cid=e["id"])
    try:
        e["x"], e["y"], e["width"], e["height"] = map(int, r.split(","))
    except ValueError:
        raise SystemExit("cannot locate component: " + r)
    return e


def one(xpath, idx=0):
    els = find(xpath)
    if len(els) <= idx:
        raise SystemExit("NOT FOUND: %s (got %d)" % (xpath, len(els)))
    return screen(els[idx])


def xdo(*args):
    subprocess.run(["xdotool", *map(str, args)], env=ENV, check=True)


def click_el(e, button=1, dx=None, dy=None, double=False):
    x = e["x"] + (int(dx) if dx is not None else e["width"] // 2)
    y = e["y"] + (int(dy) if dy is not None else e["height"] // 2)
    xdo("mousemove", x, y)
    time.sleep(0.15)
    xdo("click", *(["--repeat", "2", "--delay", "80"] if double else []), button)
    return x, y


def unser(b):
    b = bytes((x + 256) % 256 for x in b)
    if b[:4] != b"\xac\xed\x00\x05":
        return repr(b)
    t = b[4]
    if t == 0x74:
        n = int.from_bytes(b[5:7], "big"); return b[7:7 + n].decode("utf-8", "replace")
    if t == 0x7C:
        n = int.from_bytes(b[5:13], "big"); return b[13:13 + n].decode("utf-8", "replace")
    if t == 0x70:
        return "null"
    return "<non-string result: %r>" % b[:80]


def js(script, edt=True, cid=None):
    path = ("/%s" % cid if cid else "") + "/js/retrieveAny"
    r = post(path, {"script": script, "runInEdt": edt})
    if r.get("status") != "SUCCESS":
        return "JS ERROR: " + (r.get("message") or "") + "\n" + (r.get("log") or "")
    return unser(r["bytes"])


class Tree(HTMLParser):
    SKIP = {"link", "script"}

    def __init__(self):
        super().__init__()
        self.depth = 0
        self.lines = []
        self.hidden_depth = None

    def handle_starttag(self, tag, attrs):
        if tag != "div":
            return
        a = dict(attrs)
        self.depth += 1
        if "javaclass" not in a:
            return
        if self.hidden_depth is not None:
            return
        if a.get("visible") == "false":
            self.hidden_depth = self.depth
            return
        bits = [a.get("class", "?")]
        for k in ("visible_text", "accessiblename", "tooltiptext", "text"):
            v = (a.get(k) or "").strip()
            if v and not any(v == prev for prev in bits[1:]):
                bits.append("%s=%r" % (k[:4], v[:140]))
        if a.get("enabled") == "false":
            bits.append("DISABLED")
        self.lines.append((self.depth, " ".join(bits)))

    def handle_endtag(self, tag):
        if tag != "div":
            return
        if self.hidden_depth is not None and self.depth == self.hidden_depth:
            self.hidden_depth = None
        self.depth -= 1


def html_for(xpath=None):
    if not xpath:
        with urllib.request.urlopen(R + "/", timeout=60) as r:
            return r.read().decode("utf-8", "replace")
    e = one(xpath)
    with urllib.request.urlopen(R + "/%s" % e["id"], timeout=60) as r:
        return r.read().decode("utf-8", "replace")


def tree_lines(root=None):
    t = Tree(); t.feed(html_for(None))
    lines = t.lines
    if root:
        out, rd = [], None
        for d, s in lines:
            if rd is None:
                if re.search(root, s):
                    rd = d; out.append((d, s))
            elif d > rd:
                out.append((d, s))
            else:
                break
        lines = out
    return lines


def tree(root=None, grep=None):
    lines = tree_lines(root)
    base = min((d for d, _ in lines), default=0)
    for d, s in lines:
        if grep and not re.search(grep, s, re.I):
            continue
        try:
            print("  " * (d - base) + s)
        except BrokenPipeError:
            return


def comp_texts(xpath, idx=0):
    e = one(xpath, idx)
    r = post("/%s/data" % e["id"], {})
    out = []
    for t in r["componentData"]["textDataList"]:
        out.append((t["text"], e["x"] + t["point"]["x"], e["y"] + t["point"]["y"]))
    return out


def click_text(xpath, text, nth=0, button=1, double=False, exact=False):
    hits = [t for t in comp_texts(xpath) if (t[0] == text if exact else text in t[0])]
    if len(hits) <= nth:
        raise SystemExit("TEXT NOT FOUND: %r in %s; have: %s" % (text, xpath, [t[0] for t in comp_texts(xpath)][:60]))
    _, x, y = hits[nth]
    xdo("mousemove", x, y); time.sleep(0.15)
    xdo("click", *(["--repeat", "2", "--delay", "80"] if double else []), button)
    return x, y


def main(a):
    cmd = a[0]
    if cmd == "find":
        for i, e in enumerate(find(a[1])):
            screen(e)
            print(i, e["className"].split(".")[-1], e["x"], e["y"], e["width"], e["height"])
    elif cmd in ("click", "dclick", "rclick"):
        idx = int(a[2]) if len(a) > 2 else 0
        dx, dy = (a[3], a[4]) if len(a) > 4 else (None, None)
        e = one(a[1], idx)
        print("clicked", click_el(e, 3 if cmd == "rclick" else 1, dx, dy, cmd == "dclick"))
    elif cmd == "reveal":
        e = find(a[1])[int(a[2]) if len(a) > 2 else 0]
        print(js('importClass(java.awt.Rectangle); component.scrollRectToVisible(new Rectangle(0,0,component.getWidth(),component.getHeight())); "ok"', cid=e["id"]))
    elif cmd == "ctexts":
        for t in comp_texts(a[1], int(a[2]) if len(a) > 2 else 0):
            print("%4d %4d  %s" % (t[1], t[2], t[0]))
    elif cmd in ("clicktext", "dclicktext", "rclicktext"):
        nth = int(a[3]) if len(a) > 3 else 0
        print("clicked", click_text(a[1], a[2], nth, 3 if cmd == "rclicktext" else 1, cmd == "dclicktext"))
    elif cmd == "tree":
        tree(a[1] if len(a) > 1 and a[1] else None, a[2] if len(a) > 2 else None)
    elif cmd == "texts":
        for _, s in tree_lines(a[1] if len(a) > 1 else None):
            m = re.findall(r"(?:visi|acce|tool|text)='([^']*)'|(?:visi|acce|tool|text)=\"([^\"]*)\"", s)
            for g in m:
                v = g[0] or g[1]
                if v: print(v)
    elif cmd == "js":
        print(js(a[1], edt=(len(a) < 3 or a[2] != "bg")))
    elif cmd == "cjs":
        print(js(a[2], cid=one(a[1])["id"]))
    elif cmd == "shot":
        os.makedirs(S + "/shots", exist_ok=True)
        p = "%s/shots/%s.png" % (S, a[1])
        subprocess.run(["import", "-window", "root", p], env=ENV, check=True)
        print(p)
    elif cmd == "key":
        xdo("key", "--delay", "60", *a[1:])
    elif cmd == "type":
        xdo("type", "--delay", "35", a[1])
    elif cmd == "wait":
        to = float(a[2]) if len(a) > 2 else 30
        end = time.time() + to
        while time.time() < end:
            try:
                if find(a[1]):
                    print("OK"); return
            except Exception:
                pass
            time.sleep(0.5)
        raise SystemExit("TIMEOUT waiting for " + a[1])
    else:
        print(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:] or ["help"])
