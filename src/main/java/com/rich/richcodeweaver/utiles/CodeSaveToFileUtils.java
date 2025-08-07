package com.rich.richcodeweaver.utiles;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.dto.ai.HtmlCodeResult;
import com.rich.richcodeweaver.model.dto.ai.MultiFileCodeResult;
import com.rich.richcodeweaver.model.enums.CodeGeneratorTypeEnum;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件存储至本地工具类
 * 
 * @author DuRuiChi
 * @create 2025/8/5
 **/
public class CodeSaveToFileUtils {

    /**
     * 存储路径
     */
    public static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + File.separator + "appCode";

    /**
     * 保存HTML代码结果
     * @param htmlCodeResult HTML代码结果对象（必须非空）
     * @return 生成的目录File对象
     * @throws IllegalArgumentException 如果输入参数为空
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult)  {
        if (htmlCodeResult == null) {
            throw new IllegalArgumentException("HTML代码结果不能为空");
        }
        // 生成唯一目录
        String baseDirPath = buildUniqueDir(CodeGeneratorTypeEnum.HTML.getValue());
        // 写入文件
        writeSingleToFile(baseDirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(baseDirPath);
    }

    /**
     * 保存多文件网页代码
     *
     * @param result
     * @return
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result)  {
        String baseDirPath = buildUniqueDir(CodeGeneratorTypeEnum.MULTI_FILE.getValue());
        writeSingleToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeSingleToFile(baseDirPath, "style.css", result.getCssCode());
        writeSingleToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 生成唯一目录路径并创建目录
     * @param bizType 代码类型标识（用于目录名前缀）
     * @return 完整目录路径
     */
    private static String buildUniqueDir(String bizType) {
        if (StrUtil.isBlank(bizType)) {
            throw new IllegalArgumentException("业务类型不能为空或空白");
        }
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 将内容写入指定文件
     * @param content 要写入的内容
     */
    private static void writeSingleToFile(String dirPath, String filename, String content)  {
        File targetFile = new File(dirPath, filename);
        try {
            FileUtil.writeString(content, targetFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"文件写入失败：" + filename);
        }
    }
}
