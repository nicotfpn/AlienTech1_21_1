"""
╔══════════════════════════════════════════════════════════════════╗
║        AlienTech GUI Texture Generator  v2.1                    ║
║        NeoForge 1.21.1  ·  256×256 RGBA  ·  All machines        ║
║                                                                  ║
║  HOW TO RUN (Windows):                                           ║
║    Double-click this file  OR  run:  python alientech_gui_gen.py ║
║                                                                  ║
║  OUTPUT: PNG files appear in the same folder as this script.     ║
║  Then copy them to:                                              ║
║    src/main/resources/assets/alientech/textures/gui/             ║
╚══════════════════════════════════════════════════════════════════╝
"""

# ── auto-install Pillow if missing ────────────────────────────────────────────
import sys, subprocess, os, time, math

def _ensure_pillow():
    try:
        import PIL
    except ImportError:
        print("[setup] Pillow not found — installing automatically...")
        try:
            subprocess.check_call(
                [sys.executable, "-m", "pip", "install", "Pillow", "--quiet"],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            )
            print("[setup] Pillow installed successfully.\n")
        except Exception as e:
            print(f"[ERROR] Could not install Pillow automatically: {e}")
            print("        Please run:  pip install Pillow")
            _pause_and_exit(1)

_ensure_pillow()
from PIL import Image, ImageDraw

# ── output directory = same folder as this script ─────────────────────────────
OUT_DIR = os.path.dirname(os.path.abspath(__file__))

def _pause_and_exit(code=0):
    """Keep the console window open on Windows when double-clicked."""
    if os.name == "nt":          # Windows only
        input("\nPress Enter to exit...")
    sys.exit(code)


# ═════════════════════════════════════════════════════════════════════════════
#  COLOUR PALETTE
# ═════════════════════════════════════════════════════════════════════════════

BG_TOP       = (0xC8, 0xB8, 0x78, 255)   # warm sandstone light
BG_BOT       = (0xAA, 0x98, 0x58, 255)   # warm sandstone dark

BORDER_DARK  = (0x58, 0x44, 0x14, 255)   # outer border
BORDER_LIGHT = (0xE2, 0xD0, 0x8A, 255)   # inner bevel highlight

# Vanilla slots — DO NOT CHANGE, must match MC exactly
SLOT_TL      = (0x55, 0x55, 0x55, 255)
SLOT_BR      = (0xFF, 0xFF, 0xFF, 255)
SLOT_FACE    = (0x8B, 0x8B, 0x8B, 255)
SLOT_INNER   = (0x37, 0x37, 0x37, 255)

# Output-slot gold ring
GOLD_HI      = (0xFF, 0xD7, 0x00, 255)
GOLD_MID     = (0xD4, 0xAF, 0x37, 255)
GOLD_DARK    = (0x8B, 0x6C, 0x00, 255)

# Inset backgrounds
INSET_BG     = (0x1C, 0x14, 0x06, 255)
INSET_TL     = (0x12, 0x0C, 0x02, 255)
INSET_BR     = (0x50, 0x3C, 0x14, 255)

# Separator
SEP_D        = (0x88, 0x70, 0x30, 255)
SEP_L        = (0xD8, 0xC8, 0x80, 255)

# Energy fill  (turquoise → lapis → deep blue)
EN_TOP       = (0x44, 0xEE, 0xDD, 255)
EN_MID       = (0x1A, 0x7A, 0xBB, 255)
EN_BOT       = (0x06, 0x1E, 0x44, 255)

# Progress fill  (dark gold → bright gold)
PR_L         = (0x80, 0x60, 0x00, 255)
PR_R         = (0xFF, 0xD7, 0x00, 255)

# Burn fill  (violet → hot orange)  — QVT
BU_TOP       = (0x44, 0x08, 0x44, 255)
BU_BOT       = (0xFF, 0x88, 0x00, 255)

# Entropy fill  (toxic green → dark violet) — Decay Chamber
DC_TOP       = (0x22, 0xAA, 0x44, 255)
DC_BOT       = (0x18, 0x08, 0x28, 255)

TRANSPARENT  = (0, 0, 0, 0)


# ═════════════════════════════════════════════════════════════════════════════
#  LOW-LEVEL PRIMITIVES
# ═════════════════════════════════════════════════════════════════════════════

def _c(v): return max(0, min(255, int(v)))

def px(img, x, y, col):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), col)

def hline(img, x, y, w, col):
    for i in range(max(0, w)): px(img, x+i, y, col)

def vline(img, x, y, h, col):
    for i in range(max(0, h)): px(img, x, y+i, col)

def box(img, x, y, w, h, col):
    if w > 0 and h > 0:
        ImageDraw.Draw(img).rectangle([x, y, x+w-1, y+h-1], fill=col)

def v_grad(img, x, y, w, h, top, bot):
    for i in range(h):
        t = i / max(h-1, 1)
        col = (_c(top[0]+(bot[0]-top[0])*t), _c(top[1]+(bot[1]-top[1])*t),
               _c(top[2]+(bot[2]-top[2])*t), 255)
        hline(img, x, y+i, w, col)

def h_grad(img, x, y, w, h, left, right):
    for i in range(w):
        t = i / max(w-1, 1)
        col = (_c(left[0]+(right[0]-left[0])*t), _c(left[1]+(right[1]-left[1])*t),
               _c(left[2]+(right[2]-left[2])*t), 255)
        vline(img, x+i, y, h, col)

def blend(img, x, y, rgb, alpha):
    """Alpha-blend rgb onto existing pixel at (x,y)."""
    if not (0 <= x < img.width and 0 <= y < img.height): return
    bg = img.getpixel((x, y))
    a  = alpha / 255.0
    img.putpixel((x, y), (_c(rgb[0]*a + bg[0]*(1-a)),
                           _c(rgb[1]*a + bg[1]*(1-a)),
                           _c(rgb[2]*a + bg[2]*(1-a)), 255))

def inset_border(img, x, y, w, h):
    hline(img, x, y,     w, INSET_TL); vline(img, x,     y, h, INSET_TL)
    hline(img, x, y+h-1, w, INSET_BR); vline(img, x+w-1, y, h, INSET_BR)


# ═════════════════════════════════════════════════════════════════════════════
#  SHARED STRUCTURAL ELEMENTS
# ═════════════════════════════════════════════════════════════════════════════

def draw_panel(img):
    """Clean sandstone gradient + minimal 2-px bevelled border. Zero decorations."""
    v_grad(img, 0, 0, 176, 166, BG_TOP, BG_BOT)
    hline(img, 0,   0,   176, BORDER_DARK);  vline(img, 0,   0, 166, BORDER_DARK)
    hline(img, 0,   165, 176, BORDER_DARK);  vline(img, 175, 0, 166, BORDER_DARK)
    hline(img, 1,   1,   174, BORDER_LIGHT); vline(img, 1,   1, 164, BORDER_LIGHT)
    hline(img, 1,   164, 174, BORDER_DARK);  vline(img, 174, 1, 164, BORDER_DARK)

def draw_slot(img, x, y):
    box(img, x, y, 18, 18, SLOT_FACE)
    hline(img, x,   y,    18, SLOT_TL);    vline(img, x,    y,   18, SLOT_TL)
    hline(img, x,   y+17, 18, SLOT_BR);    vline(img, x+17, y,   18, SLOT_BR)
    hline(img, x+1, y+1,  16, SLOT_INNER); vline(img, x+1,  y+1, 16, SLOT_INNER)

def draw_output_slot(img, x, y):
    box(img, x-1, y-1, 20, 20, GOLD_MID)
    hline(img, x-1, y-1,  20, GOLD_HI);   vline(img, x-1,  y-1, 20, GOLD_HI)
    hline(img, x-1, y+18, 20, GOLD_DARK); vline(img, x+18, y-1, 20, GOLD_DARK)
    draw_slot(img, x, y)

def draw_inset(img, x, y, w, h):
    box(img, x, y, w, h, INSET_BG)
    inset_border(img, x, y, w, h)

def draw_inventory(img, x, y):
    for r in range(3):
        for c in range(9):
            draw_slot(img, x + c*18, y + r*18)

def draw_hotbar(img, x, y):
    for c in range(9):
        draw_slot(img, x + c*18, y)

def draw_separator(img, x, y, w):
    hline(img, x, y-2, w, SEP_D)
    hline(img, x, y-1, w, SEP_L)


# ═════════════════════════════════════════════════════════════════════════════
#  ATLAS SPRITES  (rendered into x >= 176 of the 256x256 canvas)
# ═════════════════════════════════════════════════════════════════════════════

def sprite_energy(img, x, y, w, h):
    """
    Three-stop vertical fill: bright cyan → lapis → deep navy.
    Left-edge glass highlight — the detail the user loved on the battery.
    """
    h2 = h // 2
    v_grad(img, x, y,    w, h2,   EN_TOP, EN_MID)
    v_grad(img, x, y+h2, w, h-h2, EN_MID, EN_BOT)
    # glass highlight: bright at top, fades with power curve
    for i in range(h):
        a = _c(200 * (1 - i / max(h-1, 1)) ** 0.6)
        blend(img, x+1, y+i, (0xFF, 0xFF, 0xFF), a)
    inset_border(img, x, y, w, h)

def sprite_progress(img, x, y, w, h):
    """Dark-gold → bright-gold horizontal fill with sine shimmer at mid-height."""
    h_grad(img, x, y, w, h, PR_L, PR_R)
    for i in range(1, w-1):
        a = _c(90 * math.sin(i / max(w-1, 1) * math.pi))
        blend(img, x+i, y + h//2, (0xFF, 0xFF, 0xFF), a)
    inset_border(img, x, y, w, h)

def sprite_entropy(img, x, y, w, h):
    """Toxic green → deep violet — Decay Chamber entropy bar."""
    v_grad(img, x, y, w, h, DC_TOP, DC_BOT)
    for i in range(h):
        a = _c(70 * (1 - i / max(h-1, 1)))
        blend(img, x+1, y+i, (0xAA, 0xFF, 0xAA), a)
    inset_border(img, x, y, w, h)

def sprite_burn(img, x, y, w, h):
    """Hot orange bottom → cold violet top — QVT graviton burn remaining."""
    v_grad(img, x, y, w, h, BU_TOP, BU_BOT)
    cx = x + w // 2
    for i in range(h):
        t = i / max(h-1, 1)           # 0=top(cool) 1=bottom(hot)
        a = _c(160 * (1 - t) ** 0.5)  # flicker brighter near top where it's active
        blend(img, cx, y+i, (0xFF, 0xEE, 0x44), a)
    inset_border(img, x, y, w, h)

def sprite_energy_chamber(img, x, y, w, h):
    """
    Large QVT energy display.
    Three-stop gradient + glass highlight column + subtle scan-lines every 4 rows.
    """
    h3 = h // 3
    v_grad(img, x, y,    w, h3,   EN_TOP, EN_MID)
    v_grad(img, x, y+h3, w, h-h3, EN_MID, EN_BOT)
    for i in range(h):
        a = _c(180 * (1 - i / max(h-1, 1)) ** 0.6)
        blend(img, x+1, y+i, (0xFF, 0xFF, 0xFF), a)
    for i in range(0, h, 4):
        for j in range(w):
            blend(img, x+j, y+i, (0, 0, 0), 28)
    inset_border(img, x, y, w, h)


# ═════════════════════════════════════════════════════════════════════════════
#  THEMATIC MICRO-DETAILS  (machine-specific pixel art icons)
# ═════════════════════════════════════════════════════════════════════════════

def draw_energy_ticks(img, bx, by, bw, bh, fracs=(0.25, 0.5, 0.75)):
    """
    Tick marks inside a bar at given fill fractions.
    2-px bright dashes on both inner walls — the detail the user loved.
    Bars fill bottom-to-top, so fracs map accordingly.
    """
    for f in fracs:
        ty = by + _c(bh * (1.0 - f))
        px(img, bx+1,    ty, BORDER_LIGHT)
        px(img, bx+2,    ty, BORDER_LIGHT)
        px(img, bx+bw-2, ty, BORDER_LIGHT)
        px(img, bx+bw-3, ty, BORDER_LIGHT)


def draw_flow_arrow(img, x, y):
    """
    7×5 right-pointing gold arrow — shows item flow direction.
    """
    # row pattern (0-indexed columns that are filled per row)
    rows = [
        [3],
        [0,1,2,3,4],
        [0,1,2,3,4,5,6],
        [0,1,2,3,4],
        [3],
    ]
    for dy, cols in enumerate(rows):
        for dx in cols:
            col = GOLD_HI if dx >= 4 else GOLD_MID
            px(img, x+dx, y+dy, col)
    # shadow on right tip
    px(img, x+6, y+2, GOLD_HI)
    px(img, x+5, y+1, GOLD_DARK)
    px(img, x+5, y+3, GOLD_DARK)


def draw_flame(img, x, y):
    """6×9 flame icon — marks fuel slots."""
    Y = (0xFF, 0xEE, 0x00, 255)
    O = (0xFF, 0x77, 0x00, 255)
    R = (0xBB, 0x22, 0x00, 255)
    grid = [
        "·YY···",
        "·YYO··",
        "OYYOO·",
        "OYYOOO",
        "OOOOOO",
        "OOOOOO",
        "RRRRRR",
        "·RRRR·",
        "··RR··",
    ]
    lut = {'Y': Y, 'O': O, 'R': R}
    for row, line in enumerate(grid):
        for col, ch in enumerate(line):
            if ch in lut:
                px(img, x+col, y+row, lut[ch])


def draw_skull(img, x, y):
    """7×8 pixel skull — marks the Decay Chamber mob input."""
    B = (0xDD, 0xDD, 0xCC, 255)
    D = (0x11, 0x0A, 0x11, 255)
    grid = [
        "·BBBBB·",
        "BBBBBBB",
        "BDBBBDB",
        "BBBBBBB",
        "·BDBDB·",
        "·BBBBB·",
        "·B·B·B·",
    ]
    for row, line in enumerate(grid):
        for col, ch in enumerate(line):
            if   ch == 'B': px(img, x+col, y+row, B)
            elif ch == 'D': px(img, x+col, y+row, D)


def draw_pyramid(img, x, y, rows=7):
    """Gold gradient pyramid silhouette — marks the Pyramid Core slot."""
    for r in range(rows):
        w    = 1 + r * 2
        left = x + (rows - 1 - r)
        t    = r / max(rows-1, 1)
        col  = (_c(GOLD_HI[0]*(1-t) + GOLD_DARK[0]*t),
                _c(GOLD_HI[1]*(1-t) + GOLD_DARK[1]*t),
                _c(GOLD_HI[2]*(1-t) + GOLD_DARK[2]*t), 255)
        hline(img, left, y+r, w, col)
    hline(img, x, y+rows, rows*2-1, BORDER_DARK)


def draw_lightning(img, x, y):
    """6×11 lightning bolt — energy flow from burn bar to energy chamber."""
    Y = GOLD_HI
    G = GOLD_MID
    D = GOLD_DARK
    bolt = [
        "···GG·",
        "··GYY·",
        "·GYYY·",
        "·YYYY·",
        "GYYYY·",
        "·YYYY·",
        "·YYYG·",
        "··YGG·",
        "··GG··",
        "·GG···",
        "·D····",
    ]
    lut = {'Y': Y, 'G': G, 'D': D}
    for row, line in enumerate(bolt):
        for col, ch in enumerate(line):
            if ch in lut:
                px(img, x+col, y+row, lut[ch])


def draw_graviton_symbol(img, x, y):
    """7×7 orbital symbol — represents the decaying graviton fuel item."""
    C  = (0xCC, 0x88, 0xFF, 255)
    C2 = (0xFF, 0xBB, 0xFF, 255)
    W  = (0xFF, 0xFF, 0xFF, 255)
    grid = [
        "··CCC··",
        "·C···C·",
        "C·C2C·C",
        "C·2W2·C",
        "C·C2C·C",
        "·C···C·",
        "··CCC··",
    ]
    lut = {'C': C, '2': C2, 'W': W}
    for row, line in enumerate(grid):
        for col, ch in enumerate(line):
            if ch in lut:
                px(img, x+col, y+row, lut[ch])


def draw_charge_arrows(img, x, y):
    """
    UP + DOWN direction arrows beside the battery slots.
    Gold UP   = battery charges the item in the top slot.
    Blue DOWN = item in bottom slot discharges into the battery.
    """
    UP = GOLD_MID
    DN = (0x55, 0xBB, 0xFF, 255)

    # UP arrow (7 wide, 5 tall) pointing upward
    up_rows = [[3], [2,3,4], [1,2,3,4,5], [0,1,2,3,4,5,6], [2,3,4]]
    for dy, cols in enumerate(up_rows):
        for dx in cols:
            px(img, x+dx, y+dy, UP)

    # DOWN arrow (7 wide, 5 tall) below, pointing downward
    oy = y + 8
    dn_rows = [[2,3,4], [0,1,2,3,4,5,6], [1,2,3,4,5], [2,3,4], [3]]
    for dy, cols in enumerate(dn_rows):
        for dx in cols:
            px(img, x+dx, oy+dy, DN)


def draw_battery_scanlines(img, bx, by, bw, bh):
    """Horizontal scan-lines inside the battery chamber every 3 rows — CRT readout feel."""
    for sy in range(1, bh-1, 3):
        for sx in range(1, bw-1):
            blend(img, bx+sx, by+sy, (0, 0, 0), 45)


def draw_circuit_corners(img, bx, by, bw, bh):
    """
    L-shaped engineering corner traces just outside the QVT energy chamber.
    5-px arms, 1-px wide, in dark brown — subtle but clearly technical.
    """
    C   = (0x48, 0x38, 0x10, 255)
    DOT = (0x60, 0x50, 0x20, 255)
    pad = 3

    for (cx, cy, hdx, vdy) in [
        (bx-pad,       by-pad,       +1, +1),   # top-left
        (bx+bw-1+pad,  by-pad,       -1, +1),   # top-right
        (bx-pad,       by+bh-1+pad,  +1, -1),   # bot-left
        (bx+bw-1+pad,  by+bh-1+pad,  -1, -1),   # bot-right
    ]:
        px(img, cx, cy, DOT)
        for i in range(1, 6):
            px(img, cx + hdx*i, cy,      C)   # horizontal arm
            px(img, cx,         cy+vdy*i, C)  # vertical arm


# ═════════════════════════════════════════════════════════════════════════════
#  ATLAS PACKER + VALIDATOR
# ═════════════════════════════════════════════════════════════════════════════

def pack_atlas(sprites):
    """Pack sprites vertically at x=176 with 2-px gaps. Returns {name: (x,y,w,h)}."""
    uvs = {}
    cy  = 0
    for name, sw, sh in sprites:
        if 176 + sw > 256:
            raise ValueError(f"Sprite '{name}' width {sw} exceeds atlas bounds (max 80 px).")
        if cy + sh > 256:
            raise ValueError(f"Sprite '{name}' overflows canvas at y={cy}.")
        uvs[name] = (176, cy, sw, sh)
        cy += sh + 2
    return uvs

def _atlas_ok(uvs):
    items = list(uvs.values())
    for i, (ax,ay,aw,ah) in enumerate(items):
        for j, (bx,by,bw,bh) in enumerate(items):
            if i >= j: continue
            if ax < bx+bw and ax+aw > bx and ay < by+bh and ay+ah > by:
                return False, f"overlap at ({ax},{ay}) vs ({bx},{by})"
        if ax+aw > 256 or ay+ah > 256:
            return False, f"out of bounds at ({ax},{ay})"
    return True, "ok"


# ═════════════════════════════════════════════════════════════════════════════
#  MACHINE GENERATORS
# ═════════════════════════════════════════════════════════════════════════════

def gen_primal_catalyst():
    """
    Primal Catalyst — 3-input alchemical processor.

    Theme  : Alchemy / transformation
    Details: flame above coal slot · single centred flow arrow · energy bar ticks
    """
    EB = (9, 8, 16, 44)
    CS = (8, 54)
    IS = [(56,14),(56,34),(56,54)]
    PB = (80, 36, 34, 10)
    OS = (120, 33)
    IV = (8, 84); HB = (8, 142)

    img = Image.new("RGBA", (256, 256), TRANSPARENT)
    draw_panel(img)
    draw_inset(img, *EB)
    draw_energy_ticks(img, *EB)
    draw_slot(img, *CS)
    draw_flame(img, CS[0]+5, CS[1]-11)
    for sx,sy in IS: draw_slot(img, sx, sy)
    # Single centred flow arrow in the gap (inputs right=74, progress left=80, gap=6px)
    draw_flow_arrow(img, 74, IS[1][1]+5)
    draw_inset(img, *PB)
    draw_output_slot(img, *OS)
    draw_separator(img, IV[0], IV[1], 9*18)
    draw_inventory(img, *IV)
    draw_hotbar(img, *HB)

    uvs = pack_atlas([("energy", EB[2], EB[3]), ("progress", PB[2], PB[3])])
    sprite_energy(img, *uvs["energy"])
    sprite_progress(img, *uvs["progress"])

    return img, uvs, dict(
        ENERGY_X=EB[0], ENERGY_Y=EB[1], ENERGY_W=EB[2], ENERGY_H=EB[3],
        _energy_uv=uvs["energy"][:2],
        COAL_X=CS[0], COAL_Y=CS[1],
        PROGRESS_X=PB[0], PROGRESS_Y=PB[1], PROGRESS_W=PB[2], PROGRESS_H=PB[3],
        _progress_uv=uvs["progress"][:2],
        OUTPUT_X=OS[0], OUTPUT_Y=OS[1],
    )


def gen_decay_chamber():
    """
    Decay Chamber — mob-decay entropy processor.

    Theme  : Biological horror / entropy
    Details: skull above input slot · flame above fuel · flow arrow · toxic entropy fill
    """
    EB = (9, 8, 16, 44)
    FS = (8, 54)
    IN = (56, 34)
    PB = (80, 36, 34, 10)
    OS = (120, 33)
    IV = (8, 84); HB = (8, 142)

    img = Image.new("RGBA", (256, 256), TRANSPARENT)
    draw_panel(img)
    draw_inset(img, *EB)
    draw_energy_ticks(img, *EB)
    draw_slot(img, *FS)
    draw_flame(img, FS[0]+5, FS[1]-11)
    draw_slot(img, *IN)
    draw_skull(img, IN[0] + (18-7)//2, IN[1]-10)
    draw_flow_arrow(img, IN[0]+18, IN[1]+5)
    draw_inset(img, *PB)
    draw_output_slot(img, *OS)
    draw_separator(img, IV[0], IV[1], 9*18)
    draw_inventory(img, *IV)
    draw_hotbar(img, *HB)

    uvs = pack_atlas([("energy", EB[2], EB[3]), ("progress", PB[2], PB[3])])
    sprite_entropy(img, *uvs["energy"])
    sprite_progress(img, *uvs["progress"])

    return img, uvs, dict(
        ENERGY_X=EB[0], ENERGY_Y=EB[1], ENERGY_W=EB[2], ENERGY_H=EB[3],
        _energy_uv=uvs["energy"][:2],
        PROGRESS_X=PB[0], PROGRESS_Y=PB[1], PROGRESS_W=PB[2], PROGRESS_H=PB[3],
        _progress_uv=uvs["progress"][:2],
        OUTPUT_X=OS[0], OUTPUT_Y=OS[1],
    )


def gen_pyramid_core():
    """
    Pyramid Core — FE generator powered by pyramid structure.

    Theme  : Ancient power source
    Details: gold pyramid silhouette above Eye-of-Horus slot · energy bar ticks
    """
    EB = (8, 14, 14, 60)
    SL = (80, 35)
    IV = (8, 84); HB = (8, 142)

    img = Image.new("RGBA", (256, 256), TRANSPARENT)
    draw_panel(img)
    draw_inset(img, *EB)
    draw_energy_ticks(img, *EB)
    draw_slot(img, *SL)
    draw_pyramid(img, SL[0] + (18-13)//2, SL[1]-11, rows=7)
    draw_separator(img, IV[0], IV[1], 9*18)
    draw_inventory(img, *IV)
    draw_hotbar(img, *HB)

    uvs = pack_atlas([("energy", EB[2], EB[3])])
    sprite_energy(img, *uvs["energy"])

    return img, uvs, dict(
        ENERGY_X=EB[0], ENERGY_Y=EB[1], ENERGY_W=EB[2], ENERGY_H=EB[3],
        _energy_uv=uvs["energy"][:2],
        SLOT_X=SL[0], SLOT_Y=SL[1],
    )


def gen_ancient_battery():
    """
    Ancient Battery — large FE energy storage block.

    Details the user loved — KEPT:
             ▸ Glass highlight on left edge of fill sprite
             ▸ Tick marks at 25/50/75% inside bar walls
    New additions:
             ▸ Scan-lines inside dark chamber (CRT capacitor-bank readout)
             ▸ Gold UP + blue DOWN charge-direction arrows beside the two slots
    """
    EB = (66, 14, 44, 60)
    S0 = (26, 20)   # top slot    — battery charges item
    S1 = (26, 50)   # bottom slot — item discharges into battery
    IV = (8, 84); HB = (8, 142)

    img = Image.new("RGBA", (256, 256), TRANSPARENT)
    draw_panel(img)
    draw_inset(img, *EB)
    draw_energy_ticks(img, *EB)
    draw_battery_scanlines(img, *EB)
    draw_slot(img, *S0)
    draw_slot(img, *S1)
    draw_charge_arrows(img, S0[0]+20, S0[1]+1)
    draw_separator(img, IV[0], IV[1], 9*18)
    draw_inventory(img, *IV)
    draw_hotbar(img, *HB)

    uvs = pack_atlas([("energy", EB[2], EB[3])])
    sprite_energy(img, *uvs["energy"])

    return img, uvs, dict(
        ENERGY_X=EB[0], ENERGY_Y=EB[1], ENERGY_W=EB[2], ENERGY_H=EB[3],
        _energy_uv=uvs["energy"][:2],
        SLOT0_X=S0[0], SLOT0_Y=S0[1],
        SLOT1_X=S1[0], SLOT1_Y=S1[1],
    )


def gen_quantum_vacuum_turbine():
    """
    Quantum Vacuum Turbine — graviton-burning FE generator.

    Theme  : Quantum / engineering
    Details: circuit corner traces · flame above fuel · graviton orbital above burn bar
             · lightning bolt in gap · burn bar ticks at 33/66% · chamber scan-lines
    """
    EB = (50, 14, 76, 52)
    BB = (14, 42, 16, 24)
    FS = (14, 22)
    IV = (8, 84); HB = (8, 142)

    img = Image.new("RGBA", (256, 256), TRANSPARENT)
    draw_panel(img)
    draw_inset(img, *EB)
    draw_circuit_corners(img, *EB)
    draw_inset(img, *BB)
    draw_energy_ticks(img, *BB, fracs=(0.33, 0.66))
    draw_slot(img, *FS)
    draw_flame(img, FS[0]+5, FS[1]-11)
    draw_graviton_symbol(img, BB[0] + (16-7)//2, BB[1]-10)

    # Lightning bolt: centred horizontally in gap (burn_right=30, energy_left=50, gap=20)
    #                 centred vertically within the energy chamber height
    gap_left  = BB[0] + BB[2] + 1    # 31
    gap_right = EB[0] - 1            # 49
    bolt_x    = gap_left + (gap_right - gap_left - 6) // 2
    bolt_y    = EB[1] + (EB[3] - 11) // 2
    draw_lightning(img, bolt_x, bolt_y)

    draw_separator(img, IV[0], IV[1], 9*18)
    draw_inventory(img, *IV)
    draw_hotbar(img, *HB)

    uvs = pack_atlas([("energy", EB[2], EB[3]), ("burn", BB[2], BB[3])])
    sprite_energy_chamber(img, *uvs["energy"])
    sprite_burn(img, *uvs["burn"])

    return img, uvs, dict(
        ENERGY_X=EB[0], ENERGY_Y=EB[1], ENERGY_W=EB[2], ENERGY_H=EB[3],
        _energy_uv=uvs["energy"][:2],
        BURN_X=BB[0], BURN_Y=BB[1], BURN_W=BB[2], BURN_H=BB[3],
        _burn_uv=uvs["burn"][:2],
        FUEL_X=FS[0], FUEL_Y=FS[1],
    )


# ═════════════════════════════════════════════════════════════════════════════
#  MAIN
# ═════════════════════════════════════════════════════════════════════════════

MACHINES = [
    ("primal_catalyst_gui.png",        gen_primal_catalyst,       "Primal Catalyst"),
    ("decay_chamber_gui.png",          gen_decay_chamber,         "Decay Chamber"),
    ("pyramid_core_gui.png",           gen_pyramid_core,          "Pyramid Core"),
    ("ancient_battery_gui.png",        gen_ancient_battery,       "Ancient Battery"),
    ("quantum_vacuum_turbine_gui.png", gen_quantum_vacuum_turbine,"QVT Generator"),
]

def _bar(done, total, w=28):
    f = int(w * done / total)
    return "  [" + "█"*f + "░"*(w-f) + "]"

def run():
    print()
    print("  ╔══════════════════════════════════════════════════════╗")
    print("  ║   AlienTech GUI Texture Generator  v2.1             ║")
    print("  ╚══════════════════════════════════════════════════════╝")
    print(f"  Output → {OUT_DIR}")
    print()

    results = []
    failed  = []

    for idx, (filename, generator, label) in enumerate(MACHINES):
        print(f"{_bar(idx, len(MACHINES))}  {label}...", end="", flush=True)
        t0 = time.perf_counter()
        try:
            img, uvs, coords = generator()
        except Exception as exc:
            print(f"\n  ✗ ERROR — {filename}: {exc}")
            failed.append(filename); continue

        ok, reason = _atlas_ok(uvs)
        if not ok:
            print(f"\n  ✗ ATLAS ERROR — {filename}: {reason}")
            failed.append(filename); continue

        path = os.path.join(OUT_DIR, filename)
        img.save(path, "PNG")
        ms = (time.perf_counter() - t0) * 1000
        print(f"\r{_bar(idx+1, len(MACHINES))}  {label:<22s}  {ms:4.0f} ms  ✓")
        results.append((filename, uvs, coords))

    print()
    if failed:
        print(f"  ✗ {len(failed)} file(s) failed: {', '.join(failed)}")
        _pause_and_exit(1)

    # ── Java constants ──────────────────────────────────────────────────────
    print("  ══════════════════════════════════════════════════════════")
    print("  JAVA CONSTANTS — copy into your Screen / Menu classes")
    print("  ══════════════════════════════════════════════════════════")
    for filename, uvs, coords in results:
        label = filename.replace("_gui.png","").replace("_"," ").title()
        print(f"\n  // ── {label} ──")
        for k, v in coords.items():
            if k.startswith("_"):
                sprite = k[1:].replace("_uv","")
                ux, uy = v
                sw, sh = uvs[sprite][2], uvs[sprite][3]
                print(f"  // {sprite:12s} atlas UV=({ux},{uy})  {sw}×{sh}")
            else:
                print(f"  private static final int {k:<16s} = {v};")

    print()
    print("  ══════════════════════════════════════════════════════════")
    print(f"  {len(results)}/{len(MACHINES)} textures generated successfully.")
    print()
    print("  Copy the PNG files to:")
    print("  src/main/resources/assets/alientech/textures/gui/")
    print("  ══════════════════════════════════════════════════════════")
    print()
    _pause_and_exit(0)


if __name__ == "__main__":
    run()