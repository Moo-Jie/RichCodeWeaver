package com.rich.codeweaver.service.impl.generator;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.codeweaver.mapper.RagParamMapper;
import com.rich.codeweaver.model.entity.RagParam;
import com.rich.codeweaver.model.vo.RagParamVO;
import com.rich.codeweaver.service.generator.RagParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 参数配置 Service 实现
 *
 * @author DuRuiChi
 * @create 2026/3/27
 */
@Slf4j
@Service
public class RagParamServiceImpl extends ServiceImpl<RagParamMapper, RagParam>
        implements RagParamService {

    /**
     * 查询全部 RAG 参数并转换为 VO 列表
     *
     * @return 参数 VO 列表
     */
    @Override
    public List<RagParamVO> listAllParams() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .from(RagParam.class)
                .orderBy("sort_order ASC");
        List<RagParam> list = list(queryWrapper);
        if (isEmptyList(list)) {
            return new ArrayList<>();
        }
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 按参数 key 获取参数值
     *
     * @param paramKey 参数 key
     * @param defaultValue 默认值
     * @return 参数值
     */
    @Override
    public String getParamValue(String paramKey, String defaultValue) {
        RagParam ragParam = getParamByKey(paramKey);
        if (ragParam == null || ragParam.getParamValue() == null) {
            log.warn("【RAG参数】未找到参数 key={}，使用默认值 {}", paramKey, defaultValue);
            return defaultValue;
        }
        return ragParam.getParamValue();
    }

    /**
     * 获取 int 类型参数
     *
     * @param paramKey 参数 key
     * @param defaultValue 默认值
     * @return int 参数值
     */
    @Override
    public int getIntParam(String paramKey, int defaultValue) {
        String value = getParamValue(paramKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("【RAG参数】参数 key={} 值 '{}' 无法解析为 int，使用默认值 {}", paramKey, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取 double 类型参数
     *
     * @param paramKey 参数 key
     * @param defaultValue 默认值
     * @return double 参数值
     */
    @Override
    public double getDoubleParam(String paramKey, double defaultValue) {
        String value = getParamValue(paramKey, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("【RAG参数】参数 key={} 值 '{}' 无法解析为 double，使用默认值 {}", paramKey, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 将参数实体转换为 VO
     *
     * @param ragParam 参数实体
     * @return 参数 VO
     */
    @Override
    public RagParamVO toVO(RagParam ragParam) {
        if (ragParam == null) {
            return null;
        }
        RagParamVO vo = new RagParamVO();
        BeanUtil.copyProperties(ragParam, vo);
        return vo;
    }

    /**
     * 根据参数 key 查询参数实体
     *
     * @param paramKey 参数 key
     * @return 参数实体
     */
    private RagParam getParamByKey(String paramKey) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .from(RagParam.class)
                .where("param_key = ?", paramKey);
        return getOne(queryWrapper);
    }

    /**
     * 判断参数列表是否为空
     *
     * @param list 参数列表
     * @return 是否为空
     */
    private boolean isEmptyList(List<RagParam> list) {
        return list == null || list.isEmpty();
    }
}
