import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TextureGenerator {

    // Dimensões do GUI (baseado no seu código)
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    // Cores padrão da GUI do Minecraft
    private static final Color GUI_BASE_COLOR = new Color(0xC6C6C6);
    private static final Color BORDER_LIGHT = new Color(0xFFFFFF);
    private static final Color BORDER_DARK = new Color(0x555555);

    // Cores baseadas no seu código (drawSlotBackground)
    private static final Color CUSTOM_SLOT_BORDER = new Color(0x8B8B8B);
    private static final Color CUSTOM_SLOT_INNER = new Color(0x373737);
    private static final Color CUSTOM_SLOT_SHADOW = new Color(0x272727);

    // Cores baseadas no seu código (drawEnergyBar)
    private static final Color ENERGY_BAR_BG = new Color(0x141414);
    private static final Color ENERGY_BAR_BORDER = new Color(0x555555);

    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 1. Desenhar o fundo base da GUI
        drawBaseGui(g2d);

        // 2. Desenhar os Slots Personalizados (Baseado nas coordenadas do seu código)
        // Charge Slot (Top): x=26, y=20
        drawCustomSlotBackground(g2d, 26, 20);
        // Discharge Slot (Bottom): x=26, y=50
        drawCustomSlotBackground(g2d, 26, 50);

        // 3. Desenhar o espaço reservado da Barra de Energia Gigante
        // x=66, y=14, w=44, h=60
        drawEnergyBarPlaceholder(g2d, 66, 14, 44, 60);

        // 4. Desenhar o Inventário do Jogador Padrão
        // O inventário principal geralmente começa em Y=84
        drawPlayerInventory(g2d, 8, 84);
        // A hotbar geralmente começa em Y=142
        drawPlayerHotbar(g2d, 8, 142);

        g2d.dispose();

        // Salvar a imagem
        try {
            File outputFile = new File("ancient_battery_gui.png");
            ImageIO.write(image, "png", outputFile);
            System.out.println("Textura gerada com sucesso: " + outputFile.getAbsolutePath());
            System.out.println("Mova este arquivo para: assets/alientech/textures/gui/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void drawBaseGui(Graphics2D g) {
        // Preenchimento principal
        g.setColor(GUI_BASE_COLOR);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Bordas externas (efeito chanfrado padrão MC)
        g.setColor(BORDER_LIGHT);
        g.drawLine(0, 0, WIDTH - 1, 0); // Topo
        g.drawLine(0, 0, 0, HEIGHT - 1); // Esquerda

        g.setColor(BORDER_DARK);
        g.drawLine(WIDTH - 1, 0, WIDTH - 1, HEIGHT - 1); // Direita
        g.drawLine(0, HEIGHT - 1, WIDTH - 1, HEIGHT - 1); // Base
    }

    // Simula o método drawSlotBackground do seu código
    private static void drawCustomSlotBackground(Graphics2D g, int x, int y) {
        // Border (0xFF8B8B8B)
        g.setColor(CUSTOM_SLOT_BORDER);
        g.fillRect(x, y, 18, 18);

        // Inner (0xFF373737)
        g.setColor(CUSTOM_SLOT_INNER);
        g.fillRect(x + 1, y + 1, 16, 16);

        // Shadows (0xFF272727) - Top and Left inner shadow
        g.setColor(CUSTOM_SLOT_SHADOW);
        g.drawLine(x + 1, y + 1, x + 16, y + 1); // Top
        g.drawLine(x + 1, y + 1, x + 1, y + 16); // Left
    }

    // Desenha a área de fundo da barra de energia baseada no seu código
    private static void drawEnergyBarPlaceholder(Graphics2D g, int x, int y, int width, int height) {
        // Fundo escuro (0xFF141414)
        g.setColor(ENERGY_BAR_BG);
        g.fillRect(x, y, width, height);

        // Borda (0xFF555555)
        g.setColor(ENERGY_BAR_BORDER);
        g.drawRect(x, y, width - 1, height - 1);
    }

    // Desenha os slots padrão do inventário do jogador (3 linhas de 9)
    private static void drawPlayerInventory(Graphics2D g, int startX, int startY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawStandardSlot(g, startX + col * 18, startY + row * 18);
            }
        }
    }

    // Desenha a hotbar do jogador (1 linha de 9)
    private static void drawPlayerHotbar(Graphics2D g, int startX, int startY) {
        for (int col = 0; col < 9; col++) {
            drawStandardSlot(g, startX + col * 18, startY);
        }
    }

    // Desenha um slot padrão de 18x18 do Minecraft (mais claro que os seus
    // customizados)
    private static void drawStandardSlot(Graphics2D g, int x, int y) {
        g.setColor(new Color(0x8B8B8B)); // Borda externa e preenchimento base
        g.fillRect(x, y, 18, 18);

        g.setColor(new Color(0x373737)); // Sombra interna superior/esquerda
        g.drawLine(x, y, x + 17, y);
        g.drawLine(x, y, x, y + 17);

        g.setColor(new Color(0xFFFFFF)); // Luz interna inferior/direita
        g.drawLine(x + 17, y + 17, x + 17, y);
        g.drawLine(x + 17, y + 17, x, y + 17);
    }
}
