from PIL import Image, ImageDraw
import sys
import os

def create_stone_texture(filename, accent_color, is_charger=False):
    # Base size
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    pixels = img.load()

    # Stone Palette (Sandstone/Limestone style)
    colors = {
        'base': (180, 170, 150, 255),
        'dark': (160, 150, 130, 255),
        'light': (200, 190, 170, 255),
        'accent': accent_color,
        'gold': (212, 175, 55, 255)
    }

    # Fill base stone texture
    for x in range(16):
        for y in range(16):
            if (x + y) % 3 == 0:
                pixels[x, y] = colors['dark']
            elif (x * y) % 5 == 0:
                pixels[x, y] = colors['light']
            else:
                pixels[x, y] = colors['base']

    draw = ImageDraw.Draw(img)

    if is_charger:
        # Charger: Gold rim and center energy core
        # Gold Border
        draw.rectangle([0, 0, 15, 1], fill=colors['gold'])
        draw.rectangle([0, 14, 15, 15], fill=colors['gold'])
        draw.rectangle([0, 0, 1, 15], fill=colors['gold'])
        draw.rectangle([14, 0, 15, 15], fill=colors['gold'])

        # Energy Core (Center)
        draw.rectangle([5, 5, 10, 10], fill=colors['accent'])
        pixels[7, 7] = (255, 255, 255, 255) # Spark
    
    else:
        # Battery: Stone casing with vertical energy indicator
        # Stone borders
        draw.rectangle([0, 0, 15, 2], fill=colors['dark'])
        draw.rectangle([0, 13, 15, 15], fill=colors['dark'])
        
        # Vertical energy strip
        draw.rectangle([6, 3, 9, 12], fill=colors['accent'])

    # Ensure directory exists
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    img.save(filename)
    print(f"Created {filename}")

if __name__ == "__main__":
    base_path = "src/main/resources/assets/alientech/textures/block"
    
    # Charger: Electric Blue accent
    create_stone_texture(os.path.join(base_path, "ancient_charger.png"), (0, 200, 255, 255), is_charger=True)
    
    # Battery: Energy Red/Green (Base state usually empty or full? Let's use Cyan for energy)
    create_stone_texture(os.path.join(base_path, "ancient_battery.png"), (0, 255, 200, 255), is_charger=False)
