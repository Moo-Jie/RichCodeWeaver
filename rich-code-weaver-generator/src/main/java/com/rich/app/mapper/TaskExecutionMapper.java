package com.rich.app.mapper;

import com.mybatisflex.core.BaseMapper;
import com.rich.model.entity.TaskExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务执行记录Mapper
 *
 * @author DuRuiChi
 * @create 2026-05-06
 */
@Mapper
public interface TaskExecutionMapper extends BaseMapper<TaskExecution> {
}
