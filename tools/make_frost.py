"""Grows the frost that sits in the corners of glass panels. Run from the project root."""
import json, math, random
from PIL import Image, ImageDraw, ImageFilter

S, OVER = 256, 4
random.seed(11)
W = S * OVER
img = Image.new("L", (W, W), 0)
d = ImageDraw.Draw(img)


def fern(x, y, a, length, width, bright, depth):
    """A feather of ice: a curving spine with side needles at sixty degrees."""
    steps = max(6, int(length / 14))
    bend = random.uniform(-0.012, 0.012)
    for i in range(steps):
        t = i / steps
        nx, ny = x + math.cos(a) * length / steps, y + math.sin(a) * length / steps
        w = max(1, width * (1 - t * 0.85))
        d.line((x, y, nx, ny), fill=int(bright * (1 - t * 0.55)), width=int(round(w)))
        if depth > 0 and i > 0:
            for side in (-1, 1):
                if random.random() < 0.86:
                    l = length * (0.36 * (1 - t) + 0.05) * random.uniform(0.6, 1.1)
                    fern(nx, ny, a + side * math.radians(random.uniform(52, 64)), l, w * 0.6, bright * (0.8 - t * 0.2), depth - 1)
        x, y, a = nx, ny, a + bend


for i in range(9):
    along = random.uniform(0.0, 0.62) ** 1.2 * W
    edge = i % 2 == 0
    sx, sy = (along, -6) if edge else (-6, along)
    toward = math.radians(random.uniform(38, 74)) if edge else math.radians(random.uniform(16, 52))
    fern(sx, sy, toward, W * random.uniform(0.3, 0.62) * (1 - along / W * 0.75), 9, 235, 2)
fern(-6, -6, math.radians(45), W * 0.86, 11, 255, 2)
glow = img.filter(ImageFilter.GaussianBlur(14))
img = Image.blend(img, glow, 0.28)
img = img.resize((S, S), Image.LANCZOS)
px = img.load()
out = Image.new("RGBA", (S, S))
for y in range(S):
    for x in range(S):
        r = math.hypot(x, y) / S
        haze = max(0.0, 1 - r * 1.25) ** 2 * 70
        fade = max(0.0, min(1.0, (1.18 - r) * 2.2))
        out.putpixel((x, y), (255, 255, 255, int(min(255, (px[x, y] * 0.95 + haze) * fade))))
out.save("src/main/resources/assets/hollow/textures/frost.png")
open("src/main/resources/assets/hollow/textures/frost.png.mcmeta", "w").write(json.dumps({"texture": {"blur": True, "clamp": True}}))
print("frost", out.getextrema())
