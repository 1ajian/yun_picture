package com.jianzhao.picturebackend.tasks;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jianzhao.picturebackend.mapper.UserMapper;
import com.jianzhao.picturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ClassName: UserCleanupTask
 * Package: com.jianzhao.picturebackend.tasks
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/25 18:36
 * @Version 1.0
 */
@Component
@Slf4j
public class UserCleanupTask {

    @Resource
    private UserMapper userMapper;

    /**
     * 每天一点删除 isDelete = 1 的用户
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupDeleteUsers() {
        log.info("开始执行定时任务：清理逻辑删除用户...");
        Integer count = userMapper.deleteUser();
        log.info("定时任务结束：清理逻辑删除用户，删除了{}个用户", count);
    }
}
