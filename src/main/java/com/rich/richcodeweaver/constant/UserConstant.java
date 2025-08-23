package com.rich.richcodeweaver.constant;

/**
 * 用户相关常量
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
public interface UserConstant {
    /**
     * 最小账号长度
     */
    int MIN_ACCOUNT_LENGTH = 4;

    /**
     * 最小密码长度
     */
    int MIN_PASSWORD_LENGTH = 8;

    /**
     * 最大密码长度
     */
    int MAX_PASSWORD_LENGTH = 20;

    /**
     * 默认用户名
     */
    String DEFAULT_USER_NAME = "游客";

    /**
     * 默认用户头像图片
     */
    String DEFAULT_USER_PICTURE = "https://rich-tams.oss-cn-beijing.aliyuncs.com/LOGO.jpg";

    /**
     * 默认用户名
     */
    String DEFAULT_PROFILE = "这个用户很懒，什么都没写。";

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";
}