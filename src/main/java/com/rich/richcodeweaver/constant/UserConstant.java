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
    String DEFAULT_USER_NAME = "无名";
    /**
     * 默认头像
     */
    String APP_AVATAR = "https://rich-tams.oss-cn-beijing.aliyuncs.com/logo.png";

    /**
     * 默认用户名
     */
    String DEFAULT_PROFILE = "这个用户很懒，什么都没写。";

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";
    
    // endregion
}