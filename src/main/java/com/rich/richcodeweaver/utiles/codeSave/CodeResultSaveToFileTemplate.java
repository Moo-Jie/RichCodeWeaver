package com.rich.richcodeweaver.utiles.codeSave;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码封装类保存至文件模板类
 *
 * @author DuRuiChi
 * @create 2025/8/7
 **/
public abstract class CodeResultSaveToFileTemplate<T> {
    /**
     * 存储路径
     */
    public static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + File.separator + "appCode";

    /**
     * 保存代码封装类至文件
     *
     * @param result 代码封装类对象
     * @return 生成的目录File对象
     * @throws IllegalArgumentException 如果输入参数为空
     */
    public final File saveCodeResult(T result) {
        // 校验参数
        if (result == null) {
            throw new IllegalArgumentException("代码封装参数不能为空。");
        }
        // 生成唯一目录
        String baseDirPath = buildUniqueDir(getCodeGeneratorTypeEnumValue());
        // 保存文件
        doSaveCodeResult(result, baseDirPath);
        // 返沪文件
        return new File(baseDirPath);
    }

    /**
     * 具体的保存逻辑，由子类实现
     *
     * @param result      代码封装类对象
     * @param baseDirPath 基础目录路径
     */
    protected abstract void doSaveCodeResult(T result, String baseDirPath);

    /**
     * 获取枚举值区分不同的保存策略，由子类实现
     *
     * @return 枚举值
     */
    protected abstract String getCodeGeneratorTypeEnumValue();

    /**
     * 生成唯一目录路径并创建目录
     *
     * @param bizType 代码类型标识（用于目录名前缀）
     * @return 完整目录路径
     */
    protected final String buildUniqueDir(String bizType) {
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
     *
     * @param content 要写入的内容
     */
    protected final void writeSingleToFile(String dirPath, String filename, String content) {
        File targetFile = new File(dirPath, filename);
        try {
            FileUtil.writeString(content, targetFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件写入失败：" + filename);
        }
    }
}
