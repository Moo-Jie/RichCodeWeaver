package com.rich.richcodeweaver.job.once;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.rich.richcodeweaver.constant.AppConstant.APP_SCREENSHOT_DIR;

/**
 * 定期清空缓存文件
 *
 * @author DuRuiChi
 * @create 2025/9/3
 **/
@Component
@Slf4j
public class RegularlyClearCacheFiles {
    /**
     * 定期清空缓存文件（每十二小时执行一次）
     *
     * @return void
     * @author DuRuiChi
     * @create 2025/9/3
     **/
    @Scheduled(fixedRate = 60 * 1000 * 60 * 12)
    public void run() {
        File localFile = new File(APP_SCREENSHOT_DIR + File.separator);
        if (localFile.exists()) {
            // 获取文件所在目录
            File parentDir = localFile.getParentFile();
            // 删除整个目录
            FileUtil.del(parentDir);
            log.info("定期清空缓存文件：{}", parentDir.getAbsolutePath());
        }
    }
}
