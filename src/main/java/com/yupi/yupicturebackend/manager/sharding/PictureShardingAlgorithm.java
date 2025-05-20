package com.yupi.yupicturebackend.manager.sharding;

import cn.hutool.core.util.StrUtil;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * ClassName: PictureShardingAlgorithm
 * Package: com.yupi.yupicturebackend.manager.sharding
 * Description:
 *  自定义分表算法 相当于一个路由的作用(路由到具体哪个表)
 * @Author 阿小健
 * @Create 2025/5/19 14:03
 * @Version 1.0
 */
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {
    /**
     *
     * @param availableTargetNames 所有分表名的集合
     * @param preciseShardingValue 包含了分表列和逻辑表名
     * @return
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();

        //私有、或者公共
        if (spaceId == null) {
            return logicTableName;
        }

        String realTableName =  "picture_" + spaceId;
        //包含了分表
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        } else {
            return logicTableName;
        }

    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayList<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }

}
