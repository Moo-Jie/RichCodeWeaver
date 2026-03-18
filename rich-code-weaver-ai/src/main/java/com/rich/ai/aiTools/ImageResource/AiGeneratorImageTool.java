package com.rich.ai.aiTools.ImageResource;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rich.ai.aiTools.BaseTool;
import com.rich.model.entity.ImageResource;
import com.rich.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片生成工具
 * 支持 AI 通过工具调用的方式根据描述生成设计图片
 *
 * @author DuRuiChi
 * @create 2025/9/11
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
        String type = arguments.getStr("type");
        String description = arguments.getStr("description");
        type = type == null || type.isEmpty() ? "" : "\n[\n" + type + "\n]\n";
        return String.format("[工具调用结束] %s %s\n\n描述信息：\n```\n%s\n```", "成功生成以下类型的图片", type, description);
    }

    /**
     * 根据生成类型、描述生成图片
     *
     * @param description 设计描述，如名称、行业、风格等，尽量详细
     * @return 生成的设计图片列表
     */
    @Tool("根据描述让 AI 生成设计图片（不能指定生成文字）")
    public List<ImageResource> aiGeneratorImage(@P("类型描述，如Logo、图标、架构图、概念视觉图等等") String type, @P("设计描述，含主体、风格、色彩等要素，尽量详细") String description) {
        List<ImageResource> imageList = new ArrayList<>();
        
        // 检查API密钥配置
        if (dashScopeApiKey == null || dashScopeApiKey.isEmpty()) {
            log.error("DashScope API密钥未配置，无法生成图片");
            return imageList;
        }
        
        try {
            log.info("开始生成图片 - 类型: {}, 描述: {}", type, description);
            
            // 构建设计提示词
            String imagePrompt = String.format("""
                    为我的网站生成 %s：
                    要求 - 符合介绍的风格;禁止包含任何文字
                    介绍 - %s
                    """, type, description);
            
            log.info("生成提示词: {}", imagePrompt);
            
            // 根据类型选择合适的尺寸（Logo使用小尺寸以加快生成速度）
            String size = type.contains("Logo") || type.contains("图标") ? "512*512" : "1024*1024";
            log.info("使用图片尺寸: {}", size);

            // 步骤1: 创建异步任务
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", IMAGE_MODEL);
            
            JSONObject input = new JSONObject();
            input.set("prompt", imagePrompt);
            requestBody.set("input", input);
            
            JSONObject parameters = new JSONObject();
            parameters.set("size", size);
            parameters.set("n", 1);  // 只生成1张图片
            requestBody.set("parameters", parameters);
            
            log.info("发送图片生成请求...");
            HttpResponse createResponse = HttpRequest.post(IMAGE_SYNTHESIS_URL)
                    .header("Authorization", "Bearer " + dashScopeApiKey)
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-Async", "enable")  // 异步调用必须设置
                    .body(requestBody.toString())
                    .timeout(30000)
                    .execute();
            
            if (!createResponse.isOk()) {
                log.error("创建图片生成任务失败，状态码: {}, 响应: {}", createResponse.getStatus(), createResponse.body());
                return imageList;
            }
            
            JSONObject createResult = JSONUtil.parseObj(createResponse.body());
            
            // 检查是否有错误
            if (createResult.containsKey("code")) {
                log.error("创建任务失败: code={}, message={}", createResult.getStr("code"), createResult.getStr("message"));
                return imageList;
            }
            
            String taskId = createResult.getByPath("output.task_id", String.class);
            if (taskId == null || taskId.isEmpty()) {
                log.error("未获取到任务ID，响应: {}", createResponse.body());
                return imageList;
            }
            
            log.info("任务创建成功，任务ID: {}", taskId);
            
            // 步骤2: 轮询查询任务结果（最多等待60秒）
            int maxAttempts = 12;  // 最多查询12次
            int attemptInterval = 5000;  // 每次间隔5秒
            
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                log.info("第 {}/{} 次查询任务状态...", attempt, maxAttempts);
                
                // 等待一段时间再查询
                if (attempt > 1) {
                    Thread.sleep(attemptInterval);
                }
                
                HttpResponse queryResponse = HttpRequest.get(TASK_QUERY_URL + taskId)
                        .header("Authorization", "Bearer " + dashScopeApiKey)
                        .timeout(15000)
                        .execute();
                
                if (!queryResponse.isOk()) {
                    log.warn("查询任务状态失败，状态码: {}", queryResponse.getStatus());
                    continue;
                }
                
                JSONObject queryResult = JSONUtil.parseObj(queryResponse.body());
                String taskStatus = queryResult.getByPath("output.task_status", String.class);
                
                log.info("任务状态: {}", taskStatus);
                
                if ("SUCCEEDED".equals(taskStatus)) {
                    // 任务成功，提取图片URL
                    JSONArray results = queryResult.getByPath("output.results", JSONArray.class);
                    if (results != null && !results.isEmpty()) {
                        for (int i = 0; i < results.size(); i++) {
                            JSONObject resultItem = results.getJSONObject(i);
                            String imageUrl = resultItem.getStr("url");
                            
                            if (StrUtil.isNotBlank(imageUrl)) {
                                log.info("成功获取图片URL: {}", imageUrl);
                                imageList.add(ImageResource.builder()
                                        .category(ImageCategoryEnum.AI)
                                        .description(description)
                                        .url(imageUrl)
                                        .build());
                            }
                        }
                    }
                    break;
                } else if ("FAILED".equals(taskStatus)) {
                    log.error("任务执行失败: {}", queryResponse.body());
                    break;
                } else if ("PENDING".equals(taskStatus) || "RUNNING".equals(taskStatus)) {
                    // 继续等待
                    continue;
                } else {
                    log.warn("未知任务状态: {}", taskStatus);
                    break;
                }
            }
            
            log.info("图片生成完成，共生成 {} 张图片", imageList.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("图片生成被中断: {}", e.getMessage());
        } catch (Exception e) {
            log.error("图片生成失败: {}", e.getMessage(), e);
        }
        return imageList;
    }
}
