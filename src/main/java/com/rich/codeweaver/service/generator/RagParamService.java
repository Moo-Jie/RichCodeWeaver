package com.rich.codeweaver.service.generator;

import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.entity.RagParam;
import com.rich.codeweaver.model.vo.RagParamVO;

import java.util.List;

/**
 * RAG 参数配置 Service
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
public interface RagParamService extends IService<RagParam> {

    /**
     * 获取所有参数 VO 列表（按 sort_order 排序）
     */
    List<RagParamVO> listAllParams();

    /**
     * 按 paramKey 获取参数字符串值，若不存在则返回 defaultValue
     */
    String getParamValue(String paramKey, String defaultValue);

    /**
     * 获取 int 类型参数，解析失败时返回 defaultValue
     */
    int getIntParam(String paramKey, int defaultValue);

    /**
     * 获取 double 类型参数，解析失败时返回 defaultValue
     */
    double getDoubleParam(String paramKey, double defaultValue);

    /**
     * 将实体转换为 VO
     */
    RagParamVO toVO(RagParam ragParam);
}
