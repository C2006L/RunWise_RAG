package com.runwise.controller;

import com.runwise.common.BusinessException;
import com.runwise.common.Result;
import com.runwise.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(
            Arrays.asList("image/jpeg", "image/png", "image/webp"));

    @Value("${runwise.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/checkin-image")
    public Result<Map<String, String>> uploadCheckinImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片不能超过 5MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持 JPG、PNG 或 WebP 图片");
        }

        String extension = contentType.equals("image/png") ? ".png"
                : contentType.equals("image/webp") ? ".webp" : ".jpg";
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("checkin");
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "非法文件名");
        }

        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片保存失败");
        }
        return Result.success(Collections.singletonMap("url", "/uploads/checkin/" + filename));
    }
}
