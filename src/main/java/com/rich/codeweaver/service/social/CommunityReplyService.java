package com.rich.codeweaver.service.social;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.rich.codeweaver.model.entity.CommunityReply;
import com.rich.codeweaver.model.vo.CommunityReplyVO;

import java.util.List;

/**
 * 社区回复服务接口。
 */
public interface CommunityReplyService extends IService<CommunityReply> {

    /**
     * 新增回复。
     */
    Long addReply(Long postId, Long userId, String content);

    /**
     * 分页查询指定帖子的回复。
     */
    Page<CommunityReplyVO> listReplyByPage(Long postId, Long currentUserId, long pageNum, long pageSize);

    /**
     * 获取帖子下的最新评论预览。
     */
    List<CommunityReplyVO> listLatestReplies(Long postId, Long currentUserId, int limit);

    /**
     * 将回复实体转换为前端展示对象。
     */
    CommunityReplyVO getReplyVO(CommunityReply reply, Long currentUserId);
}
