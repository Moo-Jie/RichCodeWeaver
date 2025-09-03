package com.rich.richcodeweaver.job.cycle;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.rich.richcodeweaver.constant.AppConstant.APP_SCREENSHOT_DIR;

/**
 * 项目启动时清空缓存文件
 *
 * @author DuRuiChi
 * @create 2025/9/3
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
     * @create 2025/9/3
     **/
    @Override
    public void run(String... args) throws Exception {
        File localFile = new File(APP_SCREENSHOT_DIR + File.separator);
        if (localFile.exists()) {
            // 获取文件所在目录
            File parentDir = localFile.getParentFile();
            // 删除整个目录
            FileUtil.del(parentDir);
            log.info("项目启动时清空缓存文件：{}", parentDir.getAbsolutePath());
        }
    }
}
