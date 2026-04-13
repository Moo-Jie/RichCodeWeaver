package com.rich.codeweaver.service.impl.social;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.codeweaver.mapper.AppShareMapper;
import com.rich.codeweaver.model.entity.AppShare;
import com.rich.codeweaver.service.social.AppHotStatService;
import com.rich.codeweaver.service.social.AppShareService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产物转发 服务实现
 * 实现转发记录与热点计数递增（转发不可撤销）
 *
 * @author DuRuiChi
 * @create 2026-03-25
 */
@Service
public class AppShareServiceImpl extends ServiceImpl<AppShareMapper, AppShare>
        implements AppShareService {

    @Resource
    private AppHotStatService appHotStatService;

    /**
     * 记录产物转发行为（转发不可撤销）
     * 使用 @Transactional 保证转发记录和热点统计的原子性更新
     *
     * @param appId  产物id
     * @param userId 用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doShare(Long appId, Long userId) {
        AppShare share = AppShare.builder()
                .appId(appId)
                .userId(userId)
                .build();
        save(share);
        appHotStatService.incrementField(appId, "shareCount");
    }
}
