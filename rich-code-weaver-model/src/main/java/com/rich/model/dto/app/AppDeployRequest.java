package com.rich.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 产物部署请求
 */
@Data
public class AppDeployRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 产物 id
     */
    private Long appId;
}