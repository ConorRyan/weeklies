"""Regenerate the Google Play feature graphic (assets/store/feature-graphic.png).

Run: python3 scripts/generate-feature-graphic.py
Requires Pillow (pip install pillow). The 1024x500 output matches the Play
Console feature graphic spec; keep content inside the center safe area.
"""

from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = "/home/linux/weeklies"
W, H = 1024, 500
BG_TOP = (230, 244, 254)     # #E6F4FE, the adaptive icon background color
BG_BOTTOM = (196, 226, 248)
TEXT = (17, 24, 28)          # #11181C, app text color
MUTED = (74, 96, 110)

img = Image.new("RGB", (W, H))
d = ImageDraw.Draw(img)
for y in range(H):
    t = y / (H - 1)
    c = tuple(int(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * t) for i in range(3))
    d.line([(0, y), (W, y)], fill=c)

icon = Image.open(f"{ROOT}/assets/images/icon.png").convert("RGB")
size = 240
icon_resized = icon.resize((size, size), Image.LANCZOS)
radius = 56
mask = Image.new("L", (size, size), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
rounded = Image.new("RGBA", (size, size))
rounded.paste(icon_resized, (0, 0), mask)

shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
sdraw = ImageDraw.Draw(shadow)
ix, iy = (W - size) // 2, 56
sdraw.rounded_rectangle(
    [ix + 6, iy + 10, ix + size + 6, iy + size + 10], radius=radius, fill=(10, 40, 70, 60)
)
shadow = shadow.filter(ImageFilter.GaussianBlur(14))
img.paste(Image.new("RGB", (W, H), (255, 255, 255)), (0, 0), shadow.split()[3])
img.paste(rounded, (ix, iy), rounded)

bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 74)
reg = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 34)
title = "Weeklies"
tagline = "Your week of meals, planned once."
d = ImageDraw.Draw(img)
tw = d.textlength(title, font=bold)
d.text(((W - tw) / 2, 336), title, font=bold, fill=TEXT)
gw = d.textlength(tagline, font=reg)
d.text(((W - gw) / 2, 432), tagline, font=reg, fill=MUTED)

img.save(f"{ROOT}/assets/store/feature-graphic.png")
print("saved", img.size)
