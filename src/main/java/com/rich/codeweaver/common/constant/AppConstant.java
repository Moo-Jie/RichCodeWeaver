package com.rich.codeweaver.common.constant;

/**
 * 产物相关常量
 * 定义AI产物模块相关的常量配置
 *
 * @author DuRuiChi
 * @since 2026-03-10
 **/
public interface AppConstant {
    /**
     * 默认封面
     */
    String APP_COVER = "https://rich-tams.oss-cn-beijing.aliyuncs.com/logo.png";

    /**
     * 星标产物的优先级
     */
    Integer STAR_APP_PRIORITY = 99;

    /**
     * 默认产物优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 产物生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/appCode/code_output";

    /**
     * 产物部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/appCode/code_deploy";

    /**
     * 产物截图目录
     */
    String APP_SCREENSHOT_DIR = System.getProperty("user.dir") + "/sysCache/screenshots";

    /**
     * 产物部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";
}