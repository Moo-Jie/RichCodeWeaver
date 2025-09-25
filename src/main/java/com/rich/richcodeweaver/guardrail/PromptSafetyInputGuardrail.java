package com.rich.richcodeweaver.guardrail;

import com.rich.richcodeweaver.constant.SensitiveWordsConstant;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 用户输入提示词护轨规则类
 * 实现全面的内容安全检测，包括恶意注入、敏感内容、政治敏感等多维度检测
 *
 * @author DuRuiChi
 * @create 2025/9/24
 * @update 2025/1/25 - 重构并扩充敏感词汇库
 **/
@Slf4j
public class PromptSafetyInputGuardrail implements InputGuardrail {

    // 输入长度限制
    private static final int MAX_INPUT_LENGTH = 2000;
    private static final int MIN_INPUT_LENGTH = 1;

    /**
     * 提示护轨验证
     *
     * @param userMessage 用户输入的消息对象
     * @return 验证结果，成功则返回成功状态，否则返回失败状态
     */
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        try {
            String input = userMessage.singleText();
            log.debug("开始验证用户输入，输入长度: {}", input.length());

            // 1. 基础验证
            InputGuardrailResult basicValidation = validateBasicRules(input);
            if (!basicValidation.isSuccess()) {
                return basicValidation;
            }

            // 2. 敏感词检测
            InputGuardrailResult sensitiveWordValidation = validateSensitiveWords(input);
            if (!sensitiveWordValidation.isSuccess()) {
                return sensitiveWordValidation;
            }

            // 3. 注入攻击检测
            InputGuardrailResult injectionValidation = validateInjectionAttacks(input);
            if (!injectionValidation.isSuccess()) {
                return injectionValidation;
            }

            // 4. 内容合规性检测
            InputGuardrailResult complianceValidation = validateCompliance(input);
            if (!complianceValidation.isSuccess()) {
                return complianceValidation;
            }

            // 5. 高级威胁检测
            InputGuardrailResult advancedThreatValidation = validateAdvancedThreats(input);
            if (!advancedThreatValidation.isSuccess()) {
                return advancedThreatValidation;
            }

            log.debug("用户输入验证通过");
            return success();

        } catch (Exception e) {
            log.error("护轨验证过程中发生异常", e);
            return fatal("输入验证失败，请稍后重试");
        }
    }

    /**
     * 基础规则验证
     * 验证输入是否符合基本规则，包括非空、长度限制等
     *
     * @param input 用户输入的文本
     * @return 验证结果，成功则返回成功状态，否则返回失败状态
     */
    private InputGuardrailResult validateBasicRules(String input) {
        if (input == null) {
            log.warn("输入内容为null");
            return fatal("输入内容不能为空");
        }

        if (input.trim().isEmpty()) {
            log.warn("输入内容为空字符串");
            return fatal("输入内容不能为空");
        }

        if (input.length() < MIN_INPUT_LENGTH) {
            log.warn("输入内容过短: {}", input.length());
            return fatal("输入内容过短，请提供更详细的描述");
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            log.warn("输入内容过长: {}", input.length());
            return fatal(String.format("输入内容过长，不要超过 %d 字符", MAX_INPUT_LENGTH));
        }

        return success();
    }

    /**
     * 敏感词检测 - 使用常量类中的敏感词汇
     * 验证输入是否包含敏感词汇，使用常量类中的敏感词汇
     *
     * @param input 用户输入的文本
     * @return 验证结果，成功则返回成功状态，否则返回失败状态
     */
    private InputGuardrailResult validateSensitiveWords(String input) {
        String lowerInput = input.toLowerCase();

        // 检测提示词注入敏感词
        for (String word : SensitiveWordsConstant.PROMPT_INJECTION_WORDS) {
            if (lowerInput.contains(word.toLowerCase())) {
                log.warn("检测到提示词注入敏感词: {}", word);
                return fatal("输入包含不当内容，请修改后重试");
            }
        }

        // 检测政治敏感词
        for (String word : SensitiveWordsConstant.POLITICAL_SENSITIVE_WORDS) {
            if (lowerInput.contains(word.toLowerCase())) {
                log.warn("检测到政治敏感词: {}", word);
                return fatal("输入包含敏感内容，请避免涉及政治相关话题");
            }
        }

        // 检测辱骂词汇
        for (String word : SensitiveWordsConstant.ABUSIVE_WORDS) {
            if (lowerInput.contains(word.toLowerCase())) {
                log.warn("检测到辱骂词汇: {}", word);
                return fatal("请使用文明用语，避免辱骂和仇恨言论");
            }
        }

        // 检测色情内容
        for (String word : SensitiveWordsConstant.SEXUAL_CONTENT_WORDS) {
            if (lowerInput.contains(word.toLowerCase())) {
                log.warn("检测到色情内容词汇: {}", word);
                return fatal("请避免涉及成人或色情相关内容");
            }
        }

        // 检测暴力内容
        for (String word : SensitiveWordsConstant.VIOLENCE_WORDS) {
            if (lowerInput.contains(word.toLowerCase())) {
                log.warn("检测到暴力内容词汇: {}", word);
                return fatal("请避免涉及暴力、危险或非法活动相关内容");
            }
        }

        // 检测违法内容
        for (String word : SensitiveWordsConstant.ILLEGAL_CONTENT_WORDS) {
            if (lowerInput.contains(word.toLowerCase())) {
                log.warn("检测到违法内容词汇: {}", word);
                return fatal("请避免涉及违法违规活动相关内容");
            }
        }

        return success();
    }

    /**
     * 注入攻击检测 - 使用常量类中的高级注入模式
     * 验证输入是否包含高级注入攻击模式，使用常量类中的高级注入模式
     *
     * @param input 用户输入的文本
     * @return 验证结果，成功则返回成功状态，否则返回失败状态
     */
    private InputGuardrailResult validateInjectionAttacks(String input) {
        for (Pattern pattern : SensitiveWordsConstant.ADVANCED_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("检测到注入攻击模式: {}", pattern.pattern());
                return fatal("检测到恶意输入模式，请求被拒绝");
            }
        }
        return success();
    }

    /**
     * 内容合规性检查 - 使用常量类中的合规性检查模式
     * 验证输入是否符合平台规范，使用常量类中的合规性检查模式
     *
     * @param input 用户输入的文本
     * @return 验证结果，成功则返回成功状态，否则返回失败状态
     */
    private InputGuardrailResult validateCompliance(String input) {
        for (Pattern pattern : SensitiveWordsConstant.COMPLIANCE_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("检测到不合规内容模式: {}", pattern.pattern());
                return fatal("输入内容不符合平台规范，请修改后重试");
            }
        }
        return success();
    }

    /**
     * 高级威胁检测
     * 验证输入是否包含高级威胁，包括异常字符比例、重复字符模式、Base64编码尝试等
     *
     * @param input 用户输入的文本
     * @return 验证结果，成功则返回成功状态，否则返回失败状态
     */
    private InputGuardrailResult validateAdvancedThreats(String input) {
        // 检测异常字符比例
        long specialCharCount = input.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
                .count();
        double specialCharRatio = (double) specialCharCount / input.length();
        
        if (specialCharRatio > 0.3) {
            log.warn("特殊字符比例过高: {}", specialCharRatio);
            return fatal("输入包含过多特殊字符，请使用正常的文本描述");
        }

        // 检测重复字符模式
        if (hasExcessiveRepetition(input)) {
            log.warn("检测到异常重复字符模式");
            return fatal("输入包含异常重复内容，请提供正常的描述");
        }

        // 检测Base64编码尝试
        if (isPotentialBase64Injection(input)) {
            log.warn("检测到潜在的Base64编码注入");
            return fatal("检测到可疑的编码内容，请使用普通文本");
        }

        return success();
    }

    /**
     * 检测是否有过度重复的字符或模式
     * 验证输入是否包含过度重复的字符或模式
     *
     * @param input 用户输入的文本
     * @return 如果存在过度重复的字符或模式，则返回true；否则返回false
     */
    private boolean hasExcessiveRepetition(String input) {
        if (input.length() < 10) return false;
        
        // 检测连续重复字符
        int maxRepeat = 0;
        int currentRepeat = 1;
        char lastChar = input.charAt(0);
        
        for (int i = 1; i < input.length(); i++) {
            if (input.charAt(i) == lastChar) {
                currentRepeat++;
            } else {
                maxRepeat = Math.max(maxRepeat, currentRepeat);
                currentRepeat = 1;
                lastChar = input.charAt(i);
            }
        }
        maxRepeat = Math.max(maxRepeat, currentRepeat);
        
        return maxRepeat > 10; // 超过10个连续相同字符认为异常
    }

    /**
     * 检测是否为潜在的Base64注入
     * 验证输入是否包含潜在的Base64编码注入
     *
     * @param input 用户输入的文本
     * @return 如果存在潜在的Base64编码注入，则返回true；否则返回false
     */
    private boolean isPotentialBase64Injection(String input) {
        // 简单的Base64模式检测
        Pattern base64Pattern = Pattern.compile("^[A-Za-z0-9+/]{20,}={0,2}$");
        String[] words = input.split("\\s+");
        
        for (String word : words) {
            if (word.length() > 20 && base64Pattern.matcher(word).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有敏感词统计信息（用于监控和分析）
     * 现在使用常量类中的统计方法
     *
     * @return 敏感词统计信息，键为敏感词，值为出现次数
     */
    public Map<String, Integer> getSensitiveWordStats() {
        return SensitiveWordsConstant.getSensitiveWordStats();
    }

    /**
     * 获取护rail配置信息
     * 现在使用常量类中的数据
     *
     * @return 护rail配置信息，键为配置项名称，值为配置项值
     */
    public Map<String, Object> getGuardrailConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("max_input_length", MAX_INPUT_LENGTH);
        config.put("min_input_length", MIN_INPUT_LENGTH);
        config.put("total_sensitive_words", SensitiveWordsConstant.getTotalSensitiveWordsCount());
        config.put("total_patterns", SensitiveWordsConstant.getTotalPatternsCount());
        config.put("version", "2.0 - Enhanced with expanded vocabulary");
        config.put("last_updated", "2025-01-25");
        
        // 添加详细的词汇库统计
        Map<String, Integer> wordStats = SensitiveWordsConstant.getSensitiveWordStats();
        config.putAll(wordStats);
        
        return config;
    }

    /**
     * 获取护rail版本信息
     * 现在使用常量类中的版本信息
     *
     * @return 护rail版本信息
     */
    public String getVersion() {
        return "PromptSafetyInputGuardrail v2.0 - Enhanced Edition";
    }

    /**
     * 获取护rail功能描述
     * 现在使用常量类中的功能描述
     *
     * @return 护rail功能描述
     */
    public String getDescription() {
        return "全面的AI输入安全护轨系统，包含" + 
               SensitiveWordsConstant.getTotalSensitiveWordsCount() + "个敏感词汇和" +
               SensitiveWordsConstant.getTotalPatternsCount() + "个检测模式，" +
               "覆盖提示词注入、政治敏感、暴力色情、违法违规等多个维度的安全检测";
    }
}
