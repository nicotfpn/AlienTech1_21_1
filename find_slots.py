from PIL import Image
import numpy as np
from scipy import ndimage
import os

os.chdir(r'c:\Users\Pichau\IdeaProjects\AlienTech1_21_1')
img = Image.open('src/main/resources/assets/nicoalientech/textures/gui/primal_catalyst_gui.png')
arr = np.array(img.convert('RGBA'))

gray = np.mean(arr[:,:,:3], axis=2)
dark = gray < 130
labeled, num = ndimage.label(dark)
slots = []

for i in range(1, num + 1):
    region = np.where(labeled == i)
    if len(region[0]) > 80:
        y_min, y_max = region[0].min(), region[0].max()
        x_min, x_max = region[1].min(), region[1].max()
        w, h = x_max - x_min + 1, y_max - y_min + 1
        if 12 < w < 22 and 12 < h < 22:
            slots.append({'x': x_min, 'y': y_min})

slots.sort(key=lambda s: (s['y'], s['x']))

print('Os 4 primeiros slots detectados:')
for i, s in enumerate(slots[:4]):
    print(f'Slot {i}: x={s["x"]}, y={s["y"]}')

# Menu Java format: addSlot(new SlotItemHandler(..., slotIndex, x, y))
# Em Minecraft, as coordenadas do GUI começam em (0,0) na canto superior esquerdo do container
# A textura 256x256 é escalada, mas os valores x,y usados são as coordenadas relativas ao container visual
print('\nPara o Menu.java (primeiro 4 slots):')
for i in range(min(4, len(slots))):
    s = slots[i]
    print(f'this.addSlot(new SlotItemHandler(..., {i}, {s["x"]}, {s["y"]}));')
