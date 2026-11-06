package com.rich.file.job.cycle;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.rich.common.constant.AppConstant.APP_SCREENSHOT_DIR;

/**
 * 项目启动时清空缓存文件
 *
 * @author DuRuiChi
 * @create 2026/1/3
 **/
@Component
@Slf4j
public class ClearCacheFilesAtStartup implements CommandLineRunner {

    /**
     * 项目启动时清空缓存文件
     *
     * @param args
     * @return void
     * @author DuRuiChi
     * @create 2026/1/3
     **/
    @Override
    public void run(String... args) {
        File localFile = new File(APP_SCREENSHOT_DIR + File.separator);
        if (localFile.exists()) {
            // 只清理截图目录，避免删除 sysCache 下其他运行时文件
            FileUtil.del(localFile);
            log.info("项目启动时清空缓存文件：{}", localFile.getAbsolutePath());
        }
    }
}
