package com.rich.social.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.model.entity.AppShare;
import com.rich.social.mapper.AppShareMapper;
import com.rich.social.service.AppHotStatService;
import com.rich.social.service.AppShareService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产物转发 服务实现
 * 实现转发记录与热点计数递增（转发不可撤销）
 *
 * @author DuRuiChi
 * @since 2026-03-25
 */
@Service
public class AppShareServiceImpl extends ServiceImpl<AppShareMapper, AppShare>
        implements AppShareService {

    @Resource
    private AppHotStatService appHotStatService;

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
