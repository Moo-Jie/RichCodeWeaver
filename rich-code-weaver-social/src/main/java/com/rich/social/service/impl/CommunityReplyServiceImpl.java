package com.rich.social.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.client.innerService.InnerUserService;
import com.rich.common.exception.BusinessException;
import com.rich.common.exception.ErrorCode;
import com.rich.model.entity.CommunityPost;
import com.rich.model.entity.CommunityReply;
import com.rich.model.entity.User;
import com.rich.model.vo.CommunityReplyVO;
import com.rich.model.vo.UserVO;
import com.rich.social.mapper.CommunityReplyMapper;
import com.rich.social.service.CommunityPostService;
import com.rich.social.service.CommunityReplyLikeService;
import com.rich.social.service.CommunityReplyService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 社区回复服务实现。
 * 负责回复新增、分页查询和回复展示对象组装。
 */
@Service
public class CommunityReplyServiceImpl extends ServiceImpl<CommunityReplyMapper, CommunityReply>
        implements CommunityReplyService {

    @Resource
    @Lazy
    private CommunityPostService communityPostService;

    @Resource
    @Lazy
    private CommunityReplyLikeService communityReplyLikeService;

    @DubboReference
    private InnerUserService innerUserService;

    /**
     * 新增回复，并同步增加帖子回复数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addReply(Long postId, Long userId, String content) {
        if (communityPostService.getById(postId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        CommunityReply reply = CommunityReply.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .likeCount(0)
                .build();
        save(reply);
        UpdateChain.of(CommunityPost.class)
                .setRaw("replyCount", "replyCount + 1")
                .where("id = ?", postId)
                .update();
        return reply.getId();
    }

    /**
     * 分页查询某个帖子下的所有回复。
     * 回复按创建时间正序展示，符合楼层阅读习惯。
     */
    @Override
    public Page<CommunityReplyVO> listReplyByPage(Long postId, Long currentUserId, long pageNum, long pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .from(CommunityReply.class)
                .where("postId = ?", postId)
                .orderBy("createTime ASC");
        Page<CommunityReply> replyPage = page(Page.of(pageNum, pageSize), query);
        List<CommunityReplyVO> voList = replyPage.getRecords().stream()
                .map(reply -> getReplyVO(reply, currentUserId))
                .collect(Collectors.toList());
        return new Page<>(voList, pageNum, pageSize, replyPage.getTotalRow());
    }

    /**
     * 查询帖子下最近的若干条回复，用于帖子列表里的评论预览。
     */
    @Override
    public List<CommunityReplyVO> listLatestReplies(Long postId, Long currentUserId, int limit) {
        QueryWrapper query = QueryWrapper.create()
                .from(CommunityReply.class)
                .where("postId = ?", postId)
                .orderBy("createTime DESC");
        Page<CommunityReply> replyPage = page(Page.of(1, limit), query);
        return replyPage.getRecords().stream()
                .map(reply -> getReplyVO(reply, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 将回复实体转换为回复展示对象，并补充用户信息与点赞状态。
     */
    @Override
    public CommunityReplyVO getReplyVO(CommunityReply reply, Long currentUserId) {
        if (reply == null) {
            return null;
        }
        CommunityReplyVO vo = new CommunityReplyVO();
        BeanUtil.copyProperties(reply, vo);
        try {
            User user = innerUserService.getById(reply.getUserId());
            if (user != null) {
                UserVO userVO = innerUserService.getUserVO(user);
                vo.setUser(userVO);
            }
        } catch (Exception ignored) {
        }
        vo.setHasLiked(currentUserId != null && communityReplyLikeService.hasLiked(reply.getId(), currentUserId));
        return vo;
    }
}
