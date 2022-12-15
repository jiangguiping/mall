package org.jiang.shpping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.SystemUserLevelDao;
import org.jiang.shpping.entity.SystemUserLevel;
import org.jiang.shpping.entity.UserLevel;
import org.jiang.shpping.service.SystemUserLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemUserLevelServiceImpl extends ServiceImpl<SystemUserLevelDao, SystemUserLevel> implements SystemUserLevelService {

    @Autowired
    private SystemUserLevelDao dao;

    /**
     * 获取可用等级列表
     * @return List
     */

    @Override
    public List<SystemUserLevel> getUsableList() {
        LambdaQueryWrapper<SystemUserLevel> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SystemUserLevel::getIsShow, true);
        lqw.eq(SystemUserLevel::getIsDel, false);
        lqw.orderByAsc(SystemUserLevel::getGrade);
        return dao.selectList(lqw);
    }

    @Override
    public SystemUserLevel getByLevelId(Integer levelId) {
        return dao.selectOne(new LambdaQueryWrapper<SystemUserLevel>()
                .eq(SystemUserLevel::getIsShow,1)
                .eq(SystemUserLevel::getIsDel,0)
                .eq(SystemUserLevel::getId,levelId));
    }
}
