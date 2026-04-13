package com.rich.codeweaver.model.dto.social;

import com.rich.codeweaver.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommunityPostQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String category;

    private String searchText;
}
