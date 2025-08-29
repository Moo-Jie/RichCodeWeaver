package com.rich.richcodeweaver.utils.aiUtils.codeParse;

/**
 * 代码解析器接口
 * 定义了代码解析的基本方法，用于将原始代码字符串解析为指定类型的代码对象。
 * (基于策略模式，不同的代码类型有不同的解析模式)
 *
 * @param <T> 解析后的代码对象类型
 * @author DuRuiChi
 * @create 2025/8/7
 **/
public interface CodeParser<T> {
    /**
     * 解析代码
     *
     * @param code 原始代码字符串
     * @return 解析后的代码对象
     */
    T parseCode(String code);
}
