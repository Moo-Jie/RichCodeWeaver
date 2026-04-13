package com.rich.model.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String userAccount;

    private String userPassword;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    private String userIdentity;

    private String userIndustry;

    private String phone;

    private String email;

    private LocalDateTime editTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDelete;
}
