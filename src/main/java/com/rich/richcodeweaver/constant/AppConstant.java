package com.rich.richcodeweaver.constant;

/**
 * 应用相关常量
 *
 * @author DuRuiChi
 * @create 2025/8/4
 **/
public interface AppConstant {

    /**
     * 星标应用的优先级
     */
    Integer STAR_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/appCode/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/appCode/code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";
}