import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Gera as três texturas de GUI no estilo Egípcio/Tech usando o padrão de Atlas
 * do Minecraft.
 */
public class AlienTextures {

    // Cores do estilo Egípcio/Tech
    private static final Color BG_TAN = new Color(0xD0C0A0); // Fundo bege
    private static final Color BORDER_GOLD_LIGHT = new Color(0xE0D060);
    private static final Color BORDER_GOLD_DARK = new Color(0xA09030);
    private static final Color SLOT_BG_BLUE = new Color(0x203050); // Fundo azul dos slots customizados

    // Cores padrão do inventário do jogador (para manter compatibilidade visual)
    private static final Color SLOT_GRAY_BG = new Color(0x8B8B8B);
    private static final Color SLOT_GRAY_DARK = new Color(0x373737);
    private static final Color SLOT_GRAY_LIGHT = new Color(0xFFFFFF);

    // Cores das barras de energia
    private static final Color ENERGY_BG = new Color(0x141414);
    private static final Color ENERGY_FILL_GREEN = new Color(0x00FF00);
    private static final Color ENERGY_FILL_HIGHLIGHT = new Color(0x40FF40);

    public static void main(String[] args) throws IOException {
        generateAncientBattery();
        generatePyramidCore();
        generatePrimalCatalyst();
    }

    private static void generateAncientBattery() throws IOException {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // === ÁREA PRINCIPAL (0,0) ===
        drawEgyptianBackground(g, 176, 166);

        // Slots Customizados (Azul/Dourado)
        drawCustomSlot(g, 26, 20); // Charge
        drawCustomSlot(g, 26, 50); // Discharge

        // Fundo da Barra Gigante (Vazio)
        drawEnergyBackground(g, 66, 14, 44, 60);

        // Inventário do Jogador (Padrão Cinza)
        drawPlayerSlots(g, 8, 84);

        // === ÁREA DO ATLAS (X > 176) ===
        // Barra Gigante CHEIA (Local: X=176, Y=0)
        drawEnergyFill(g, 176, 0, 44, 60, true);

        g.dispose();
        ImageIO.write(img, "png", new File("ancient_battery_gui.png"));
        System.out.println("Gerado: ancient_battery_gui.png");
    }

    private static void generatePyramidCore() throws IOException {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // === ÁREA PRINCIPAL (0,0) ===
        drawEgyptianBackground(g, 176, 166);

        // Fundo da Barra Vertical Esquerda (Vazio) (x=8, y=14, w=14, h=60)
        drawEnergyBackground(g, 8, 14, 14, 60);

        // Slot Central para o Item (x=80, y=35)
        drawCustomSlot(g, 80, 35);

        // Inventário do Jogador
        drawPlayerSlots(g, 8, 84);

        // === ÁREA DO ATLAS (X > 176) ===
        // Barra Vertical CHEIA (Local: X=176, Y=14, para bater com o código fornecido)
        drawEnergyFill(g, 176, 14, 14, 60, false);

        g.dispose();
        ImageIO.write(img, "png", new File("pyramid_core_gui.png"));
        System.out.println("Gerado: pyramid_core_gui.png");
    }

    private static void generatePrimalCatalyst() throws IOException {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // === ÁREA PRINCIPAL (0,0) ===
        drawEgyptianBackground(g, 176, 166);

        // Fundo da Barra Vertical Esquerda (Vazio)
        drawEnergyBackground(g, 8, 14, 14, 60);

        // Slots de Entrada (3 slots veriticais na esquerda)
        // x=33, y=18, 38, 58
        drawCustomSlot(g, 33, 18);
        drawCustomSlot(g, 33, 38);
        drawCustomSlot(g, 33, 58);

        // Slot de Saída (Grande/Direita)
        // x=126, y=30 (Um pouco maior, 26x26)
        drawLargeCustomSlot(g, 126, 30);

        // Fundo da Seta (Vazio) - entre inputs e output
        // x=70, y=35
        g.setColor(SLOT_BG_BLUE.darker());
        g.fillRect(70, 35, 24, 17); // Fundo escuro da seta

        // Inventário do Jogador
        drawPlayerSlots(g, 8, 84);

        // === ÁREA DO ATLAS (X > 176) ===
        // 1. Barra Vertical CHEIA
        drawEnergyFill(g, 176, 14, 14, 60, false);

        // 2. Seta de Progresso CHEIA (Local: X=176, Y=37)
        g.setColor(BORDER_GOLD_LIGHT);
        // Desenha uma seta
        int arrowX = 176;
        int arrowY = 37;
        int[] xPoints = { arrowX, arrowX + 16, arrowX + 16, arrowX + 24, arrowX + 16, arrowX + 16, arrowX };
        int[] yPoints = { arrowY + 4, arrowY + 4, arrowY + 0, arrowY + 8, arrowY + 17, arrowY + 13, arrowY + 13 };
        g.fillPolygon(xPoints, yPoints, 7);

        g.dispose();
        ImageIO.write(img, "png", new File("primal_catalyst_gui.png"));
        System.out.println("Gerado: primal_catalyst_gui.png");
    }

    // --- MÉTODOS AUXILIARES DE DESENHO ---

    private static void drawEgyptianBackground(Graphics2D g, int w, int h) {
        g.setColor(BG_TAN);
        g.fillRect(0, 0, w, h);
        // Borda Dourada
        g.setColor(BORDER_GOLD_LIGHT);
        g.drawLine(0, 0, w - 1, 0);
        g.drawLine(0, 0, 0, h - 1);
        g.setColor(BORDER_GOLD_DARK);
        g.drawLine(w - 1, 0, w - 1, h - 1);
        g.drawLine(0, h - 1, w - 1, h - 1);
    }

    private static void drawCustomSlot(Graphics2D g, int x, int y) {
        g.setColor(SLOT_BG_BLUE);
        g.fillRect(x, y, 18, 18);
        g.setColor(BORDER_GOLD_DARK);
        g.drawLine(x, y, x + 17, y);
        g.drawLine(x, y, x, y + 17);
        g.setColor(BORDER_GOLD_LIGHT);
        g.drawLine(x + 17, y + 17, x + 17, y);
        g.drawLine(x + 17, y + 17, x, y + 17);
    }

    private static void drawLargeCustomSlot(Graphics2D g, int x, int y) {
        int w = 26;
        int h = 26;
        g.setColor(SLOT_BG_BLUE);
        g.fillRect(x, y, w, h);
        g.setColor(BORDER_GOLD_DARK);
        g.drawLine(x, y, x + w - 1, y);
        g.drawLine(x, y, x, y + h - 1);
        g.setColor(BORDER_GOLD_LIGHT);
        g.drawLine(x + w - 1, y + h - 1, x + w - 1, y);
        g.drawLine(x + w - 1, y + h - 1, x, y + h - 1);
    }

    private static void drawPlayerSlots(Graphics2D g, int startX, int startY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawStandardGraySlot(g, startX + col * 18, startY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawStandardGraySlot(g, startX + col * 18, startY + 58);
        }
    }

    private static void drawStandardGraySlot(Graphics2D g, int x, int y) {
        g.setColor(SLOT_GRAY_BG);
        g.fillRect(x, y, 18, 18);
        g.setColor(SLOT_GRAY_DARK);
        g.drawLine(x, y, x + 17, y);
        g.drawLine(x, y, x, y + 17);
        g.setColor(SLOT_GRAY_LIGHT);
        g.drawLine(x + 17, y + 17, x + 17, y);
        g.drawLine(x + 17, y + 17, x, y + 17);
    }

    private static void drawEnergyBackground(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(ENERGY_BG);
        g.fillRect(x, y, w, h);
        g.setColor(BORDER_GOLD_DARK);
        g.drawRect(x, y, w - 1, h - 1);
    }

    private static void drawEnergyFill(Graphics2D g, int x, int y, int w, int h, boolean giant) {
        int innerX = x + 1;
        int innerY = y + 1;
        int innerW = w - 2;
        int innerH = h - 2;

        g.setColor(ENERGY_FILL_GREEN);
        g.fillRect(innerX, innerY, innerW, innerH);

        // Highlight lateral
        g.setColor(ENERGY_FILL_HIGHLIGHT);
        if (giant) {
            g.fillRect(innerX, innerY, 4, innerH); // Highlight mais grosso para a barra gigante
        } else {
            g.fillRect(innerX, innerY, 2, innerH); // Highlight fino para barras verticais
        }
    }
}
