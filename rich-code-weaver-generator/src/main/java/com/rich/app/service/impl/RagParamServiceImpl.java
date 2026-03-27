package com.rich.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.app.mapper.RagParamMapper;
import com.rich.app.service.RagParamService;
import com.rich.model.entity.RagParam;
import com.rich.model.vo.RagParamVO;
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

    @Override
    public List<RagParamVO> listAllParams() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .from(RagParam.class)
                .orderBy("sort_order ASC");
        List<RagParam> list = list(queryWrapper);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public String getParamValue(String paramKey, String defaultValue) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .from(RagParam.class)
                .where("param_key = ?", paramKey);
        RagParam ragParam = getOne(queryWrapper);
        if (ragParam == null || ragParam.getParamValue() == null) {
            log.warn("【RAG参数】未找到参数 key={}，使用默认值 {}", paramKey, defaultValue);
            return defaultValue;
        }
        return ragParam.getParamValue();
    }

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

    @Override
    public RagParamVO toVO(RagParam ragParam) {
        if (ragParam == null) {
            return null;
        }
        RagParamVO vo = new RagParamVO();
        BeanUtil.copyProperties(ragParam, vo);
        return vo;
    }
}
