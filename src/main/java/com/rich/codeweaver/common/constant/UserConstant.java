package com.rich.codeweaver.common.constant;

/**
 * 用户相关常量
 * 定义用户模块相关的常量配置
 *
 * @author DuRuiChi
 * @since 2026-03-08
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

    String DEFAULT_ROLE = "user";
    String ADMIN_ROLE = "admin";
    String PASSWORD_SALT = "rich";
    String USER_IDENTITY_INDIVIDUAL = "individual";

    /**
     * 邮箱正则
     */
    String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /**
     * 最小用户昵称长度
     */
    int MIN_USERNAME_LENGTH = 1;

    /**
     * 最大用户昵称长度
     */
    int MAX_USERNAME_LENGTH = 20;
}