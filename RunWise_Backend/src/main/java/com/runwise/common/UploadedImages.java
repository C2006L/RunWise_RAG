package com.runwise.common;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Decode before saving, discard metadata, and bind new URLs to their owner. */
public final class UploadedImages {
    private UploadedImages() {}
    public static final class Image {
        public final byte[] bytes;
        public final String extension;
        Image(byte[] bytes, String extension) { this.bytes=bytes; this.extension=extension; }
    }
    public static Image sanitize(byte[] data) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) invalid();
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Arrays.asList("png", "jpeg", "jpg", "webp").contains(format)) invalid();
                reader.setInput(input);
                long width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width * height > 20000000) invalid();
                BufferedImage decoded = reader.read(0);
                if (decoded == null) invalid();
                String outputFormat = format.equals("png") || decoded.getColorModel().hasAlpha() ? "png" : "jpeg";
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(decoded, outputFormat, output)) invalid();
                if (output.size() > 5 * 1024 * 1024) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "图片处理后超过5MB，请压缩后上传");
                }
                return new Image(output.toByteArray(), outputFormat.equals("png") ? ".png" : ".jpg");
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无法读取图片，请上传有效的JPG、PNG或WebP图片");
        }
    }
    private static void invalid() { throw new BusinessException(ResultCode.PARAM_ERROR, "图片格式无效或像素过大"); }

    public static void requireOwned(String url, String previous, Long user, String uploadDir) {
        if (url == null || url.trim().isEmpty()) return;
        // Preserve already saved legacy photos only on the same owned record.
        if (url.equals(previous)) return;
        String prefix = "/uploads/checkin/" + user + "/";
        if (!url.startsWith(prefix) || !url.substring(prefix.length()).matches("[a-f0-9]{32}\\.(png|jpg)")) invalid();
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = root.resolve(url.substring("/uploads/".length())).normalize();
        try {
            if (!Files.isRegularFile(file) || !file.toRealPath().startsWith(root.toRealPath())) invalid();
        } catch (IOException e) { invalid(); }
    }
}
