package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 根据menu name 获取 value
     * @param key menu name
     * @return String
     */
    String getValueByKey(String key);

    /**
     * 同时获取多个配置
     * @param keys 多个配置key
     * @return 查询到的多个结果
     */
    List<String> getValuesByKes(List<String> keys);
}
