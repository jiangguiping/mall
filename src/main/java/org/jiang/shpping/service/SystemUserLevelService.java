package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.SystemUserLevel;

import java.util.List;

public interface SystemUserLevelService extends IService<SystemUserLevel> {

    /**
     * 获取可用等级列表
     * @return List
     */
    List<SystemUserLevel> getUsableList();

    SystemUserLevel getByLevelId(Integer levelId);
}
