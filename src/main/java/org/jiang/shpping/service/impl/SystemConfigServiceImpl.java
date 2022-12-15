package org.jiang.shpping.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.SystemConfigDao;
import org.jiang.shpping.entity.SystemConfig;
import org.jiang.shpping.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigDao, SystemConfig> implements SystemConfigService {
    @Resource
    private SystemConfigDao dao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据menu name 获取 value
     *
     * @param name menu name
     * @return String
     */
    @Override
    public String getValueByKey(String name) {
        return get(name);
    }

    /**
     * 同时获取多个配置
     *
     * @param keys 多个配置key
     * @return 查询到的多个结果
     */
    @Override
    public List<String> getValuesByKes(List<String> keys) {
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            result.add(getValueByKey(key));
        }
        return result;
    }

    /**
     * 把数据同步到redis
     * @param name String
     * @return String
     */
    private String get(String name) {
         SystemConfig systemConfig = dao.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getStatus, false)
                .eq(SystemConfig::getName, name));
         return systemConfig.getValue();
    }
}
