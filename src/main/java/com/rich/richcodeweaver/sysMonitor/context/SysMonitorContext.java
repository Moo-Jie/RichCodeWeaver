package com.rich.richcodeweaver.sysMonitor.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用于系统监控的上下文信息
 *
 * @author DuRuiChi
 * @create 2025/11/8
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysMonitorContext implements Serializable {

    /**
     * 监控用户 ID
     **/
    private String userId;

    /**
     * 监控应用 ID
     **/
    private String appId;

    @Serial
    private static final long serialVersionUID = 1L;
}
