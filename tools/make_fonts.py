"""Writes one font JSON per pixel size, so every piece of text is drawn 1:1 and stays sharp. Run from the project root."""
import json, os
D = "src/main/resources/assets/hollow/font/"
for f in os.listdir(D):
    if f.endswith(".json"): os.remove(D + f)
def out(name, ttf, size, over):
    json.dump({"providers": [{"type": "ttf", "file": "hollow:%s.ttf" % ttf, "size": size, "oversample": over}]}, open(D + name + ".json", "w"), separators=(",", ":"))
SMALL = range(7, 41)
for px in SMALL:
    out("text%d" % px, "urbanist_semi", px, 1.0)
    out("bold%d" % px, "urbanist_heavy", px, 1.0)
for px in list(SMALL) + list(range(44, 129, 4)):
    out("display%d" % px, "unbounded_black", px, 1.0)
# Labels on the game's own widgets are drawn by the game, so these match the GUI scale instead.
for s in range(1, 9):
    out("label%d" % s, "urbanist_heavy", 9.0, float(s))
print(len([f for f in os.listdir(D) if f.endswith(".json")]))
