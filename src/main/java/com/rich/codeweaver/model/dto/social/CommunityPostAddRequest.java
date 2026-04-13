package com.rich.codeweaver.model.dto.social;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommunityPostAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;

    private String content;

    private String category;
}
