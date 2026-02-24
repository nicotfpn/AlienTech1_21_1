"""
Generate Ancient Battery GUI texture with Egyptian theme.
Uses the same style as Primal Catalyst GUI.
"""
from PIL import Image, ImageDraw

# GUI dimensions
WIDTH = 256
HEIGHT = 256

# Colors - Egyptian theme
SAND = (194, 178, 128)          # Background
GOLD_LIGHT = (212, 175, 55)     # Gold highlights
GOLD_DARK = (139, 115, 50)      # Gold shadows
LAPIS = (38, 97, 156)           # Blue panel
LAPIS_LIGHT = (65, 148, 200)    # Blue highlights
SLOT_BG = (55, 55, 70)          # Slot background
SLOT_BORDER = (139, 139, 139)   # Slot border

def create_battery_gui():
    """Create Ancient Battery GUI texture."""
    img = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Main panel (176x166)
    panel_x, panel_y = 0, 0
    panel_w, panel_h = 176, 166
    
    # Fill with sand color
    draw.rectangle([panel_x, panel_y, panel_x + panel_w - 1, panel_y + panel_h - 1], fill=SAND)
    
    # Gold 3D border (outer)
    draw.rectangle([panel_x, panel_y, panel_x + panel_w - 1, panel_y + 3], fill=GOLD_LIGHT)  # Top
    draw.rectangle([panel_x, panel_y, panel_x + 3, panel_y + panel_h - 1], fill=GOLD_LIGHT)  # Left
    draw.rectangle([panel_x, panel_y + panel_h - 4, panel_x + panel_w - 1, panel_y + panel_h - 1], fill=GOLD_DARK)  # Bottom
    draw.rectangle([panel_x + panel_w - 4, panel_y, panel_x + panel_w - 1, panel_y + panel_h - 1], fill=GOLD_DARK)  # Right
    
    # Lapis panel (machine area)
    lapis_x, lapis_y = 26, 14
    lapis_w, lapis_h = 124, 60
    draw.rectangle([lapis_x, lapis_y, lapis_x + lapis_w - 1, lapis_y + lapis_h - 1], fill=LAPIS)
    
    # Lapis 3D border
    draw.rectangle([lapis_x, lapis_y, lapis_x + lapis_w - 1, lapis_y + 2], fill=LAPIS_LIGHT)
    draw.rectangle([lapis_x, lapis_y, lapis_x + 2, lapis_y + lapis_h - 1], fill=LAPIS_LIGHT)
    draw.rectangle([lapis_x, lapis_y + lapis_h - 3, lapis_x + lapis_w - 1, lapis_y + lapis_h - 1], fill=(20, 60, 100))
    draw.rectangle([lapis_x + lapis_w - 3, lapis_y, lapis_x + lapis_w - 1, lapis_y + lapis_h - 1], fill=(20, 60, 100))
    
    # Energy bar background (left side, vertical)
    bar_x, bar_y = 8, 14
    bar_w, bar_h = 14, 60
    draw.rectangle([bar_x, bar_y, bar_x + bar_w - 1, bar_y + bar_h - 1], fill=(40, 40, 50))
    draw.rectangle([bar_x, bar_y, bar_x + bar_w - 1, bar_y + 1], fill=(60, 60, 70))
    draw.rectangle([bar_x, bar_y + bar_h - 2, bar_x + bar_w - 1, bar_y + bar_h - 1], fill=(20, 20, 30))
    
    # Charge slot (center)
    slot_x, slot_y = 79, 34
    slot_size = 18
    draw.rectangle([slot_x, slot_y, slot_x + slot_size - 1, slot_y + slot_size - 1], fill=SLOT_BG)
    draw.rectangle([slot_x, slot_y, slot_x + slot_size - 1, slot_y], fill=SLOT_BORDER)
    draw.rectangle([slot_x, slot_y, slot_x, slot_y + slot_size - 1], fill=SLOT_BORDER)
    
    # Player inventory area
    inv_y = 84
    for row in range(3):
        for col in range(9):
            sx = 8 + col * 18
            sy = inv_y + row * 18
            draw.rectangle([sx, sy, sx + 17, sy + 17], fill=SLOT_BG)
            draw.rectangle([sx, sy, sx + 17, sy], fill=SLOT_BORDER)
            draw.rectangle([sx, sy, sx, sy + 17], fill=SLOT_BORDER)
    
    # Hotbar
    hotbar_y = 142
    for col in range(9):
        sx = 8 + col * 18
        sy = hotbar_y
        draw.rectangle([sx, sy, sx + 17, sy + 17], fill=SLOT_BG)
        draw.rectangle([sx, sy, sx + 17, sy], fill=SLOT_BORDER)
        draw.rectangle([sx, sy, sx, sy + 17], fill=SLOT_BORDER)
    
    # Energy bar fill texture (at position 176, 14)
    energy_x = 176
    energy_y = 14
    for y in range(60):
        # Gradient from orange (top) to yellow (bottom)
        ratio = y / 60
        r = int(255 - ratio * 50)
        g = int(150 + ratio * 50)
        b = int(30)
        draw.rectangle([energy_x, energy_y + y, energy_x + 13, energy_y + y], fill=(r, g, b))
    
    # Add glow effect to energy bar
    for y in range(60):
        if y % 4 == 0:
            draw.rectangle([energy_x + 2, energy_y + y, energy_x + 4, energy_y + y], fill=(255, 255, 200, 128))
    
    return img

if __name__ == "__main__":
    img = create_battery_gui()
    output_path = "src/main/resources/assets/alientech/textures/gui/ancient_battery_gui.png"
    img.save(output_path)
    print(f"Created: {output_path}")
