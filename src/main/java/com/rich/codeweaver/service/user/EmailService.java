package com.rich.codeweaver.service.user;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.rich.codeweaver.common.exception.BusinessException;
import com.rich.codeweaver.common.exception.ErrorCode;
import com.rich.codeweaver.common.utils.EmailUtils;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮件验证码服务
 * 负责生成、发送、校验邮箱验证码
 *
 * @author DuRuiChi
 */
@Slf4j
@Service
public class EmailService {

    /**
     * 验证码 Redis key 前缀
     */
    private static final String EMAIL_CODE_PREFIX = "email:code:";

    /**
     * 发送频率限制 Redis key 前缀
     */
    private static final String EMAIL_RATE_PREFIX = "email:rate:";

    /**
     * 验证码有效期（分钟）
     */
    private static final long CODE_EXPIRE_MINUTES = 5;

    /**
     * 发送间隔限制（秒）
     */
    private static final long SEND_INTERVAL_SECONDS = 60;

    /**
     * 验证码长度
     */
    private static final int CODE_LENGTH = 6;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${email-code.from}")
    private String from;

    /**
     * 发送邮箱验证码
     *
     * @param email 目标邮箱
     */
    public void sendVerificationCode(String email) {
        String normalizedEmail = EmailUtils.normalizeEmail(email);
        if (StrUtil.isBlank(normalizedEmail) || !EmailUtils.isValidEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }

        String rateKey = EMAIL_RATE_PREFIX + normalizedEmail;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(rateKey))) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码发送过于频繁，请60秒后重试");
        }

        String code = RandomUtil.randomNumbers(CODE_LENGTH);

        String codeKey = EMAIL_CODE_PREFIX + normalizedEmail;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        stringRedisTemplate.opsForValue().set(rateKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        try {
            sendCodeEmail(normalizedEmail, code);
            log.info("邮箱验证码已发送: email={}", normalizedEmail);
        } catch (Exception e) {
            stringRedisTemplate.delete(codeKey);
            stringRedisTemplate.delete(rateKey);
            log.error("邮箱验证码发送失败: email={}", normalizedEmail, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    /**
     * 校验邮箱验证码
     *
     * @param email 邮箱
     * @param code  用户输入的验证码
     */
    public void verifyCode(String email, String code) {
        String normalizedEmail = EmailUtils.normalizeEmail(email);
        if (StrUtil.isBlank(normalizedEmail) || StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱或验证码不能为空");
        }

        String codeKey = EMAIL_CODE_PREFIX + normalizedEmail;
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);

        if (StrUtil.isBlank(savedCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请重新获取");
        }

        if (!savedCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }

        // 验证成功后删除验证码，防止重复使用
        stringRedisTemplate.delete(codeKey);
    }

    /**
     * 构建并发送 HTML 验证码邮件
     */
    private void sendCodeEmail(String toEmail, String code) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(toEmail);
        helper.setSubject("【RichCodeWeaver】邮箱验证码");
        helper.setText(buildEmailContent(code), true);

        mailSender.send(message);
    }

    /**
     * 构建验证码邮件 HTML 内容
     */
    private String buildEmailContent(String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif;">
                <div style="max-width:480px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                  <div style="background:#1a1a1a;padding:28px 32px;text-align:center;">
                    <h2 style="color:#fff;margin:0;font-size:20px;letter-spacing:1px;">RichCodeWeaver</h2>
                  </div>
                  <div style="padding:36px 32px;">
                    <p style="color:#333;font-size:15px;margin:0 0 20px;">您好，您正在注册 RichCodeWeaver 账号，以下是您的邮箱验证码：</p>
                    <div style="text-align:center;margin:28px 0;">
                      <span style="display:inline-block;font-size:36px;font-weight:700;letter-spacing:8px;color:#1a1a1a;background:#f5f5f5;padding:16px 36px;border-radius:10px;border:1px solid #e5e5e5;">%s</span>
                    </div>
                    <p style="color:#999;font-size:13px;margin:0;line-height:1.6;">验证码 <strong>%d 分钟</strong>内有效，请勿泄露给他人。</p>
                    <p style="color:#999;font-size:13px;margin:8px 0 0;">如非本人操作，请忽略此邮件。</p>
                  </div>
                  <div style="padding:16px 32px;background:#fafafa;text-align:center;border-top:1px solid #f0f0f0;">
                    <p style="color:#bbb;font-size:12px;margin:0;">RichCodeWeaver — AI 智能产物生成平台</p>
                  </div>
                </div>
                </body>
                </html>
                """.formatted(code, CODE_EXPIRE_MINUTES);
    }
}
