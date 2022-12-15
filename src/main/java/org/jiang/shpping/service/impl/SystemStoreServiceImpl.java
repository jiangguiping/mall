package org.jiang.shpping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.SystemStoreDao;
import org.jiang.shpping.entity.SystemStore;
import org.jiang.shpping.service.SystemStoreService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class SystemStoreServiceImpl extends ServiceImpl<SystemStoreDao, SystemStore> implements SystemStoreService {

    @Resource
    private SystemStoreDao dao;

    /**
     * 根据基本参数查询
     * @param systemStore 基本参数
     * @return 门店结果
     */
    @Override
    public SystemStore getByCondition(SystemStore systemStore) {
        LambdaQueryWrapper<SystemStore> lqw = new LambdaQueryWrapper<>();
        lqw.setEntity(systemStore);
        return dao.selectOne(lqw);
    }
}
