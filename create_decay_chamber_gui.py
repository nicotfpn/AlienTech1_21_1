"""
Decay Chamber GUI Generator
============================
Uses the same Egyptian theme as mc_gui_generator.py.
Simplified layout: energy bar, fuel slot, 1 input, 1 output.
256×256 PNG. Panel 176×166 at (0,0).
"""

from PIL import Image, ImageDraw
import os

CONFIG = {
    "output": "decay_chamber_gui.png",
    "energy_bar":   {"x": 9,   "y": 8,  "w": 16, "h": 44},
    "coal_slot":    {"x": 8,   "y": 54},
    "input_slots":  [
        {"x": 56, "y": 34},   # single centered input
    ],
    "progress_bar": {"x": 80, "y": 36, "w": 34, "h": 10},
    "output_slot":  {"x": 120, "y": 33},
    "inventory":    {"x": 8,  "y": 84},
    "hotbar":       {"x": 8,  "y": 142},
}

# ── Egyptian Palette ────────────────────────────────────────────────────────
SAND_LIGHT   = (0xD4, 0xC4, 0x8A, 255)
SAND_MID     = (0xB8, 0xA2, 0x60, 255)
SAND_DARK    = (0x8C, 0x74, 0x38, 255)
SAND_DEEP    = (0x5A, 0x44, 0x1C, 255)

GOLD_BRIGHT  = (0xFF, 0xD7, 0x00, 255)
GOLD_MID     = (0xD4, 0xAF, 0x37, 255)
GOLD_DARK    = (0x8B, 0x6C, 0x00, 255)

LAPIS        = (0x1F, 0x5F, 0xAA, 255)
LAPIS_LIGHT  = (0x4A, 0x90, 0xD9, 255)
LAPIS_DARK   = (0x0A, 0x2A, 0x55, 255)

TURQ         = (0x00, 0xAA, 0x99, 255)
TURQ_LIGHT   = (0x33, 0xDD, 0xCC, 255)

INSET_BG     = (0x3C, 0x2A, 0x0A, 255)
INSET_TL     = (0x28, 0x1A, 0x04, 255)
INSET_BR     = (0x70, 0x55, 0x22, 255)

SLOT_TL      = (0x55, 0x55, 0x55, 255)
SLOT_BR      = (0xFF, 0xFF, 0xFF, 255)
SLOT_FACE    = (0x8B, 0x8B, 0x8B, 255)
SLOT_INNER   = (0x37, 0x37, 0x37, 255)

TRANSPARENT  = (0, 0, 0, 0)

# ── Helpers ─────────────────────────────────────────────────────────────────
def hline(img, x, y, w, c):
    if w <= 0: return
    ImageDraw.Draw(img).line([(x, y), (x+w-1, y)], fill=c)

def vline(img, x, y, h, c):
    if h <= 0: return
    ImageDraw.Draw(img).line([(x, y), (x, y+h-1)], fill=c)

def box(img, x, y, w, h, c):
    if w <= 0 or h <= 0: return
    ImageDraw.Draw(img).rectangle([x, y, x+w-1, y+h-1], fill=c)

def v_grad(img, x, y, w, h, top, bot):
    for i in range(h):
        t = i / max(h-1, 1)
        c = tuple(int(top[k]+(bot[k]-top[k])*t) for k in range(3))+(255,)
        hline(img, x, y+i, w, c)

def h_grad(img, x, y, w, h, left, right):
    for i in range(w):
        t = i / max(w-1, 1)
        c = tuple(int(left[k]+(right[k]-left[k])*t) for k in range(3))+(255,)
        vline(img, x+i, y, h, c)

def put(img, x, y, c):
    if 0 <= x < 256 and 0 <= y < 256:
        img.putpixel((x, y), c)

# ── Egyptian decorative helpers ──────────────────────────────────────────────
def draw_egyptian_border(img, x, y, w, h):
    hline(img, x,     y,     w, GOLD_MID);    vline(img, x,     y,     h, GOLD_MID)
    hline(img, x,     y+h-1, w, GOLD_DARK);   vline(img, x+w-1, y,     h, GOLD_DARK)
    hline(img, x+1,   y+1,   w-2, SAND_DEEP); vline(img, x+1,   y+1,   h-2, SAND_DEEP)
    hline(img, x+1,   y+h-2, w-2, SAND_MID);  vline(img, x+w-2, y+1,   h-2, SAND_MID)
    hline(img, x+2,   y+2,   w-4, SAND_DARK); vline(img, x+2,   y+2,   h-4, SAND_DARK)
    hline(img, x+2,   y+h-3, w-4, SAND_LIGHT);vline(img, x+w-3, y+2,   h-4, SAND_LIGHT)

def draw_hieroglyph_strip(img, x, y, w):
    for i in range(0, w, 4):
        cx = x + i + 1
        put(img, cx,   y,   GOLD_MID)
        put(img, cx-1, y+1, GOLD_DARK)
        put(img, cx+1, y+1, GOLD_DARK)
        put(img, cx,   y+2, GOLD_MID)
        put(img, cx,   y+1, GOLD_BRIGHT)

def draw_corner_ankh(img, x, y):
    pts = [
        (2,2),(2,3),(2,4),(2,5),(2,6),
        (0,3),(1,3),(3,3),(4,3),
        (1,1),(2,0),(3,1),(1,2),(3,2),
    ]
    for px2, py2 in pts:
        put(img, x+px2, y+py2, GOLD_MID)
    put(img, x+2, y+0, GOLD_BRIGHT)

def draw_eye_of_ra(img, cx, cy):
    pts = [
        (0,1),(1,0),(2,0),(3,0),(4,0),(5,1),(6,1),(5,2),(4,3),(3,3),(2,3),(1,2),(0,2),
    ]
    for px2, py2 in pts:
        put(img, cx-3+px2, cy-1+py2, GOLD_MID)
    for px2,py2,c in [(3,1,SAND_DEEP),(3,2,SAND_DEEP)]:
        put(img, cx-3+px2, cy-1+py2, c)
    put(img, cx-3+2, cy-1+1, GOLD_BRIGHT)

def draw_column_decoration(img, x, y, h):
    for i in range(0, h, 3):
        put(img, x, y+i, GOLD_DARK)
        if i % 6 == 0:
            put(img, x, y+i, GOLD_MID)

# ── Core components ──────────────────────────────────────────────────────────
def draw_panel(img):
    v_grad(img, 0, 0, 176, 166, SAND_LIGHT, SAND_MID)
    draw_egyptian_border(img, 0, 0, 176, 166)
    draw_hieroglyph_strip(img, 4, 12, 168)
    draw_corner_ankh(img, 4,  4)
    draw_corner_ankh(img, 163, 4)
    draw_column_decoration(img, 3, 16, 148)
    draw_column_decoration(img, 172, 16, 148)
    draw_eye_of_ra(img, 88, 7)

def draw_inset(img, x, y, w, h):
    box(img, x, y, w, h, INSET_BG)
    hline(img, x, y,     w, INSET_TL); vline(img, x, y,     h, INSET_TL)
    hline(img, x, y+h-1, w, INSET_BR); vline(img, x+w-1, y, h, INSET_BR)

def draw_slot(img, x, y):
    box(img, x, y, 18, 18, SLOT_FACE)
    hline(img, x,    y,    18, SLOT_TL)
    vline(img, x,    y,    18, SLOT_TL)
    hline(img, x,    y+17, 18, SLOT_BR)
    vline(img, x+17, y,    18, SLOT_BR)
    hline(img, x+1,  y+1,  16, SLOT_INNER)
    vline(img, x+1,  y+1,  16, SLOT_INNER)

def draw_output_slot(img, x, y):
    box(img, x-1, y-1, 20, 20, GOLD_MID)
    hline(img, x-1, y-1, 20, GOLD_BRIGHT)
    vline(img, x-1, y-1, 20, GOLD_BRIGHT)
    hline(img, x-1, y+18, 20, GOLD_DARK)
    vline(img, x+18, y-1, 20, GOLD_DARK)
    draw_slot(img, x, y)

def draw_energy_fill_sprite(img, x, y, w, h):
    v_grad(img, x, y, w, h, TURQ_LIGHT, LAPIS_DARK)
    vline(img, x+1, y+1, h-2, (0xCC, 0xFF, 0xEE, 120))
    hline(img, x, y,     w, INSET_TL); vline(img, x,     y, h, INSET_TL)
    hline(img, x, y+h-1, w, INSET_BR); vline(img, x+w-1, y, h, INSET_BR)

def draw_progress_fill_sprite(img, x, y, w, h):
    h_grad(img, x, y, w, h, GOLD_DARK, GOLD_BRIGHT)
    hline(img, x+1, y+h//2-1, w-2, (0xFF, 0xFF, 0xCC, 120))
    hline(img, x, y,     w, INSET_TL); vline(img, x,     y, h, INSET_TL)
    hline(img, x, y+h-1, w, INSET_BR); vline(img, x+w-1, y, h, INSET_BR)

def draw_inventory(img, x, y):
    for row in range(3):
        for col in range(9):
            draw_slot(img, x + col*18, y + row*18)

def draw_hotbar(img, x, y):
    for col in range(9):
        draw_slot(img, x + col*18, y)

def draw_separator(img, x, y, w):
    hline(img, x, y-3, w, GOLD_DARK)
    hline(img, x, y-2, w, GOLD_MID)
    hline(img, x, y-1, w, SAND_DARK)

# ── Atlas UV packer ─────────────────────────────────────────────────────────
def pack_atlas(cfg):
    ATLAS_X = 176
    sprites = [
        ("energy",   cfg["energy_bar"]["w"],  cfg["energy_bar"]["h"]),
        ("progress", cfg["progress_bar"]["w"], cfg["progress_bar"]["h"]),
    ]
    uvs = {}
    cy = 0
    for name, w, h in sprites:
        uvs[name] = (ATLAS_X, cy, w, h)
        cy += h + 2
    return uvs

# ── Generate ─────────────────────────────────────────────────────────────────
def generate(cfg):
    img = Image.new("RGBA", (256, 256), TRANSPARENT)

    draw_panel(img)

    eb = cfg["energy_bar"]
    draw_inset(img, eb["x"], eb["y"], eb["w"], eb["h"])

    cs = cfg["coal_slot"]
    draw_slot(img, cs["x"], cs["y"])

    for s in cfg["input_slots"]:
        draw_slot(img, s["x"], s["y"])

    pb = cfg["progress_bar"]
    draw_inset(img, pb["x"], pb["y"], pb["w"], pb["h"])
    for tx in range(8, pb["w"]-2, 8):
        vline(img, pb["x"]+tx, pb["y"]+2, pb["h"]-4, INSET_BR)

    os2 = cfg["output_slot"]
    draw_output_slot(img, os2["x"], os2["y"])

    inv = cfg["inventory"]
    hb  = cfg["hotbar"]
    draw_separator(img, inv["x"], inv["y"], 9*18)
    draw_inventory(img, inv["x"], inv["y"])
    draw_hotbar(img, hb["x"], hb["y"])

    uvs = pack_atlas(cfg)

    ae = uvs["energy"]
    draw_energy_fill_sprite(img, ae[0], ae[1], eb["w"], eb["h"])

    ap = uvs["progress"]
    draw_progress_fill_sprite(img, ap[0], ap[1], pb["w"], pb["h"])

    # Save to the mod's textures/gui directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(script_dir, "src", "main", "resources", "assets", "alientech", "textures", "gui", cfg["output"])
    img.save(out)

    print(f"Saved: {out}\n")
    print("=" * 60)
    print("  JAVA CONSTANTS")
    print("=" * 60)
    print(f"private static final int ENERGY_X    = {eb['x']}, ENERGY_Y    = {eb['y']};")
    print(f"private static final int ENERGY_W    = {eb['w']}, ENERGY_H    = {eb['h']};")
    print(f"// energy atlas UV   -> ({ae[0]}, {ae[1]})  {ae[2]}x{ae[3]}")
    print()
    print(f"private static final int COAL_X      = {cs['x']}, COAL_Y      = {cs['y']};")
    print()
    for i, s in enumerate(cfg["input_slots"], 1):
        print(f"private static final int INPUT{i}_X    = {s['x']}, INPUT{i}_Y    = {s['y']};")
    print()
    print(f"private static final int PROGRESS_X  = {pb['x']}, PROGRESS_Y  = {pb['y']};")
    print(f"private static final int PROGRESS_W  = {pb['w']}, PROGRESS_H  = {pb['h']};")
    print(f"// progress atlas UV -> ({ap[0]}, {ap[1]})  {ap[2]}x{ap[3]}")
    print()
    print(f"private static final int OUTPUT_X    = {os2['x']}, OUTPUT_Y    = {os2['y']};")
    print(f"private static final int INV_X       = {inv['x']}, INV_Y       = {inv['y']};")
    print(f"private static final int HB_X        = {hb['x']},  HB_Y        = {hb['y']};")
    print("=" * 60)

generate(CONFIG)
