package com.rich.ai.aiTools.ImageResource;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.aiTools.BaseTool;
import com.rich.file.service.FileService;
import com.rich.model.entity.ImageResource;
import com.rich.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片生成工具
 * 支持 AI 通过工具调用的方式根据描述生成设计图片
 *
 * @author DuRuiChi
 * @create 2026/1/11
 **/
@Slf4j
@Component
public class AiGeneratorImageTool extends BaseTool {

    // DashScope API基础URL
    private static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1";

    // 图片生成任务创建接口
    private static final String IMAGE_SYNTHESIS_URL = DASHSCOPE_BASE_URL + "/services/aigc/text2image/image-synthesis";

    // 任务查询接口
    private static final String TASK_QUERY_URL = DASHSCOPE_BASE_URL + "/tasks/";

    /**
     * DashScope API密钥，从配置文件中注入
     */
    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    /**
     * 文件服务，用于上传图片到阿里云OSS
     */
    @Autowired
    private FileService fileService;

    /**
     * 本地缓存目录路径
     */
    private static final String CACHE_DIR = "cache";

    /**
     * 图像生成模型名称，使用wan2.2-t2i-flash极速版
     */
    private static final String IMAGE_MODEL = "wan2.2-t2i-flash";

    @Override
    public String getToolName() {
        return "aiGeneratorImage";
    }

    @Override
    public String getToolDisplayName() {
        return "AI图片生成工具";
    }

    @Override
    public String getResultMsg(JSONObject arguments) {
        // 从参数中提取图片类型和描述信息
        String type = arguments.getStr("type");
        String description = arguments.getStr("description");

        // 格式化类型显示文本
        String displayType = (type == null || type.trim().isEmpty())
                ? ""
                : "\n[\n" + type + "\n]\n";

        // 格式化描述显示文本，避免null值
        String displayDescription = (description == null) ? "无" : description;

        return String.format("[工具调用结束] %s %s\n\n描述信息：\n```\n%s\n```",
                "成功生成以下类型的图片", displayType, displayDescription);
    }

    /**
     * 根据生成类型、描述生成图片
     * 使用阿里云DashScope的万相文生图API进行AI图片生成
     *
     * @param type        图片类型描述，如Logo、图标、架构图、概念视觉图等
     * @param description 设计描述，含主体、风格、色彩等要素，尽量详细
     * @return 生成的设计图片列表（失败时返回空列表）
     */
    @Tool("根据描述让 AI 生成设计图片（不能指定生成文字）")
    public List<ImageResource> aiGeneratorImage(
            @P("类型描述，如Logo、图标、架构图、概念视觉图等等") String type,
            @P("设计描述，含主体、风格、色彩等要素，尽量详细") String description) {

        // 初始化图片列表（用于存储生成结果）
        List<ImageResource> imageList = new ArrayList<>();

        // 参数校验：检查图片类型是否为空
        if (type == null || type.trim().isEmpty()) {
            log.warn("图片类型不能为空");
            return imageList;
        }

        // 参数校验：检查描述信息是否为空
        if (description == null || description.trim().isEmpty()) {
            log.warn("描述信息不能为空");
            return imageList;
        }

        // 配置校验：检查DashScope API密钥是否已配置
        if (dashScopeApiKey == null || dashScopeApiKey.trim().isEmpty()) {
            log.error("DashScope API密钥未配置，无法生成图片");
            return imageList;
        }

        try {
            log.info("开始生成图片 - 类型: {}, 描述: {}", type, description);

            // 构建AI图片生成提示词（包含类型、要求和详细描述）
            String imagePrompt = String.format("""
                    为我的网站生成 %s：
                    要求 - 符合介绍的风格;禁止包含任何文字
                    介绍 - %s
                    """, type, description);

            log.info("生成提示词: {}", imagePrompt);

            // 根据图片类型选择合适的尺寸（Logo和图标使用小尺寸以加快生成速度）
            String imageSize = (type.contains("Logo") || type.contains("图标"))
                    ? "512*512"
                    : "1024*1024";
            log.info("使用图片尺寸: {}", imageSize);

            // 步骤1: 创建异步图片生成任务
            // 构建请求体JSON对象
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", IMAGE_MODEL);  // 设置使用的AI模型

            // 构建输入参数（包含提示词）
            JSONObject input = new JSONObject();
            input.set("prompt", imagePrompt);
            requestBody.set("input", input);

            // 构建生成参数（包含尺寸和数量）
            JSONObject parameters = new JSONObject();
            parameters.set("size", imageSize);  // 设置图片尺寸
            parameters.set("n", 1);  // 只生成1张图片
            requestBody.set("parameters", parameters);

            log.info("发送图片生成请求...");

            // 发送POST请求创建异步任务
            HttpResponse createResponse = HttpRequest.post(IMAGE_SYNTHESIS_URL)
                    .header("Authorization", "Bearer " + dashScopeApiKey)  // API认证
                    .header("Content-Type", "application/json")  // 请求内容类型
                    .header("X-DashScope-Async", "enable")  // 启用异步模式（必须设置）
                    .body(requestBody.toString())
                    .timeout(30000)  // 设置30秒超时
                    .execute();

            // 检查HTTP响应状态码
            if (!createResponse.isOk()) {
                log.error("创建图片生成任务失败，状态码: {}, 响应: {}",
                        createResponse.getStatus(), createResponse.body());
                return imageList;
            }

            // 解析响应体
            String responseBody = createResponse.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.error("创建任务响应体为空");
                return imageList;
            }

            JSONObject createResult = JSONUtil.parseObj(responseBody);

            // 检查API是否返回错误码
            if (createResult.containsKey("code")) {
                String errorCode = createResult.getStr("code");
                String errorMessage = createResult.getStr("message");
                log.error("创建任务失败: code={}, message={}", errorCode, errorMessage);
                return imageList;
            }

            // 提取任务ID
            String taskId = createResult.getByPath("output.task_id", String.class);
            if (taskId == null || taskId.trim().isEmpty()) {
                log.error("未获取到任务ID，响应: {}", responseBody);
                return imageList;
            }

            log.info("任务创建成功，任务ID: {}", taskId);

            // 步骤2: 轮询查询任务结果（最多等待60秒）
            int maxAttempts = 12;  // 最多查询12次
            int attemptInterval = 5000;  // 每次间隔5秒（5000毫秒）

            // 轮询查询任务状态直到完成或超时
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                log.info("第 {}/{} 次查询任务状态...", attempt, maxAttempts);

                // 第一次查询前不等待，后续查询前等待指定间隔
                if (attempt > 1) {
                    Thread.sleep(attemptInterval);
                }

                // 发送GET请求查询任务状态
                HttpResponse queryResponse = HttpRequest.get(TASK_QUERY_URL + taskId)
                        .header("Authorization", "Bearer " + dashScopeApiKey)  // API认证
                        .timeout(15000)  // 设置15秒超时
                        .execute();

                // 检查HTTP响应状态
                if (!queryResponse.isOk()) {
                    log.warn("查询任务状态失败，状态码: {}, 将重试", queryResponse.getStatus());
                    continue;  // 继续下一次查询
                }

                // 解析查询结果
                String queryBody = queryResponse.body();
                if (queryBody == null || queryBody.trim().isEmpty()) {
                    log.warn("查询任务响应体为空，将重试");
                    continue;
                }

                JSONObject queryResult = JSONUtil.parseObj(queryBody);
                String taskStatus = queryResult.getByPath("output.task_status", String.class);

                log.info("任务状态: {}", taskStatus);

                // 根据任务状态进行不同处理
                if ("SUCCEEDED".equals(taskStatus)) {
                    // 任务成功完成，提取生成的图片URL
                    JSONArray results = queryResult.getByPath("output.results", JSONArray.class);

                    if (results != null && !results.isEmpty()) {
                        // 遍历所有生成的图片结果
                        for (int i = 0; i < results.size(); i++) {
                            JSONObject resultItem = results.getJSONObject(i);
                            String imageUrl = resultItem.getStr("url");

                            // 校验图片URL是否有效
                            if (StrUtil.isNotBlank(imageUrl)) {
                                log.info("成功获取图片URL: {}", imageUrl);

                                // 下载图片到本地缓存并上传到OSS
                                String ossUrl = downloadAndUploadToOSS(imageUrl);

                                // 使用OSS URL或原始URL（如果上传失败）
                                String finalUrl = StrUtil.isNotBlank(ossUrl) ? ossUrl : imageUrl;

                                // 构建图片资源对象并添加到列表
                                ImageResource imageResource = ImageResource.builder()
                                        .category(ImageCategoryEnum.AI)  // 设置分类为AI生成图片
                                        .description(description)  // 使用原始描述作为图片描述
                                        .url(finalUrl)  // 使用OSS URL或原始URL
                                        .build();
                                imageList.add(imageResource);
                            }
                        }
                    }
                    break;  // 任务完成，退出轮询

                } else if ("FAILED".equals(taskStatus)) {
                    // 任务执行失败
                    log.error("任务执行失败: {}", queryBody);
                    break;  // 退出轮询

                } else if ("PENDING".equals(taskStatus) || "RUNNING".equals(taskStatus)) {
                    // 任务还在进行中，继续等待
                    log.debug("任务进行中，状态: {}", taskStatus);
                    continue;  // 继续下一次查询

                } else {
                    // 未知的任务状态
                    log.warn("未知任务状态: {}, 响应: {}", taskStatus, queryBody);
                    break;  // 退出轮询
                }
            }

            log.info("图片生成完成，共生成 {} 张图片", imageList.size());

        } catch (InterruptedException e) {
            // 捕获线程中断异常（在等待任务完成时可能发生）
            Thread.currentThread().interrupt();  // 恢复中断状态
            log.error("图片生成被中断: {}", e.getMessage());
        } catch (Exception e) {
            // 捕获所有其他异常并记录详细错误信息
            log.error("图片生成失败，类型: {}, 描述: {}, 错误: {}", type, description, e.getMessage(), e);
        }

        // 返回生成的图片列表（可能为空列表）
        return imageList;
    }

    /**
     * 下载图片到本地缓存并上传到阿里云OSS
     *
     * @param imageUrl 图片URL
     * @return OSS上传后的URL，失败返回null
     */
    private String downloadAndUploadToOSS(String imageUrl) {
        File cacheFile = null;
        try {
            // 1. 创建缓存目录（如果不存在）
            Path cachePath = Paths.get(CACHE_DIR);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
                log.info("创建缓存目录: {}", cachePath.toAbsolutePath());
            }

            // 2. 生成唯一的文件名
            String fileName = UUID.randomUUID().toString().replaceAll("-", "") + ".png";
            String cacheFilePath = CACHE_DIR + File.separator + fileName;

            log.info("开始下载图片到本地缓存: {}", cacheFilePath);

            // 3. 下载图片到本地缓存
            HttpResponse response = HttpRequest.get(imageUrl)
                    .timeout(30000)  // 30秒超时
                    .execute();

            if (!response.isOk()) {
                log.error("下载图片失败，状态码: {}", response.getStatus());
                return null;
            }

            // 将图片字节写入本地文件
            byte[] imageBytes = response.bodyBytes();
            cacheFile = new File(cacheFilePath);
            FileUtil.writeBytes(imageBytes, cacheFile);
            log.info("图片已缓存到本地: {}, 大小: {} bytes", cacheFilePath, imageBytes.length);

            // 4. 将缓存的文件上传到OSS
            log.info("开始上传图片到阿里云OSS...");

            // 将File转换为MultipartFile
            try (FileInputStream input = new FileInputStream(cacheFile)) {
                MultipartFile multipartFile = new MockMultipartFile(
                        "file",
                        fileName,
                        "image/png",
                        input
                );

                // 调用文件服务上传到OSS
                String ossUrl = fileService.upload(multipartFile);

                if (StrUtil.isNotBlank(ossUrl)) {
                    log.info("图片上传OSS成功: {}", ossUrl);
                    return ossUrl;
                } else {
                    log.error("图片上传OSS失败，返回URL为空");
                    return null;
                }
            }

        } catch (IOException e) {
            log.error("下载或上传图片时发生IO异常: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("处理图片时发生异常: {}", e.getMessage(), e);
            return null;
        } finally {
            // 5. 清理本地缓存文件
            if (cacheFile != null && cacheFile.exists()) {
                boolean deleted = cacheFile.delete();
                if (deleted) {
                    log.info("已清理本地缓存文件: {}", cacheFile.getAbsolutePath());
                } else {
                    log.warn("清理本地缓存文件失败: {}", cacheFile.getAbsolutePath());
                }
            }
        }
    }
}
