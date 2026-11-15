package com.runwise.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runwise.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

/** Authenticated, transient recognition. No URL fetching and no database writes. */
@RestController
@RequestMapping("/api/files")
public class ImageRecognitionController {
    private final ObjectMapper json;
    @Value("${runwise.rag.base-url:http://localhost:5000}") private String baseUrl;
    public ImageRecognitionController(ObjectMapper json) { this.json = json; }

    @PostMapping("/recognize-checkin")
    public Result<Map<String,Object>> recognize(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5L * 1024 * 1024)
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择5MB以内的运动截图");
        if (!Arrays.asList("image/jpeg", "image/png", "image/webp").contains(file.getContentType()))
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持JPG、PNG或WebP截图");
        try {
            Map<String,String> input = Collections.singletonMap("imageBase64", Base64.getEncoder().encodeToString(file.getBytes()));
            try (HttpResponse response = HttpRequest.post(baseUrl + "/api/rag/image-metrics")
                    .header("Content-Type", "application/json").body(json.writeValueAsString(input)).timeout(85000).execute()) {
                if (response.getStatus() == 400)
                    throw new BusinessException(ResultCode.PARAM_ERROR, "无法读取此图片，请更换清晰的运动截图");
                if (!response.isOk()) throw ModelServiceErrors.fromResponse(response.body());
                return Result.success(json.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {}));
            }
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(ResultCode.RAG_SERVICE_ERROR, "图片识别服务连接失败或超时，请稍后重试；仍可手动填写打卡。"); }
    }
}
