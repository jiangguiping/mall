package org.jiang.shpping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.util.FnvHash;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.UserLevelDao;
import org.jiang.shpping.dto.UserDTO;
import org.jiang.shpping.entity.SystemUserLevel;
import org.jiang.shpping.entity.User;
import org.jiang.shpping.entity.UserLevel;
import org.jiang.shpping.service.SystemUserLevelService;
import org.jiang.shpping.service.UserLevelService;
import org.jiang.shpping.service.UserService;
import org.jiang.shpping.utils.Constants;
import org.jiang.shpping.utils.JanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserLevelServiceImpl extends ServiceImpl<UserLevelDao, UserLevel> implements UserLevelService {

    @Autowired
    private SystemUserLevelService systemUserLevelService;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private UserService userService;



    /**
     * 经验升级
     *
     * @param user 用户
     * @return Boolean
     */
    @Override
    public Boolean upLevel(UserDTO user) {
        //确定当前经验所达到的等级
        List<SystemUserLevel> list = systemUserLevelService.getUsableList();

        if (CollUtil.isEmpty(list)) {
            log.error("系统会员等级未配置，请配置对应数据");
            return Boolean.TRUE;
        }

        SystemUserLevel userLevelConfig = null;
        for (SystemUserLevel systemUserLevel : list) {
            if(user.getExperience() > systemUserLevel.getExperience()){
                userLevelConfig = systemUserLevel;
                continue;
            }
            break;
        }

        if(ObjectUtil.isNull(userLevelConfig)) {
            log.warn("未找到用户对应的会员等级,uid = " + user.getUid());
            return Boolean.TRUE;
        }
        // 判断用户是否还在原等级
        if (user.getLevel().equals(userLevelConfig.getId())) {
            return Boolean.TRUE;
        }
        // 判断用户等级经过向上调整
        List<SystemUserLevel> collect = list.stream().filter(e -> e.getId().equals(user.getLevel())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(collect)) {
            if (collect.get(0).getGrade() > userLevelConfig.getGrade()) {
                return Boolean.TRUE;
            }
        }

        UserLevel newLevel = new UserLevel();
        newLevel.setStatus(true);
        newLevel.setIsDel(false);
        newLevel.setGrade(userLevelConfig.getGrade());
        newLevel.setUid(user.getUid());
        newLevel.setLevelId(userLevelConfig.getId());
        newLevel.setDiscount(userLevelConfig.getDiscount());

        Date date = JanUtils.nowDateTimeReturnDate(Constants.DATE_FORMAT);

        String mark = Constants.USER_LEVEL_UP_LOG_MARK.replace("【{$userName}】", user.getNickname()).
                replace("{$date}", JanUtils.dateToStr(date, Constants.DATE_FORMAT)).
                replace("{$levelName}", userLevelConfig.getName());
        newLevel.setMark(mark);

         User user1 = BeanUtil.copyProperties(user, User.class);

        //更新会员等级
        user.setLevel(userLevelConfig.getId());
        return transactionTemplate.execute(e -> {
            save(newLevel);
            userService.updateById(user1);
            return Boolean.TRUE;
        });
    }
}
