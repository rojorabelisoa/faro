package com.faro.demo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/** Photos de démonstration dessinées (pièce stylisée + légende) : aucune image à télécharger. */
final class PhotosDemo {

    enum Scene { CUISINE, SALLE_DE_BAINS, PIECE, JARDIN }

    private PhotosDemo() {
    }

    static byte[] jpeg(Scene scene, String legende, long graine) throws IOException {
        int l = 1200;
        int h = 900;
        BufferedImage image = new BufferedImage(l, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.util.Random alea = new java.util.Random(graine);
        int sol = 610;
        Color mur = scene == Scene.JARDIN ? new Color(150, 195, 230) : new Color(222 - alea.nextInt(20), 212, 196);
        g.setPaint(new GradientPaint(0, 0, mur.brighter(), 0, sol, mur));
        g.fillRect(0, 0, l, sol);
        // Sol : parquet, carrelage ou pelouse.
        Color teinteSol = switch (scene) {
            case SALLE_DE_BAINS, CUISINE -> new Color(190, 186, 180);
            case JARDIN -> new Color(96, 150, 70);
            case PIECE -> new Color(170, 120, 75);
        };
        g.setPaint(new GradientPaint(0, sol, teinteSol, 0, h, teinteSol.darker()));
        g.fillRect(0, sol, l, h - sol);
        g.setColor(teinteSol.darker());
        g.setStroke(new BasicStroke(2));
        for (int x = -400; x < l + 400; x += scene == Scene.PIECE ? 70 : 140) {
            g.drawLine(l / 2 + (x - l / 2) / 3, sol, x, h);
        }
        switch (scene) {
            case SALLE_DE_BAINS -> {
                g.setColor(new Color(238, 238, 235));
                for (int y = 120; y < sol; y += 60) {
                    for (int x = 0; x < l; x += 120) {
                        g.drawRect(x + (y / 60 % 2) * 60, y, 120, 60);
                    }
                }
                g.setColor(Color.WHITE);
                g.fillRoundRect(140, 470, 520, 160, 60, 60);
                g.setColor(new Color(200, 200, 200));
                g.drawRoundRect(140, 470, 520, 160, 60, 60);
                g.fillRoundRect(820, 380, 160, 40, 10, 10);
            }
            case CUISINE -> {
                g.setColor(new Color(120, 90, 70));
                g.fillRect(80, 430, 760, 180);
                g.setColor(new Color(235, 230, 220));
                g.fillRect(80, 410, 760, 22);
                g.setColor(new Color(120, 90, 70));
                g.fillRect(120, 150, 640, 160);
                g.setColor(new Color(90, 65, 50));
                for (int x = 120; x < 760; x += 160) {
                    g.drawRect(x, 150, 160, 160);
                    g.drawRect(x - 40, 430, 190, 180);
                }
            }
            case PIECE -> {
                g.setColor(new Color(255, 255, 255, 120));
                g.fillRect(0, 590, l, 20);
            }
            case JARDIN -> {
                g.setColor(new Color(210, 200, 185));
                g.fillRect(0, 260, 520, 350);
                g.setColor(new Color(150, 80, 60));
                g.fillPolygon(new int[] {-20, 260, 540}, new int[] {270, 120, 270}, 3);
                g.setColor(new Color(60, 110, 50));
                g.fillOval(760, 260, 260, 260);
                g.fillRect(870, 480, 40, 140);
            }
        }
        if (scene != Scene.JARDIN) {
            g.setColor(new Color(190, 225, 245));
            g.fillRect(900, 120, 220, 260);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(10));
            g.drawRect(900, 120, 220, 260);
            g.drawLine(1010, 120, 1010, 380);
        }
        // Légende, comme sur une photo de chantier annotée.
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, h - 90, l, 90);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        g.drawString(legende, 32, h - 34);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.drawString("Photo de démonstration", l - 290, h - 38);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
