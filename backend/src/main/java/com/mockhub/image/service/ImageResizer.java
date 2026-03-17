package com.mockhub.image.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

@Component
public class ImageResizer {

    private static final int THUMBNAIL_WIDTH = 200;

    public InputStream resize(InputStream inputStream, int targetWidth) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new IOException("Could not read image from input stream");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        if (originalWidth <= targetWidth) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(originalImage, "png", outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }

        double ratio = (double) targetWidth / originalWidth;
        int targetHeight = (int) (originalHeight * ratio);

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "png", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public InputStream createThumbnail(InputStream inputStream) throws IOException {
        return resize(inputStream, THUMBNAIL_WIDTH);
    }
}
