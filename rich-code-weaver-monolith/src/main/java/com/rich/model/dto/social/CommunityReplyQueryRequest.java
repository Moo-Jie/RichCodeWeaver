package com.rich.model.dto.social;

import com.rich.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommunityReplyQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long postId;
}
