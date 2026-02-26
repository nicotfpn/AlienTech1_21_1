#!/usr/bin/env python3
"""
AlienTech GUI Generator V9 — Perfected & Fully Commented
==========================================================

HOW MINECRAFT GUI TEXTURES WORK
---------------------------------
Every machine GUI texture is a single 256×256 RGBA PNG split into two zones:

  ZONE 1  (pixels 0,0 → 175,165)
    The static background that Java draws ONCE per frame with:
      blit(guiLeft, guiTop, 0, 0, 176, 166)
    Contains: the panel, all item slots, the EMPTY (dark) bar backgrounds,
    and the EMPTY arrow background.
    These pixels NEVER change — they are always fully drawn under everything else.

  ZONE 2  (pixels starting at u=176 or beyond the 176×166 GUI area)
    A sprite sheet of FILLED versions of bars and arrows.
    Java clips these per-tick to animate partial fill levels, e.g.:
      int fill = (int)(ratio * barHeight);
      blit(guiLeft + barX, guiTop + barY + (barHeight - fill),
           spriteU, barHeight - fill, barWidth, fill);
    Only the filled portion is blitted on top of the empty background.

This two-zone design means Zone 1 always shows the empty state, and Zone 2
sprites are layered on top to show the current fill — no Java-side pixel math needed.

LAYER ORDER PER FRAME (in Java renderBg):
  1. blit Zone 1 (full static background)
  2. blit entropy bar fill (clipped from Zone 2, if machine has_entropy_sidebar)
  3. blit FE bar fill      (clipped from Zone 2, if machine has_fe_sidebar)
  4. blit progress arrow   (clipped from Zone 2, if machine has a progress arrow)
  5. blit battery fill     (clipped from Zone 2, ancient_battery only)

UV COORDINATE CONTRACT (copy these into your Java Screen class):
  Entropy bar sprite : u=176, v=0,  w=8,  h=52   bottom-up fill
  FE bar sprite      : u=186, v=0,  w=8,  h=52   bottom-up fill
  Progress arrow     : u=176, v=54, w=24, h=16   left-to-right fill
  Battery body fill  : u=196, v=0,  w=28, h=36   bottom-up fill (ancient_battery only)

FIXES IN V9 (vs V8):
  1. Nub bottom border added — nub was floating into battery body with no pixel separation.
  2. MachineSpec split has_fe into has_fe_sidebar + has_fe — truthful flags, no more key hacks.
  3. Verifier FE check now uses has_fe_sidebar — no more hardcoded ancient_battery exception.
  4. Dead fe_x_for() helper removed — it was orphaned after the ancient_battery refactor.
  5. Dead MEK_* colour constants removed — leftover from deleted Mekanism-style widget.
  6. PROG_LEFT/PROG_RIGHT fixed to 4-tuples (RGBA) — was inconsistent 3-tuples.
  7. Dead draw.rectangle() outline call removed from _draw_empty_bar() — was overwritten immediately.
  8. Pyramid Core slot+pyramid combo is now centered together as a unit in the machine area.
  9. QVT slot+circle combo is now vertically centered as a unit in the machine area.
 10. OUT_DIR creation moved inside generate_all() — importing the module no longer creates folders.
"""

import os       # path manipulation and directory creation
import hashlib  # MD5 uniqueness check at the end

try:
    from PIL import Image, ImageDraw  # Pillow — the only dependency
except ImportError:
    raise SystemExit("Pillow required:  pip install Pillow")

from dataclasses import dataclass, field   # lightweight struct for MachineSpec
from typing import Callable                # type hint for the generator function field


# ===========================================================================
# SECTION 1 — CANVAS AND GUI GEOMETRY
# ===========================================================================
# Minecraft GUI textures must be power-of-two PNGs. 256×256 is the standard.
CANVAS_W = 256
CANVAS_H = 256

# The GUI panel always starts at the top-left corner of the texture.
# Java draws it with: blit(guiLeft, guiTop, 0, 0, GUI_W, GUI_H)
GUI_LEFT = 0
GUI_TOP  = 0
GUI_W    = 176   # standard Minecraft container width in pixels
GUI_H    = 166   # standard Minecraft container height in pixels
# Pixels from (GUI_W, 0) rightward to (255, 255) are Zone 2 (sprite sheet).
# Pixels from (0, GUI_H) downward are also free — currently unused.


# ===========================================================================
# SECTION 2 — COLOUR PALETTE
# ===========================================================================
# All colours are (R, G, B, A) tuples with A=255 (fully opaque) throughout.
# Transparent pixels outside the GUI area have A=0 from the blank canvas.
# RULE: every colour in this file is a 4-tuple — no 3-tuple shortcuts.

# --- Panel colours (vanilla Minecraft style) ---
PANEL_BG     = (0xC6, 0xC6, 0xC6, 255)  # main grey panel fill
BORDER_DARK  = (0x37, 0x37, 0x37, 255)  # dark bevel edge (top-left on slots, bottom-right on panel)
BORDER_LIGHT = (0xFF, 0xFF, 0xFF, 255)  # bright bevel edge (bottom-right on slots, top-left on panel)
SLOT_FACE    = (0x8B, 0x8B, 0x8B, 255)  # inner face of item slots (slightly darker than panel)
BAR_EMPTY_BG = (0x44, 0x44, 0x44, 255)  # fill of empty bar cavities in Zone 1

# --- Entropy bar gradient (red, Zone 2 sprite, fills bottom-up) ---
ENT_TOP_RGBA = (0xFF, 0x00, 0x00, 255)  # bright red at the top of a full entropy bar
ENT_BOT_RGBA = (0x8B, 0x00, 0x00, 255)  # dark red at the bottom of a full entropy bar

# --- FE (Forge Energy) bar gradient (gold, Zone 2 sprite, fills bottom-up) ---
FE_TOP_RGBA  = (0xFF, 0xD7, 0x00, 255)  # bright gold at the top of a full FE bar
FE_BOT_RGBA  = (0xFF, 0x8C, 0x00, 255)  # dark orange at the bottom of a full FE bar

# --- Progress arrow gradient (dark gold → bright gold, fills left-to-right) ---
# FIX #6: these are now 4-tuples like every other colour in this file.
PROG_LEFT  = (0x80, 0x60, 0x00, 255)   # dim gold at the left (start) of the arrow
PROG_RIGHT = (0xFF, 0xD7, 0x00, 255)   # bright gold at the right (end) of the arrow

# --- Battery body colours (ancient_battery specific) ---
BAT_TOP = (0xFF, 0x22, 0x22, 255)   # bright red at the top of the battery body
BAT_BOT = (0x66, 0x00, 0x00, 255)   # dark red at the bottom of the battery body
BAT_NUB = (0x99, 0x00, 0x00, 255)   # darker red for the positive nub cap (above body)
BAT_NEG = (0xBB, 0xBB, 0xBB, 255)   # grey for the "−" symbol at the battery bottom


# ===========================================================================
# SECTION 3 — SIDEBAR BAR GEOMETRY
# ===========================================================================
# Machines with entropy or FE show 8px-wide vertical bars on the left side
# of the panel, between the border and the machine work area.
# Their position is fixed — the same coordinates regardless of machine.

BAR_WIDTH  = 8    # width of each bar in pixels
BAR_HEIGHT = 52   # height of each bar in pixels (= max fill value Java uses)

# BAR_TOP: vertical start of both bars. GUI_TOP+17 clears the panel title area.
BAR_TOP = GUI_TOP + 17   # = 17

# ENT_X: left edge of entropy bar. 1px panel border + 7px pad from GUI_LEFT.
ENT_X = GUI_LEFT + 8     # = 8

# FE_X: left edge of FE bar, right of the entropy bar with a 3px gap.
FE_X = ENT_X + BAR_WIDTH + 3   # = 8+8+3 = 19

# BAR_RIGHT_BORDER: first free X pixel after both bars — start of machine area.
BAR_RIGHT_BORDER = FE_X + BAR_WIDTH + 1   # = 19+8+1 = 28


# ===========================================================================
# SECTION 4 — ZONE 2 SPRITE SHEET UV COORDINATES
# ===========================================================================
# Pixel coords inside the PNG where filled animation sprites live.
# Java uses these as (srcX, srcY) in blit() calls.
# All placed at x >= 176 so they never overlap Zone 1.

UV_ENT_U,  UV_ENT_V  = 176, 0    # entropy sprite: 8×52, red gradient
UV_FE_U,   UV_FE_V   = 186, 0    # FE sprite: 8×52, gold gradient (right of entropy, no gap)
UV_PROG_U, UV_PROG_V = 176, 54   # progress arrow sprite: 24×16, gold gradient (below bars)
UV_BAT_U,  UV_BAT_V  = 196, 0    # battery fill sprite: 28×36, red gradient (ancient_battery only)
UV_BAT_W,  UV_BAT_H  = 28, 36    # battery sprite dimensions — also used in verifier and generator


# ===========================================================================
# SECTION 5 — MACHINE WORK AREA AND IO SLOT LAYOUT
# ===========================================================================
# The machine area is the zone between the sidebar bars and the right panel edge,
# and between the top and the inventory divider. All machine-specific slots,
# arrows and decorations must fit entirely inside it.

SLOT_W = 18   # item slot width  (16px inner face + 1px border on each side)
SLOT_H = 18   # item slot height

# Machine area pixel boundaries:
MA_LEFT  = BAR_RIGHT_BORDER   # = 28   — first X after both bars
MA_RIGHT = 172                # = 172  — 4px margin from the inner right panel edge
MA_TOP   = BAR_TOP            # = 17   — same top as bars
MA_BOT   = 83                 # = 83   — where the inventory divider line starts
MA_W     = MA_RIGHT - MA_LEFT # = 144  — usable horizontal pixels
MA_H     = MA_BOT   - MA_TOP  # = 66   — usable vertical pixels

# --- 3-slot column layout (Primal Catalyst) ---
# Arrangement: [col of 3 inputs] → [24px arrow] → [1 output]
# Total width of this layout unit:
IO3_W     = SLOT_W + 4 + 24 + 4 + SLOT_W   # = 18+4+24+4+18 = 68px
# Center the 68px unit horizontally in MA_W=144:
IO3_COL_X = MA_LEFT + (MA_W - IO3_W) // 2  # = 28+(144-68)//2 = 28+38 = 66
# Stack 3 input slots vertically, centered in MA_H=66:
IO3_COL_Y0 = MA_TOP + (MA_H - 3*SLOT_H) // 2   # top slot    = 17+(66-54)//2 = 23
IO3_COL_Y1 = IO3_COL_Y0 + SLOT_H                # middle slot = 23+18 = 41
IO3_COL_Y2 = IO3_COL_Y0 + 2 * SLOT_H            # bottom slot = 23+36 = 59
# Progress arrow: 4px right of input column, vertically centered across all 3 slots:
IO3_PROG_X = IO3_COL_X + SLOT_W + 4             # = 66+18+4 = 88
IO3_PROG_Y = IO3_COL_Y0 + (3*SLOT_H - 16) // 2  # = 23+(54-16)//2 = 42
# Output slot: 4px right of arrow, at middle-slot height:
IO3_OUT_X  = IO3_PROG_X + 24 + 4                # = 88+24+4 = 116
IO3_OUT_Y  = IO3_COL_Y1                          # = 41

# --- 1-slot single row layout (Decay Chamber) ---
# Arrangement: [1 input] → [24px arrow] → [1 output], all on a single row
IO1_W     = SLOT_W + 4 + 24 + 4 + SLOT_W   # = 68px (same unit width as 3-slot)
# Center the unit horizontally and vertically:
IO1_IN_X  = MA_LEFT + (MA_W - IO1_W) // 2  # = 28+(144-68)//2 = 66
IO1_IN_Y  = MA_TOP  + (MA_H - SLOT_H) // 2 # = 17+(66-18)//2 = 41
# Arrow: 4px right of input, vertically centered inside the single slot height:
IO1_PROG_X = IO1_IN_X + SLOT_W + 4          # = 66+18+4 = 88
IO1_PROG_Y = IO1_IN_Y + (SLOT_H - 16) // 2  # = 41+(18-16)//2 = 42
# Output: 4px right of arrow, same row:
IO1_OUT_X  = IO1_PROG_X + 24 + 4            # = 88+24+4 = 116
IO1_OUT_Y  = IO1_IN_Y                        # = 41


# ===========================================================================
# SECTION 6 — PLAYER INVENTORY LAYOUT
# ===========================================================================
# The bottom portion of every GUI (below y=83) is the standard player inventory.
# 3×9 main inventory rows, then a hotbar row with a natural gap above it.

INV_START_X = GUI_LEFT + 8    # = 8   — left edge of the first slot
INV_START_Y = GUI_TOP  + 84   # = 84  — one pixel below the machine area divider
HOTBAR_Y    = GUI_TOP  + 142  # = 142 — hotbar sits 4px below the last inventory row


# ===========================================================================
# SECTION 7 — PRIMITIVE DRAW HELPERS
# ===========================================================================
# Thin wrappers around Pillow calls. Pillow uses inclusive end-pixel coordinates
# in rectangle and line commands, which is error-prone. These helpers accept
# (x, y, width, height) instead, making every call self-documenting.

def _box(draw, x, y, w, h, color):
    """Fill a solid rectangle. (x,y) = top-left corner; w,h = dimensions in pixels."""
    # Pillow [x0,y0,x1,y1]: x1/y1 are inclusive, so subtract 1 from each.
    draw.rectangle([x, y, x+w-1, y+h-1], fill=color)

def _hline(draw, x, y, length, color):
    """Draw a 1px-tall horizontal line starting at (x,y) with given pixel length."""
    draw.line([x, y, x+length-1, y], fill=color)

def _vline(draw, x, y, length, color):
    """Draw a 1px-wide vertical line starting at (x,y) with given pixel length."""
    draw.line([x, y, x, y+length-1], fill=color)

def _v_grad(draw, x, y, w, h, top, bot):
    """
    Fill a rectangle with a vertical linear gradient from colour 'top' to 'bot'.
    Draws one horizontal scanline per pixel row.
    t=0.0 at the top row, t=1.0 at the bottom row.
    Accepts 3- or 4-tuple colours — alpha is always written as 255.
    """
    for i in range(h):
        t = i / max(h-1, 1)                       # normalise row position; guard div-by-zero
        r = int(top[0] + (bot[0] - top[0]) * t)   # interpolate red channel
        g = int(top[1] + (bot[1] - top[1]) * t)   # interpolate green channel
        b = int(top[2] + (bot[2] - top[2]) * t)   # interpolate blue channel
        draw.line([x, y+i, x+w-1, y+i], fill=(r, g, b, 255))

def _h_grad(draw, x, y, w, h, left, right):
    """
    Fill a rectangle with a horizontal linear gradient from 'left' to 'right'.
    Draws one vertical column per pixel column.
    t=0.0 at the leftmost column, t=1.0 at the rightmost.
    Used for the progress arrow sprite which fills left-to-right.
    Accepts 3- or 4-tuple colours — alpha is always written as 255.
    """
    for i in range(w):
        t = i / max(w-1, 1)                          # normalise column position
        r = int(left[0] + (right[0] - left[0]) * t)  # interpolate red
        g = int(left[1] + (right[1] - left[1]) * t)  # interpolate green
        b = int(left[2] + (right[2] - left[2]) * t)  # interpolate blue
        draw.line([x+i, y, x+i, y+h-1], fill=(r, g, b, 255))


# ===========================================================================
# SECTION 8 — PANEL (ZONE 1: drawn once, identical for all machines)
# ===========================================================================
def draw_panel(im):
    """
    Draws the main grey panel background with a vanilla-style double bevel border.

    Vanilla Minecraft uses a two-layer bevel:
      Outer layer: LIGHT top/left, DARK bottom/right  → the whole panel looks raised
      Inner layer: DARK top/left, LIGHT bottom/right  → a sunken inset just inside the raise

    Drawing order: fill → outer bevel → inner bevel.
    Later lines overwrite earlier ones at corners — this is intentional and matches vanilla.
    """
    draw = ImageDraw.Draw(im)

    # Base fill: the entire panel area in flat grey.
    _box(draw, GUI_LEFT, GUI_TOP, GUI_W, GUI_H, PANEL_BG)

    # Outer bevel — LIGHT edges: top and left make the panel appear raised.
    _hline(draw, GUI_LEFT, GUI_TOP,         GUI_W, BORDER_LIGHT)  # outer top
    _vline(draw, GUI_LEFT, GUI_TOP,         GUI_H, BORDER_LIGHT)  # outer left

    # Outer bevel — DARK edges: bottom and right complete the raised illusion.
    _hline(draw, GUI_LEFT, GUI_TOP+GUI_H-1, GUI_W, BORDER_DARK)   # outer bottom
    _vline(draw, GUI_LEFT+GUI_W-1, GUI_TOP, GUI_H, BORDER_DARK)   # outer right

    # Inner bevel — DARK edges (1px inset): creates the sunken-inset look.
    _hline(draw, GUI_LEFT+1, GUI_TOP+1,         GUI_W-2, BORDER_DARK)   # inner top
    _vline(draw, GUI_LEFT+1, GUI_TOP+1,         GUI_H-2, BORDER_DARK)   # inner left

    # Inner bevel — LIGHT edges (1px inset from outer): closes the inner bevel.
    _hline(draw, GUI_LEFT+1, GUI_TOP+GUI_H-2,   GUI_W-2, BORDER_LIGHT)  # inner bottom
    _vline(draw, GUI_LEFT+GUI_W-2, GUI_TOP+1,   GUI_H-2, BORDER_LIGHT)  # inner right


# ===========================================================================
# SECTION 9 — ITEM SLOT (ZONE 1: reused everywhere)
# ===========================================================================
def draw_slot(im, x, y):
    """
    Draws a single 18×18 item slot at (x, y) with a vanilla sunken-bevel appearance.

    Slot appearance: dark top/left borders and light bottom/right borders,
    creating a "pressed inward" look. This is the opposite of the panel bevel.

    Drawing order: fill first, then vertical borders, then horizontal borders.
    Horizontal lines drawn last so the corner pixel inherits the h-line colour,
    which matches vanilla's exact corner rendering.
    """
    draw = ImageDraw.Draw(im)

    # Fill the entire slot with the slot-face grey.
    _box(draw, x, y, SLOT_W, SLOT_H, SLOT_FACE)

    # Sunken bevel: DARK on left and top edges.
    _vline(draw, x,          y, SLOT_H, BORDER_DARK)    # left border
    _hline(draw, x, y,          SLOT_W, BORDER_DARK)    # top border (overwrites top-left corner)

    # Sunken bevel: LIGHT on right and bottom edges.
    _vline(draw, x+SLOT_W-1, y, SLOT_H, BORDER_LIGHT)   # right border
    _hline(draw, x, y+SLOT_H-1, SLOT_W, BORDER_LIGHT)   # bottom border


# ===========================================================================
# SECTION 10 — PLAYER INVENTORY (ZONE 1: identical for all machines)
# ===========================================================================
def draw_inventory(im):
    """
    Draws the standard 3×9 player inventory grid and 1×9 hotbar below it.
    The gap between the inventory rows and the hotbar comes naturally from
    HOTBAR_Y being 4px further down than INV_START_Y + 3*SLOT_H = 138.
    """
    # Three inventory rows:
    for row in range(3):
        for col in range(9):
            draw_slot(im,
                INV_START_X + col * SLOT_W,  # step right by SLOT_W per column
                INV_START_Y + row * SLOT_H   # step down by SLOT_H per row
            )

    # One hotbar row:
    for col in range(9):
        draw_slot(im,
            INV_START_X + col * SLOT_W,  # same X spacing as inventory
            HOTBAR_Y                      # fixed Y, slightly below inventory rows
        )


# ===========================================================================
# SECTION 11 — ZONE 1: EMPTY BAR BACKGROUNDS
# ===========================================================================
def _draw_empty_bar(im, x, y):
    """
    Draws an empty sidebar bar cavity at (x, y) — the zero-fill state.
    This is what the player sees when the bar is empty (entropy=0 or FE=0).
    Java blits the filled Zone 2 sprite on top per tick to show the actual level.

    The cavity interior is BAR_WIDTH×BAR_HEIGHT = 8×52 pixels.
    The visible sunken frame adds 1px outside the fill on all four sides,
    making the total visual size (BAR_WIDTH+2)×(BAR_HEIGHT+2) = 10×54.

    FIX #7: the dead draw.rectangle() outline call has been removed.
    Previously it drew BORDER_DARK one pixel outside, then the _hline/_vline
    calls immediately overwrote those same pixels. Only the inner bevel lines
    are needed — they produce the correct sunken-slot appearance on their own.
    """
    assert x >= 1, f"bar at x={x}: no room for the 1px left border"
    draw = ImageDraw.Draw(im)

    # Fill the bar interior with dark empty-state colour.
    _box(draw, x, y, BAR_WIDTH, BAR_HEIGHT, BAR_EMPTY_BG)

    # Inner bevel: DARK on top and left edges (sunken appearance, same as slots).
    _hline(draw, x, y,              BAR_WIDTH,  BORDER_DARK)    # top edge of cavity
    _vline(draw, x, y,              BAR_HEIGHT, BORDER_DARK)    # left edge of cavity

    # Inner bevel: LIGHT on bottom and right edges.
    _hline(draw, x, y+BAR_HEIGHT-1, BAR_WIDTH,  BORDER_LIGHT)   # bottom edge
    _vline(draw, x+BAR_WIDTH-1, y,  BAR_HEIGHT, BORDER_LIGHT)   # right edge

    # Outer 1px dark frame: drawn AFTER the inner bevel so it forms the outer border.
    # Top outer pixel row (above the fill):
    _hline(draw, x-1, y-1, BAR_WIDTH+2, BORDER_DARK)   # outer top
    # Bottom outer pixel row:
    _hline(draw, x-1, y+BAR_HEIGHT, BAR_WIDTH+2, BORDER_DARK)   # outer bottom
    # Left and right outer columns:
    _vline(draw, x-1,          y-1, BAR_HEIGHT+2, BORDER_DARK)  # outer left
    _vline(draw, x+BAR_WIDTH,  y-1, BAR_HEIGHT+2, BORDER_DARK)  # outer right

def draw_entropy_bar_bg(im, x=ENT_X, y=BAR_TOP):
    """
    Draws the empty entropy bar at the default entropy position (left sidebar).
    x defaults to ENT_X=8, y defaults to BAR_TOP=17.
    """
    _draw_empty_bar(im, x, y)

def draw_fe_bar_bg(im, x=FE_X, y=BAR_TOP):
    """
    Draws the empty FE bar at the default FE position (second from left).
    x defaults to FE_X=19, y defaults to BAR_TOP=17.
    """
    _draw_empty_bar(im, x, y)


# ===========================================================================
# SECTION 12 — ZONE 1: EMPTY PROGRESS ARROW BACKGROUND
# ===========================================================================
def _draw_empty_progress(im, x, y, w, h):
    """
    Draws a dark rectangular progress arrow background at (x, y) with size w×h.
    This is the empty/zero state — Java blits the filled Zone 2 sprite on top.

    The arrow tip is a small pixel staircase jutting 3px right of the rectangle,
    forming a right-pointing arrow head shape at the vertical centre.

    Staircase pattern (relative to x+w, y+h//2):
      Centre row (dy=0):  pixels at dx=0,1,2  → 3px wide tip
      Adjacent rows ±1:   pixels at dx=0,1    → 2px wide shoulders
    """
    draw = ImageDraw.Draw(im)

    # Fill arrow body.
    _box(draw, x, y, w, h, BAR_EMPTY_BG)

    # Dark border around the arrow body rectangle.
    draw.rectangle([x, y, x+w-1, y+h-1], outline=BORDER_DARK)

    # Arrow tip staircase pixels to the right of the body.
    mid = y + h // 2
    for dy, dx_max in [(0, 2), (1, 1), (-1, 1)]:
        for dx in range(dx_max + 1):
            draw.point((x+w+dx, mid+dy), fill=BORDER_DARK)


# ===========================================================================
# SECTION 13 — ZONE 2: FILLED SPRITE SHEET (shared across all machine PNGs)
# ===========================================================================
def draw_sprite_sheet(im):
    """
    Populates Zone 2 with all filled animation sprites.
    Called for EVERY machine PNG — the UV coords are identical across all machines
    so Java needs only one constant set.

    Sprites:
      1. Entropy bar fill  — u=176, v=0,  8×52,  red gradient,  bottom-up
      2. FE bar fill       — u=186, v=0,  8×52,  gold gradient, bottom-up
      3. Progress arrow    — u=176, v=54, 24×16, gold gradient, left-to-right
      4. Battery body fill — u=196, v=0,  28×36, red gradient,  bottom-up

    Java usage example (entropy bar at ratio r):
      int fill  = (int)(r * BAR_HEIGHT);            // pixels to show
      int srcV  = BAR_HEIGHT - fill;                // crop from the top
      blit(guiLeft+ENT_X, guiTop+BAR_TOP+srcV,     // screen destination
           UV_ENT_U, srcV, BAR_WIDTH, fill);        // texture source + size
    """
    draw = ImageDraw.Draw(im)

    # Sprite 1 — Entropy bar (8×52 red gradient at x=176).
    # Bright red at top → dark red at bottom.
    # Java fills bottom-up so low entropy shows only the dark bottom portion.
    _v_grad(draw,
            UV_ENT_U, UV_ENT_V,          # sprite top-left in the PNG
            BAR_WIDTH, BAR_HEIGHT,        # 8×52
            ENT_TOP_RGBA, ENT_BOT_RGBA)  # row 0 colour → row 51 colour

    # Sprite 2 — FE bar (8×52 gold gradient at x=186).
    # Bright gold at top → dark orange at bottom. Same bottom-up fill as entropy.
    _v_grad(draw,
            UV_FE_U, UV_FE_V,            # immediately right of entropy sprite
            BAR_WIDTH, BAR_HEIGHT,
            FE_TOP_RGBA, FE_BOT_RGBA)

    # Sprite 3 — Progress arrow (24×16 gold gradient at x=176, y=54).
    # Dark gold left → bright gold right. Java fills left-to-right.
    # y=54 clears the bar sprites which end at y=52 (safe 2px gap).
    _h_grad(draw,
            UV_PROG_U, UV_PROG_V,  # below the bar sprites
            24, 16,                 # 24 wide, 16 tall
            PROG_LEFT, PROG_RIGHT)  # dim gold → bright gold

    # Arrow tip pixels for sprite 3: same staircase as the empty background tip.
    # These live at x=200..202 — within the 256px canvas.
    # Only visible in-game when progress is 100% (full 24px blitted).
    mid = UV_PROG_V + 8   # vertical centre of the 16px arrow = y=62
    for dy, dx_max in [(0, 2), (1, 1), (-1, 1)]:
        for dx in range(dx_max + 1):
            # Slightly darker than PROG_RIGHT (80%) for the tip shadow.
            tip_color = tuple(int(c * 0.8) for c in PROG_RIGHT[:3]) + (255,)
            draw.point((UV_PROG_U + 24 + dx, mid + dy), fill=tip_color)

    # Sprite 4 — Battery body fill (28×36 red gradient at x=196).
    # Identical gradient to the visible battery body in Zone 1.
    # No border, nub, or labels — just the raw fill so Java can clip and blit cleanly.
    #
    # Java usage (ancient_battery at charge ratio r):
    #   int fill = (int)(r * UV_BAT_H);                      // e.g. 22px filled
    #   int srcV = UV_BAT_H - fill;                           // e.g. 14 (start partway down)
    #   blit(guiLeft+86, guiTop+21+srcV, 196, srcV, 28, fill);
    _v_grad(draw,
            UV_BAT_U, UV_BAT_V,   # x=196, y=0
            UV_BAT_W, UV_BAT_H,   # 28×36
            BAT_TOP, BAT_BOT)     # bright red → dark red (must match Zone 1 body exactly)


# ===========================================================================
# SECTION 14 — MACHINE GENERATORS (ZONE 1: machine-specific content)
# ===========================================================================
# Each generator draws only what is unique to its machine.
# The panel, inventory, and sprite sheet are already drawn before any generator runs.

def generate_primal_catalyst(im):
    """
    Primal Catalyst — entropy sidebar + FE sidebar.
    Layout: 3 stacked input slots → 24px progress arrow → 1 output slot.
    The whole 68px-wide IO unit is centered in the machine area.
    """
    draw_entropy_bar_bg(im)   # entropy at ENT_X=8
    draw_fe_bar_bg(im)        # FE at FE_X=19

    # Three input slots stacked vertically on the left of the IO unit.
    draw_slot(im, IO3_COL_X, IO3_COL_Y0)  # top    (x=66, y=23)
    draw_slot(im, IO3_COL_X, IO3_COL_Y1)  # middle (x=66, y=41)
    draw_slot(im, IO3_COL_X, IO3_COL_Y2)  # bottom (x=66, y=59)

    # Progress arrow between input column and output slot.
    _draw_empty_progress(im, IO3_PROG_X, IO3_PROG_Y, 24, 16)   # x=88, y=42

    # Single output slot, aligned with the middle input row.
    draw_slot(im, IO3_OUT_X, IO3_OUT_Y)   # x=116, y=41


def generate_decay_chamber(im):
    """
    Decay Chamber — entropy sidebar + FE sidebar.
    Layout: 1 input slot → 24px progress arrow → 1 output slot.
    Single centered row.
    """
    draw_entropy_bar_bg(im)   # entropy at ENT_X=8
    draw_fe_bar_bg(im)        # FE at FE_X=19

    draw_slot(im, IO1_IN_X, IO1_IN_Y)                           # input  x=66, y=41
    _draw_empty_progress(im, IO1_PROG_X, IO1_PROG_Y, 24, 16)   # arrow  x=88, y=42
    draw_slot(im, IO1_OUT_X, IO1_OUT_Y)                         # output x=116, y=41


def generate_pyramid_core(im):
    """
    Pyramid Core — entropy sidebar only. No FE. No progress arrow. No output slot.
    Layout: 1 standalone input slot + 4-tier pixel-art pyramid decoration.

    FIX #8: the slot and pyramid are now treated as a single visual unit,
    horizontally centered together in the machine area instead of the slot
    being centered alone and the pyramid floating to its right.

    The pyramid is a stepped shape with 4 tiers. Each tier is 4px tall, 4px
    wider than the tier above it, and 2px wider on each side (centered).
    Colours graduate from light sandy gold (peak) to dark earthy brown (base).
    """
    draw_entropy_bar_bg(im)  # entropy bar at default position

    draw = ImageDraw.Draw(im)

    # --- Measure the combined slot+gap+pyramid unit ---
    # Pyramid: base is 17px wide. Total unit = slot + 8px gap + 17px base = 43px.
    PYR_BASE_W = 17   # width of the widest pyramid tier
    UNIT_GAP   = 8    # pixel gap between slot right edge and pyramid base left edge
    UNIT_W     = SLOT_W + UNIT_GAP + PYR_BASE_W   # = 18+8+17 = 43px

    # Center the entire unit horizontally in the machine area:
    unit_x = MA_LEFT + (MA_W - UNIT_W) // 2   # = 28+(144-43)//2 = 28+50 = 78

    # Center the unit vertically (slot height used as reference):
    unit_y = MA_TOP + (MA_H - SLOT_H) // 2    # = 17+(66-18)//2 = 17+24 = 41

    # Draw the input slot at the left of the unit.
    draw_slot(im, unit_x, unit_y)              # x=78, y=41

    # Pyramid anchor: left edge of pyramid base, right of the gap.
    pyr_left = unit_x + SLOT_W + UNIT_GAP     # = 78+18+8 = 104

    # Pyramid vertical anchor: base bottom aligns with slot bottom for visual grounding.
    # The 4 tiers are each 4px tall with 2px gaps between tiers.
    # Total pyramid height = 4*4 + 3*2 = 22px. Place base bottom at slot bottom.
    pyr_bot  = unit_y + SLOT_H - 1            # = 41+17 = 58

    # Draw 4 tiers bottom-to-top. Each tier: width shrinks by 4px, shifts right by 2px.
    # Tier colours get lighter moving up (base=dark brown, peak=sandy gold).
    tiers = [
        (0, 17, (0x50, 0x3C, 0x14, 255)),   # tier 0: base,       17px wide, darkest
        (6, 13, (0x70, 0x4C, 0x20, 255)),   # tier 1: lower body, 13px wide
        (12, 9, (0x90, 0x5C, 0x28, 255)),   # tier 2: upper body,  9px wide
        (18, 5, (0xB0, 0x7C, 0x38, 255)),   # tier 3: peak,        5px wide, lightest
    ]
    for offset_from_base, tier_w, color in tiers:
        # offset_from_base: how many px above the base bottom this tier sits.
        # tier is centered on pyr_left + PYR_BASE_W//2:
        tier_x = pyr_left + (PYR_BASE_W - tier_w) // 2
        tier_y = pyr_bot - offset_from_base - 4 + 1   # tier top = base_bot - offset - height + 1
        _box(draw, tier_x, tier_y, tier_w, 4, color)


def generate_ancient_battery(im):
    """
    Ancient Battery — uses FE energy (has_fe=True) but draws NO sidebar bars.
    Instead: a pixel-art battery (pilha) centered in the full machine area,
    plus two item slots (charge/discharge) below it.

    Battery structure:
      - Positive nub (cap): narrow dark-red rectangle above the body
      - Body:               tall rectangle with red gradient fill + dark border
      - "+" symbol:         pixel-art cross in white, near the body top
      - "−" symbol:         pixel-art dash in grey, near the body bottom
      - Two item slots:     centered below the body

    FIX #1: the nub now has a BORDER_DARK line on its bottom edge, visually
    separating it from the body. Previously the nub floated into the body
    with no pixel boundary between them.

    The Zone 2 battery fill sprite at UV_BAT_U=196 uses the same BAT_TOP→BAT_BOT
    gradient — Java blits it clipped bottom-up on top of the body outline.
    """
    draw = ImageDraw.Draw(im)

    # Battery body dimensions (UV_BAT_W×UV_BAT_H must match the Zone 2 sprite):
    BAT_W = UV_BAT_W   # = 28px
    BAT_H = UV_BAT_H   # = 36px

    # Center battery body horizontally in the full machine area:
    bat_x = (MA_LEFT + MA_RIGHT) // 2 - BAT_W // 2   # = 100-14 = 86
    # Position body near the top, leaving room for slots below:
    bat_y = MA_TOP + 4   # = 17+4 = 21

    # --- Positive nub (top cap) ---
    nub_w, nub_h = 10, 4
    nub_x = bat_x + (BAT_W - nub_w) // 2   # center nub on body: 86+9 = 95
    nub_y = bat_y - nub_h                   # nub sits directly above body: 21-4 = 17

    # Fill nub with darker red than the body top.
    _box(draw, nub_x, nub_y, nub_w, nub_h, BAT_NUB)

    # Nub border: top, left, right edges of the nub (dark outline on 3 exposed sides).
    _hline(draw, nub_x, nub_y,               nub_w, BORDER_DARK)   # nub top
    _vline(draw, nub_x, nub_y,               nub_h, BORDER_DARK)   # nub left
    _vline(draw, nub_x + nub_w - 1, nub_y,   nub_h, BORDER_DARK)   # nub right
    # FIX #1: nub bottom border — separates the nub from the body visually.
    # Previously missing, causing the nub to bleed into the body with no boundary.
    _hline(draw, nub_x, nub_y + nub_h - 1,   nub_w, BORDER_DARK)   # nub bottom ← NEW

    # --- Battery body ---
    # Dark border 1px outside the body on all 4 sides.
    draw.rectangle([bat_x-1, bat_y-1, bat_x+BAT_W, bat_y+BAT_H], outline=BORDER_DARK)

    # Red gradient fill: bright red (top) → dark red (bottom).
    # MUST match Zone 2 sprite gradient exactly — they are the same element at runtime.
    _v_grad(draw, bat_x, bat_y, BAT_W, BAT_H, BAT_TOP, BAT_BOT)

    # --- "+" symbol (pixel-art 5×5 cross, BORDER_LIGHT on red → clearly visible) ---
    mid_x = bat_x + BAT_W // 2   # horizontal centre of body = 86+14 = 100
    _hline(draw, mid_x - 2, bat_y + 6, 5, BORDER_LIGHT)   # horizontal bar
    _vline(draw, mid_x,     bat_y + 4, 5, BORDER_LIGHT)   # vertical bar

    # --- "−" symbol (horizontal dash, grey so it's less prominent than "+") ---
    _hline(draw, mid_x - 2, bat_y + BAT_H - 8, 5, BAT_NEG)

    # --- Two item slots centered below the battery body ---
    slot_gap = 6                                           # px gap between left and right slot
    slots_w  = SLOT_W * 2 + slot_gap                      # = 18+6+18 = 42px
    slot_x0  = (MA_LEFT + MA_RIGHT) // 2 - slots_w // 2   # left slot X: 100-21 = 79
    slot_x1  = slot_x0 + SLOT_W + slot_gap                # right slot X: 79+18+6 = 103
    slot_y   = bat_y + BAT_H + 6                          # Y: 21+36+6 = 63 (≤ MA_BOT=83 ✓)
    draw_slot(im, slot_x0, slot_y)   # left slot  — charge input
    draw_slot(im, slot_x1, slot_y)   # right slot — discharge output


def generate_quantum_vacuum_turbine(im):
    """
    Quantum Vacuum Turbine — entropy sidebar (left) + FE sidebar (right).
    Layout: 1 centered input slot, turbine circle indicator below it.

    FIX #9: the slot+circle combo is now vertically centered as a unit in
    the machine area. Previously the slot was pinned near the top (MA_TOP+4)
    and left ~19px of dead space below the circle.

    Turbine circle layers (drawn outward-to-inward):
      outer ring  r=11: panel-colour fill + dark outline → housing
      inner ring  r=6:  light-blue fill + dark outline   → rotor disc
      cross spokes:     two dark lines (H+V) through centre → blade shafts
      centre hub  r=3:  solid dark fill → axle pivot point
    """
    draw_entropy_bar_bg(im, x=ENT_X)   # entropy bar at x=8
    draw_fe_bar_bg(im, x=FE_X)         # FE bar at x=19

    r_outer = 11   # outer housing ring radius
    r_inner = 6    # rotor disc radius
    r_hub   = 3    # axle hub radius
    CIRCLE_DIAM = r_outer * 2 + 1   # = 23px (bounding box of outer ring)
    SLOT_TO_CIRCLE_GAP = 6          # px gap between slot bottom and circle top

    # Total height of the slot+gap+circle unit:
    UNIT_H = SLOT_H + SLOT_TO_CIRCLE_GAP + CIRCLE_DIAM   # = 18+6+23 = 47px

    # Center the slot horizontally in the machine area:
    slot_cx = (MA_LEFT + MA_RIGHT) // 2   # = (28+172)//2 = 100
    slot_x  = slot_cx - SLOT_W // 2       # = 100-9 = 91

    # FIX #9: center the entire unit vertically in the machine area:
    unit_top = MA_TOP + (MA_H - UNIT_H) // 2   # = 17+(66-47)//2 = 17+9 = 26
    slot_y   = unit_top                          # slot starts at top of unit

    draw_slot(im, slot_x, slot_y)

    draw = ImageDraw.Draw(im)

    # Turbine circle centre: below the slot by the gap + half circle diameter.
    cx = slot_cx                                              # = 100
    cy = slot_y + SLOT_H + SLOT_TO_CIRCLE_GAP + r_outer      # = 26+18+6+11 = 61

    # Outer ring: panel-coloured fill, dark outline — the circular housing frame.
    draw.ellipse([cx-r_outer, cy-r_outer, cx+r_outer, cy+r_outer],
                 outline=BORDER_DARK, fill=PANEL_BG)

    # Inner ring: light-blue fill, dark outline — the spinning rotor.
    draw.ellipse([cx-r_inner, cy-r_inner, cx+r_inner, cy+r_inner],
                 outline=BORDER_DARK, fill=(0xAA, 0xCC, 0xFF, 255))

    # Cross spokes: H and V dark lines across the full outer ring diameter.
    _hline(draw, cx - r_outer, cy, r_outer * 2 + 1, BORDER_DARK)   # horizontal spoke
    _vline(draw, cx, cy - r_outer, r_outer * 2 + 1, BORDER_DARK)   # vertical spoke

    # Centre hub: solid dark circle covering the spoke intersection — the axle.
    draw.ellipse([cx-r_hub, cy-r_hub, cx+r_hub, cy+r_hub], fill=BORDER_DARK)


def generate_ancient_charger(im):
    """
    Ancient Charger — entropy sidebar + FE sidebar.
    Layout: 1 centered input slot. No arrow, no output.
    """
    draw_entropy_bar_bg(im)   # entropy at ENT_X=8
    draw_fe_bar_bg(im)        # FE at FE_X=19
    
    # Calculate center of machine area
    cx = (MA_LEFT + MA_RIGHT) // 2
    cy = MA_TOP + (MA_H) // 2
    
    # Draw single slot centered
    draw_slot(im, cx - SLOT_W // 2, cy - SLOT_H // 2)


# ===========================================================================
# SECTION 15 — MACHINE SPEC REGISTRY
# ===========================================================================
@dataclass
class MachineSpec:
    key:               str
    filename:          str
    has_entropy:       bool   # does this machine consume/generate entropy energy?
    has_entropy_sidebar: bool  # does it show an entropy bar on the left sidebar?
    has_fe:            bool   # does this machine use Forge Energy?
    has_fe_sidebar:    bool   # does it show a FE bar on the sidebar?
    generator:         Callable = None  # assigned from _GENERATORS below

MACHINE_SPECS: list = [
    #                    key                       filename                         ent    ent_bar  fe     fe_bar
    MachineSpec("primal_catalyst",        "primal_catalyst_gui.png",        True,  True,  True,  True ),
    MachineSpec("decay_chamber",          "decay_chamber_gui.png",           True,  True,  True,  True ),
    MachineSpec("pyramid_core",           "pyramid_core_gui.png",            True,  True,  False, False),
    MachineSpec("ancient_battery",        "ancient_battery_gui.png",         False, False, True,  False),  # FE but no sidebar bar
    MachineSpec("quantum_vacuum_turbine", "quantum_vacuum_turbine_gui.png",  True,  True,  True,  True ),
    MachineSpec("ancient_charger",        "ancient_charger_gui.png",         True,  True,  True,  True ),
]

# Attach generator functions after both the list and functions are defined.
_GENERATORS = {
    "primal_catalyst":        generate_primal_catalyst,
    "decay_chamber":          generate_decay_chamber,
    "pyramid_core":           generate_pyramid_core,
    "ancient_battery":        generate_ancient_battery,
    "quantum_vacuum_turbine": generate_quantum_vacuum_turbine,
    "ancient_charger":        generate_ancient_charger,
}
for _spec in MACHINE_SPECS:
    _spec.generator = _GENERATORS[_spec.key]


# ===========================================================================
# SECTION 16 — PIXEL-LEVEL VERIFIER
# ===========================================================================
def verify_image(path: str, spec: MachineSpec):
    im   = Image.open(path).convert("RGBA")
    px   = im.load()
    name = os.path.basename(path)

    # 1. Canvas size.
    assert im.size == (CANVAS_W, CANVAS_H), \
        f"{name}: wrong canvas size {im.size}, expected ({CANVAS_W},{CANVAS_H})"

    # 2. Panel background colour at a safe interior point.
    assert px[10, 10][:3] == PANEL_BG[:3], \
        f"{name}: panel BG wrong at (10,10) — got {px[10,10]}"

    # 3. First inventory slot face — confirms draw_inventory() ran.
    cx_inv = INV_START_X + SLOT_W // 2    # = 8+9 = 17
    cy_inv = INV_START_Y + SLOT_H // 2    # = 84+9 = 93
    assert px[cx_inv, cy_inv][:3] == SLOT_FACE[:3], \
        f"{name}: inventory slot face wrong at ({cx_inv},{cy_inv}) — got {px[cx_inv,cy_inv]}"

    # 4. Hotbar slot face.
    cy_hot = HOTBAR_Y + SLOT_H // 2       # = 142+9 = 151
    assert px[cx_inv, cy_hot][:3] == SLOT_FACE[:3], \
        f"{name}: hotbar slot face wrong at ({cx_inv},{cy_hot}) — got {px[cx_inv,cy_hot]}"

    # 5. Entropy bar Zone 1 must be EMPTY (dark), not the red gradient.
    if spec.has_entropy_sidebar:
        ex  = ENT_X + BAR_WIDTH // 2       # = 8+4 = 12
        ey  = BAR_TOP + BAR_HEIGHT // 2    # = 17+26 = 43
        pix = px[ex, ey]
        assert pix[:3] == BAR_EMPTY_BG[:3], \
            f"{name}: entropy bar Zone 1 must be EMPTY at ({ex},{ey}) — got {pix}"

    # 6. FE bar Zone 1 must be EMPTY.
    if spec.has_fe_sidebar:
        fx  = FE_X + BAR_WIDTH // 2        # = 19+4 = 23
        fy  = BAR_TOP + BAR_HEIGHT // 2    # = 17+26 = 43
        pix = px[fx, fy]
        assert pix[:3] == BAR_EMPTY_BG[:3], \
            f"{name}: FE bar Zone 1 must be EMPTY at ({fx},{fy}) — got {pix}"

    # 7. Zone 2 entropy sprite: top row = ENT_TOP_RGBA, bottom row = ENT_BOT_RGBA.
    ent_top_px = px[UV_ENT_U + BAR_WIDTH//2, UV_ENT_V]
    ent_bot_px = px[UV_ENT_U + BAR_WIDTH//2, UV_ENT_V + BAR_HEIGHT - 1]
    assert ent_top_px[:3] == ENT_TOP_RGBA[:3], \
        f"{name}: entropy sprite top wrong — expected {ENT_TOP_RGBA[:3]}, got {ent_top_px}"
    assert ent_bot_px[:3] == ENT_BOT_RGBA[:3], \
        f"{name}: entropy sprite bottom wrong — expected {ENT_BOT_RGBA[:3]}, got {ent_bot_px}"

    # 8. Zone 2 FE sprite: top = FE_TOP_RGBA, bottom = FE_BOT_RGBA.
    fe_top_px = px[UV_FE_U + BAR_WIDTH//2, UV_FE_V]
    fe_bot_px = px[UV_FE_U + BAR_WIDTH//2, UV_FE_V + BAR_HEIGHT - 1]
    assert fe_top_px[:3] == FE_TOP_RGBA[:3], \
        f"{name}: FE sprite top wrong — expected {FE_TOP_RGBA[:3]}, got {fe_top_px}"
    assert fe_bot_px[:3] == FE_BOT_RGBA[:3], \
        f"{name}: FE sprite bottom wrong — expected {FE_BOT_RGBA[:3]}, got {fe_bot_px}"

    # 9. Zone 2 progress sprite centre should be gold-ish (green channel > 100).
    prog_px = px[UV_PROG_U + 12, UV_PROG_V + 8]
    assert prog_px[1] > 100, \
        f"{name}: progress sprite centre looks wrong — expected gold, got {prog_px}"

    # 10. Zone 2 battery sprite gradient check.
    bat_top_px = px[UV_BAT_U + UV_BAT_W//2, UV_BAT_V]
    bat_bot_px = px[UV_BAT_U + UV_BAT_W//2, UV_BAT_V + UV_BAT_H - 1]
    assert bat_top_px[0] > 200, \
        f"{name}: battery sprite top should be bright red (R>200), got {bat_top_px}"
    assert bat_bot_px[0] < 120, \
        f"{name}: battery sprite bottom should be dark red (R<120), got {bat_bot_px}"

    # 11. Inventory gap strip at y=83 must be panel colour (nothing from machine area bled in).
    assert px[10, 83][:3] == PANEL_BG[:3], \
        f"{name}: content leaked into inventory gap at y=83"

    # 12. Pixel just outside the GUI rectangle must be fully transparent.
    assert px[GUI_W+1, GUI_H+1][3] == 0, \
        f"{name}: pixel outside GUI at ({GUI_W+1},{GUI_H+1}) is not transparent"


# ===========================================================================
# SECTION 17 — MAIN: GENERATE, SAVE, VERIFY, DEDUPLICATE
# ===========================================================================
def generate_all():
    # Resolve output directory relative to this script's location.
    out_dir = os.path.join(
        os.path.abspath(os.path.dirname(__file__)),
        "src", "main", "resources", "assets", "alientech", "textures", "gui"
    )
    os.makedirs(out_dir, exist_ok=True)  # create the full path if it doesn't exist

    generated = []

    for spec in MACHINE_SPECS:
        # Blank transparent canvas — pixels outside the GUI area stay alpha=0.
        im = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))

        draw_panel(im)         # Layer 1: grey panel + double bevel border
        draw_inventory(im)     # Layer 2: player inventory + hotbar slots
        draw_sprite_sheet(im)  # Layer 3: Zone 2 filled sprites (bars, arrow, battery)
        spec.generator(im)     # Layer 4: machine-specific Zone 1 content

        out_path = os.path.join(out_dir, spec.filename)
        im.save(out_path, "PNG")

        verify_image(out_path, spec)   # raises AssertionError immediately on failure
        generated.append(out_path)
        print(f"[OK] {os.path.relpath(out_path)}")

    # Duplicate detection: two machines producing identical PNGs means a layout bug.
    hashes = {}
    for path in generated:
        h = hashlib.md5(open(path, 'rb').read()).hexdigest()
        if h in hashes:
            raise AssertionError(
                f"DUPLICATE DETECTED: {os.path.basename(path)} "
                f"is pixel-identical to {os.path.basename(hashes[h])}"
            )
        hashes[h] = path

    # Derive every coordinate that Java will need
    _bat_w   = UV_BAT_W
    _bat_h   = UV_BAT_H
    _bat_x   = (MA_LEFT + MA_RIGHT) // 2 - _bat_w // 2   # = 86
    _bat_y   = MA_TOP + 4                                  # = 21
    _nub_w, _nub_h = 10, 4
    _nub_x   = _bat_x + (_bat_w - _nub_w) // 2            # = 95
    _nub_y   = _bat_y - _nub_h                             # = 17
    _sg      = 6
    _sw2     = SLOT_W * 2 + _sg                            # = 42
    _bsl_x0  = (MA_LEFT + MA_RIGHT) // 2 - _sw2 // 2      # = 79
    _bsl_x1  = _bsl_x0 + SLOT_W + _sg                     # = 103
    _bsl_y   = _bat_y + _bat_h + 6                         # = 63

    _qvt_r_outer = 11
    _qvt_r_inner = 6
    _qvt_r_hub   = 3
    _qvt_gap     = 6
    _qvt_unit_h  = SLOT_H + _qvt_gap + (_qvt_r_outer * 2 + 1)   # = 47
    _qvt_slot_cx = (MA_LEFT + MA_RIGHT) // 2                       # = 100
    _qvt_slot_x  = _qvt_slot_cx - SLOT_W // 2                     # = 91
    _qvt_slot_y  = MA_TOP + (MA_H - _qvt_unit_h) // 2             # = 26
    _qvt_cx      = _qvt_slot_cx                                    # = 100
    _qvt_cy      = _qvt_slot_y + SLOT_H + _qvt_gap + _qvt_r_outer # = 61

    _pyr_base_w  = 17
    _pyr_gap     = 8
    _pyr_unit_w  = SLOT_W + _pyr_gap + _pyr_base_w                # = 43
    _pyr_slot_x  = MA_LEFT + (MA_W - _pyr_unit_w) // 2            # = 78
    _pyr_slot_y  = MA_TOP  + (MA_H - SLOT_H) // 2                 # = 41
    _pyr_left    = _pyr_slot_x + SLOT_W + _pyr_gap                # = 104
    _pyr_bot     = _pyr_slot_y + SLOT_H - 1                       # = 58

    print("\n" + "=" * 70)
    print("  ALIENTECH GUI — COMPLETE JAVA COORDINATE REFERENCE  (V9)")
    print("=" * 70)

    print(f"""
  TEXTURE LAYOUT: 256x256, Zone 1: 0,0->176,166
  
  PANEL: x={GUI_LEFT}, y={GUI_TOP}, {GUI_W}x{GUI_H}
  
  BARS: size={BAR_WIDTH}x{BAR_HEIGHT}, top={BAR_TOP}
  ENTROPY: x={ENT_X}, FE: x={FE_X}
  
  MA AREA: {MA_LEFT},{MA_TOP} -> {MA_RIGHT},{MA_BOT} ({MA_W}x{MA_H})
  
  SPRITES (Zone 2):
    Entropy: u={UV_ENT_U}, v={UV_ENT_V}
    FE: u={UV_FE_U}, v={UV_FE_V}
    Arrow: u={UV_PROG_U}, v={UV_PROG_V} (24x16)
    Battery Fill: u={UV_BAT_U}, v={UV_BAT_V} ({UV_BAT_W}x{UV_BAT_H})
    
  ARROW POS: x={IO3_PROG_X}, y={IO3_PROG_Y}
  
  PRIMAL CATALYST SLOTS: 
    Inputs: x={IO3_COL_X}, y={IO3_COL_Y0}/{IO3_COL_Y1}/{IO3_COL_Y2}
    Output: x={IO3_OUT_X}, y={IO3_OUT_Y}
    
  DECAY CHAMBER SLOTS:
    Input: x={IO1_IN_X}, y={IO1_IN_Y}
    Output: x={IO1_OUT_X}, y={IO1_OUT_Y}
    
  PYRAMID CORE SLOT: x={_pyr_slot_x}, y={_pyr_slot_y}
  
  ANCIENT BATTERY: 
    Body: x={_bat_x}, y={_bat_y}
    Slots: x={_bsl_x0}/{_bsl_x1}, y={_bsl_y}
    
  VACUUM TURBINE SLOT: x={_qvt_slot_x}, y={_qvt_slot_y}
  
  ANCIENT CHARGER SLOT: x={91}, y={41}
  """)

    print("\nALL GUIS REGENERATED — V9 SUCCESS")


if __name__ == "__main__":
    generate_all()
