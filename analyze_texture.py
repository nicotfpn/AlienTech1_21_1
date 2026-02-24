from PIL import Image
import numpy as np

# Load texture
img = Image.open('src/main/resources/assets/nicoalientech/textures/gui/primal_catalyst_gui.png')
img_array = np.array(img.convert('RGBA'))

print(f"Texture size: {img.size}")
print(f"Looking for slot positions (16x16 dark squares)...\n")

# Convert to grayscale
grayscale = np.mean(img_array[:,:,:3], axis=2)

# Look for dark pixels (slots typically darker)
dark_threshold = 120
dark_pixels = grayscale < dark_threshold

# Find contiguous regions
from scipy import ndimage
labeled, num_features = ndimage.label(dark_pixels)

slot_candidates = []
for i in range(1, min(num_features + 1, 200)):
    region = np.where(labeled == i)
    if len(region[0]) > 100:  # At least 100 pixels
        y_min, y_max = region[0].min(), region[0].max()
        x_min, x_max = region[1].min(), region[1].max()
        width = x_max - x_min + 1
        height = y_max - y_min + 1
        center_x = (x_min + x_max) // 2
        center_y = (y_min + y_max) // 2
        
        # Slots are roughly 16x16
        if 12 < width < 22 and 12 < height < 22:
            slot_candidates.append({
                'center_x': center_x,
                'center_y': center_y,
                'x_min': x_min,
                'y_min': y_min,
                'x_max': x_max,
                'y_max': y_max,
                'width': width,
                'height': height
            })

# Sort by position (top to bottom, left to right)
slot_candidates.sort(key=lambda s: (s['y_min'], s['x_min']))

print(f"Found {len(slot_candidates)} potential slots:\n")
for i, slot in enumerate(slot_candidates):
    print(f"Slot {i}: center=({slot['center_x']}, {slot['center_y']}), " +
          f"bounds=[{slot['x_min']}-{slot['x_max']}, {slot['y_min']}-{slot['y_max']}], " +
          f"size={slot['width']}x{slot['height']}")

# For GUI rendering in Minecraft, slot position usually refers to top-left corner + 1
# So for a slot centered at (center_x, center_y) with size 16x16,
# the GUI position would be approximately (center_x - 8, center_y - 8)
print("\n\nSlot positions for Menu.java (approximate top-left + offset):")
for i, slot in enumerate(slot_candidates):
    gui_x = slot['x_min'] 
    gui_y = slot['y_min']
    print(f"Slot {i}: new SlotItemHandler(..., {i}, {gui_x}, {gui_y})")
