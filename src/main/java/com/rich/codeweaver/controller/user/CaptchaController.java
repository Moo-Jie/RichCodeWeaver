package com.rich.codeweaver.controller.user;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.rich.codeweaver.common.model.BaseResponse;
import com.rich.codeweaver.common.utils.ResultUtils;
import com.rich.codeweaver.common.exception.BusinessException;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.exception.ThrowUtils;
import com.rich.codeweaver.model.dto.user.SendEmailCodeRequest;
import com.rich.codeweaver.service.user.EmailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码控制器
 * 提供图形计算验证码和邮箱验证码接口
 *
 * @author DuRuiChi
 */
@Slf4j
@RestController
@RequestMapping("/user/captcha")
public class CaptchaController {

    /**
     * 图形验证码 Redis key 前缀
     */
    private static final String CAPTCHA_PREFIX = "captcha:math:";

    /**
     * 图形验证码有效期（分钟）
     */
    private static final long CAPTCHA_EXPIRE_MINUTES = 2;

    /**
     * 图片宽度
     */
    private static final int IMG_WIDTH = 160;

    /**
     * 图片高度
     */
    private static final int IMG_HEIGHT = 40;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EmailService emailService;

    /**
     * 获取数学计算验证码图片
     * 返回 captchaId 和 base64 图片
     */
    @GetMapping("/math")
    public BaseResponse<Map<String, String>> getMathCaptcha() {
        // 1. 生成简单算术题
        Random random = new Random();
        int a = random.nextInt(50) + 1;
        int b = random.nextInt(50) + 1;
        // 随机加法或减法，确保结果非负
        String operator;
        int result;
        if (random.nextBoolean()) {
            operator = "+";
            result = a + b;
        } else {
            // 确保 a >= b，结果非负
            if (a < b) {
                int temp = a;
                a = b;
                b = temp;
            }
            operator = "-";
            result = a - b;
        }
        String expression = a + " " + operator + " " + b + " = ?";

        // 2. 生成 captchaId 并存入 Redis
        String captchaId = IdUtil.simpleUUID();
        stringRedisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaId,
                String.valueOf(result),
                CAPTCHA_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        // 3. 生成验证码图片（base64）
        String base64Image = generateCaptchaImage(expression);

        // 4. 返回结果
        Map<String, String> data = new HashMap<>(2);
        data.put("captchaId", captchaId);
        data.put("captchaImage", "data:image/png;base64," + base64Image);

        return ResultUtils.success(data);
    }

    /**
     * 发送邮箱验证码
     * 需先通过图形验证码校验
     */
    @PostMapping("/email/code")
    public BaseResponse<Boolean> sendEmailCode(@RequestBody SendEmailCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        String email = request.getEmail();
        String captchaId = request.getCaptchaId();
        String captchaAnswer = request.getCaptchaAnswer();

        // 1. 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(email), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(captchaId), ErrorCode.PARAMS_ERROR, "验证码ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(captchaAnswer), ErrorCode.PARAMS_ERROR, "请输入计算结果");

        // 2. 校验图形验证码
        verifyCaptcha(captchaId, captchaAnswer);

        // 3. 发送邮箱验证码
        emailService.sendVerificationCode(email);

        return ResultUtils.success(true);
    }

    /**
     * 校验图形验证码
     */
    private void verifyCaptcha(String captchaId, String answer) {
        String key = CAPTCHA_PREFIX + captchaId;
        String correctAnswer = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isBlank(correctAnswer)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请刷新重试");
        }

        if (!correctAnswer.equals(answer.trim())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "计算结果错误");
        }

        // 验证通过后删除，防止重复使用
        stringRedisTemplate.delete(key);
    }

    /**
     * 生成验证码图片
     * 使用 Java2D 绘制算术表达式，带干扰线和噪点
     *
     * @param expression 算术表达式
     * @return base64 编码的 PNG 图片
     */
    private String generateCaptchaImage(String expression) {
        BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景
        g.setColor(new Color(245, 245, 245));
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

        // 绘制干扰线
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(200 + random.nextInt(40), 200 + random.nextInt(40), 200 + random.nextInt(40)));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT),
                    random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT));
        }

        // 绘制噪点
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(180 + random.nextInt(60), 180 + random.nextInt(60), 180 + random.nextInt(60)));
            g.fillOval(random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT), 2, 2);
        }

        // 绘制文字
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(new Color(60 + random.nextInt(40), 60 + random.nextInt(40), 60 + random.nextInt(40)));

        // 计算文字居中位置
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(expression);
        int x = (IMG_WIDTH - textWidth) / 2;
        int y = (IMG_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;

        // 逐字符绘制，添加微小随机偏移
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            int offsetY = random.nextInt(5) - 2;
            g.drawString(String.valueOf(c), x, y + offsetY);
            x += fm.charWidth(c);
        }

        g.dispose();

        // 转为 base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码生成失败");
        }
    }
}
