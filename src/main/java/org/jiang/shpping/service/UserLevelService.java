package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.dto.UserDTO;
import org.jiang.shpping.entity.User;
import org.jiang.shpping.entity.UserLevel;

public interface UserLevelService extends IService<UserLevel> {
    /**
     * 经验升级
     * @param user 用户
     * @return Boolean
     */
    Boolean upLevel(UserDTO user);
}
