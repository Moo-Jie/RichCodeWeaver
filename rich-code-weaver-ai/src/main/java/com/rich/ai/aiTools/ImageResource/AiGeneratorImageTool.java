package com.rich.ai.aiTools.ImageResource;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
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
import java.util.Map;

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

    /**
     * DashScope API密钥，从配置文件中注入
     * 默认值为空字符串
     */
    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;


    /**
     * 图像生成模型名称，从配置文件中注入
     * 默认使用wan2.2-t2i-flash绘图大模型
     */
    @Value("${dashscope.image-model:wan2.2-t2i-flash}")
    private String imageModel;

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
        try {
            // 构建设计提示词
            String imagePrompt = String.format("""
                    为我的网站生成 %s：
                    要求 - 符合介绍的风格;禁止包含任何文字
                    介绍 - %s
                    """, type, description);

            // 配置图像合成参数
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(dashScopeApiKey)      // 设置API密钥
                    .model(imageModel)            // 设置模型名称
                    .prompt(imagePrompt)           // 设置提示词
                    .size("512*512")             // 设置图片尺寸为512x512像素
                    .n(1)                     // 图片数量
                    .build();

            // 创建图像合成实例并调用API
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);

            // 处理API响应结果
            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
                List<Map<String, String>> results = result.getOutput().getResults();
                for (Map<String, String> imageResult : results) {
                    String imageUrl = imageResult.get("url");
                    // 检查URL是否有效
                    if (StrUtil.isNotBlank(imageUrl)) {
                        // 构建图片资源对象
                        imageList.add(ImageResource.builder()
                                .category(ImageCategoryEnum.AI)  // 设置分类为AI
                                .description(description)           // 保留原始描述
                                .url(imageUrl)                     // 设置图片URL
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            // 记录错误日志
            log.error("生成失败: {}", e.getMessage(), e);
        }
        return imageList;
    }
}
