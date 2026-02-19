"""
Script para criar as texturas GUI do AlienTech
Estilo Mekanism com tema Egípcio (Dourado + Azul Lápis-Lazúli)
"""

from PIL import Image, ImageDraw

# ==================== Cores Egípcias ====================
COLORS = {
    'gold_dark': (139, 115, 50, 255),      # Dourado escuro
    'gold': (212, 175, 55, 255),           # Dourado principal
    'gold_light': (255, 223, 136, 255),    # Dourado claro (highlight)
    'lapis': (38, 97, 156, 255),           # Azul lápis-lazúli
    'lapis_dark': (26, 64, 103, 255),      # Lápis escuro
    'lapis_light': (65, 144, 200, 255),    # Lápis claro
    'sand': (194, 178, 128, 255),          # Cor de areia
    'sand_dark': (139, 128, 92, 255),      # Areia escura
    'black': (20, 20, 20, 255),            # Preto para slots
    'gray': (60, 60, 60, 255),             # Cinza
    'energy_full': (255, 200, 50, 255),    # Energia cheia (dourado brilhante)
    'energy_empty': (40, 40, 40, 255),     # Energia vazia
}


def draw_egyptian_border(draw, x, y, width, height, border_width=3):
    """Desenha uma borda egípcia dourada com efeito 3D"""
    # Borda externa dourada clara (highlight)
    draw.rectangle([x, y, x + width - 1, y + height - 1], outline=COLORS['gold_light'])
    
    # Borda média dourada
    draw.rectangle([x + 1, y + 1, x + width - 2, y + height - 2], outline=COLORS['gold'])
    
    # Borda interna escura (shadow)
    draw.rectangle([x + 2, y + 2, x + width - 3, y + height - 3], outline=COLORS['gold_dark'])


def draw_slot_egyptian(draw, x, y, size=18):
    """Desenha um slot estilo egípcio"""
    # Fundo escuro
    draw.rectangle([x, y, x + size - 1, y + size - 1], fill=COLORS['black'])
    
    # Borda 3D invertida (sombra em cima/esquerda, luz em baixo/direita)
    draw.line([(x, y), (x + size - 1, y)], fill=COLORS['gray'])  # Top
    draw.line([(x, y), (x, y + size - 1)], fill=COLORS['gray'])  # Left
    draw.line([(x, y + size - 1), (x + size - 1, y + size - 1)], fill=COLORS['gold_dark'])  # Bottom
    draw.line([(x + size - 1, y), (x + size - 1, y + size - 1)], fill=COLORS['gold_dark'])  # Right


def draw_energy_bar_vertical(draw, x, y, width, height):
    """Desenha uma barra de energia vertical estilo Mekanism/Obelisco"""
    # Fundo da barra
    draw.rectangle([x, y, x + width - 1, y + height - 1], fill=COLORS['energy_empty'])
    
    # Borda dourada
    draw.rectangle([x, y, x + width - 1, y + height - 1], outline=COLORS['gold'])
    draw.rectangle([x - 1, y - 1, x + width, y + height], outline=COLORS['gold_dark'])


def draw_arrow_egyptian(draw, x, y):
    """Desenha seta de progresso estilo egípcio"""
    arrow_color = COLORS['sand_dark']
    
    # Base da seta
    draw.rectangle([x, y + 6, x + 18, y + 10], fill=arrow_color)
    
    # Ponta da seta
    for i in range(8):
        draw.line([(x + 18 + i, y + 8 - i), (x + 18 + i, y + 8 + i)], fill=arrow_color)


def draw_hieroglyph_pattern(draw, x, y, width, height):
    """Desenha um padrão sutil de hieróglifos na borda"""
    # Padrão simples de linhas decorativas
    pattern_color = COLORS['gold']
    step = 8
    
    for i in range(x + step, x + width - step, step * 2):
        draw.line([(i, y + 2), (i + 3, y + 2)], fill=pattern_color)
        draw.line([(i, y + height - 3), (i + 3, y + height - 3)], fill=pattern_color)


def create_primal_catalyst_gui(output_path):
    """Cria GUI do Primal Catalyst - Estilo Mekanism Egípcio"""
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    gui_width = 176
    gui_height = 166
    
    # Fundo principal (cor de areia)
    draw.rectangle([0, 0, gui_width - 1, gui_height - 1], fill=COLORS['sand'])
    
    # Borda dourada egípcia
    draw_egyptian_border(draw, 0, 0, gui_width, gui_height)
    
    # Padrão decorativo no topo
    draw_hieroglyph_pattern(draw, 0, 0, gui_width, gui_height)
    
    # Painel de processamento (fundo lápis-lazúli)
    panel_x, panel_y = 28, 14
    panel_w, panel_h = 120, 60
    draw.rectangle([panel_x, panel_y, panel_x + panel_w - 1, panel_y + panel_h - 1], fill=COLORS['lapis_dark'])
    draw.rectangle([panel_x, panel_y, panel_x + panel_w - 1, panel_y + panel_h - 1], outline=COLORS['gold'])
    
    # Barra de energia vertical (lado esquerdo, estilo obelisco)
    energy_x, energy_y = 8, 14
    energy_w, energy_h = 14, 60
    draw_energy_bar_vertical(draw, energy_x, energy_y, energy_w, energy_h)
    
    # 3 slots de input (dentro do painel)
    draw_slot_egyptian(draw, 32, 17)   # Input 1
    draw_slot_egyptian(draw, 32, 37)   # Input 2
    draw_slot_egyptian(draw, 32, 57)   # Input 3
    
    # Seta de progresso
    draw_arrow_egyptian(draw, 78, 37)
    
    # Slot de output
    draw_slot_egyptian(draw, 125, 29)
    
    # Inventário do jogador (com borda dourada sutil)
    inv_y = 84
    draw.rectangle([7, inv_y - 1, 169, inv_y + 53], outline=COLORS['gold_dark'])
    
    for row in range(3):
        for col in range(9):
            draw_slot_egyptian(draw, 8 + col * 18, inv_y + row * 18)
    
    # Hotbar
    hotbar_y = 142
    draw.rectangle([7, hotbar_y - 1, 169, hotbar_y + 17], outline=COLORS['gold_dark'])
    for col in range(9):
        draw_slot_egyptian(draw, 8 + col * 18, hotbar_y)
    
    # Área para seta animada (lado direito da textura)
    arrow_anim_x = 176
    arrow_anim_y = 37
    arrow_filled = COLORS['energy_full']
    draw.rectangle([arrow_anim_x, arrow_anim_y + 6, arrow_anim_x + 18, arrow_anim_y + 10], fill=arrow_filled)
    for i in range(8):
        draw.line([(arrow_anim_x + 18 + i, arrow_anim_y + 8 - i), 
                   (arrow_anim_x + 18 + i, arrow_anim_y + 8 + i)], fill=arrow_filled)
    
    # Área para barra de energia preenchida (para animação)
    energy_filled_x = 176
    energy_filled_y = 14
    draw.rectangle([energy_filled_x, energy_filled_y, 
                    energy_filled_x + 14 - 1, energy_filled_y + 60 - 1], 
                   fill=COLORS['energy_full'])
    
    img.save(output_path)
    print(f"GUI Primal Catalyst salva em: {output_path}")


def create_pyramid_core_gui(output_path):
    """Cria GUI do Pyramid Core - Estilo Mekanism Egípcio"""
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    gui_width = 176
    gui_height = 166
    
    # Fundo principal (cor de areia)
    draw.rectangle([0, 0, gui_width - 1, gui_height - 1], fill=COLORS['sand'])
    
    # Borda dourada egípcia
    draw_egyptian_border(draw, 0, 0, gui_width, gui_height)
    draw_hieroglyph_pattern(draw, 0, 0, gui_width, gui_height)
    
    # Painel central (fundo lápis-lazúli)
    panel_x, panel_y = 28, 14
    panel_w, panel_h = 120, 60
    draw.rectangle([panel_x, panel_y, panel_x + panel_w - 1, panel_y + panel_h - 1], fill=COLORS['lapis_dark'])
    draw.rectangle([panel_x, panel_y, panel_x + panel_w - 1, panel_y + panel_h - 1], outline=COLORS['gold'])
    
    # Barra de energia grande (central)
    energy_x, energy_y = 8, 14
    energy_w, energy_h = 14, 60
    draw_energy_bar_vertical(draw, energy_x, energy_y, energy_w, energy_h)
    
    # Slot de Alloy (centro do painel)
    draw_slot_egyptian(draw, 80, 35)
    
    # Inventário
    inv_y = 84
    draw.rectangle([7, inv_y - 1, 169, inv_y + 53], outline=COLORS['gold_dark'])
    for row in range(3):
        for col in range(9):
            draw_slot_egyptian(draw, 8 + col * 18, inv_y + row * 18)
    
    # Hotbar
    hotbar_y = 142
    draw.rectangle([7, hotbar_y - 1, 169, hotbar_y + 17], outline=COLORS['gold_dark'])
    for col in range(9):
        draw_slot_egyptian(draw, 8 + col * 18, hotbar_y)
    
    # Área para barra de energia preenchida
    energy_filled_x = 176
    energy_filled_y = 14
    draw.rectangle([energy_filled_x, energy_filled_y, 
                    energy_filled_x + 14 - 1, energy_filled_y + 60 - 1], 
                   fill=COLORS['energy_full'])
    
    img.save(output_path)
    print(f"GUI Pyramid Core salva em: {output_path}")


# Criar as GUIs
create_primal_catalyst_gui("src/main/resources/assets/alientech/textures/gui/primal_catalyst_gui.png")
create_pyramid_core_gui("src/main/resources/assets/alientech/textures/gui/pyramid_core_gui.png")

print("GUIs egípcias criadas com sucesso!")

