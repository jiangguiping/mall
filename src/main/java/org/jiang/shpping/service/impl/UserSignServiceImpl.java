package org.jiang.shpping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.dto.UserDTO;
import org.jiang.shpping.entity.User;
import org.jiang.shpping.entity.UserExperienceRecord;
import org.jiang.shpping.entity.UserIntegralRecord;
import org.jiang.shpping.entity.UserSign;
import org.jiang.shpping.dao.UserSignDao;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.response.UserSignInfoResponse;
import org.jiang.shpping.service.*;
import org.jiang.shpping.utils.*;
import org.jiang.shpping.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

@Service
public class UserSignServiceImpl extends ServiceImpl<UserSignDao, UserSign> implements UserSignService {


    @Resource
    private UserSignDao dao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserLevelService userLevelService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserIntegralRecordService userIntegralRecordService;

    @Autowired
    private UserExperienceRecordService userExperienceRecordService;

    @Autowired
    private SystemGroupDataService systemGroupDataService;

    @Override
    public PageResult<UserSignVo> getList(PageDTO pageDTO) {

        Page<UserSign> page = new Page<>(pageDTO.getPage(),pageDTO.getLimit());

         LambdaQueryWrapper<UserSign> lwq = new LambdaQueryWrapper<UserSign>()
                .eq(UserSign::getType, 1)
                .eq(UserSign::getUid, UserHolder.getUser().getUid()).orderByDesc(UserSign::getId);
         Page<UserSign> userSignPage = dao.selectPage(page, lwq);

        List<UserSignVo> signvo = BeanCopyUtils.copyList(userSignPage.getRecords(),UserSignVo.class);

        return new PageResult<>(signvo, (int) userSignPage.getTotal());
    }

    @Override
    public PageResult<UserSignMonthVo> getListGroupMonth(PageDTO pageDTO) {

        Page<UserSign> page = new Page<>(pageDTO.getPage(),pageDTO.getLimit());

        LambdaQueryWrapper<UserSign> lambdaQueryWrapper = new LambdaQueryWrapper<UserSign>()
                .eq(UserSign::getType,1)
                .eq(UserSign::getUid,UserHolder.getUser().getUid())
                .orderByDesc(UserSign::getCreateDay);

        Page<UserSign> userSignPage = dao.selectPage(page, lambdaQueryWrapper);

        List<UserSignMonthVo> singMonthVos =BeanCopyUtils.copyList(userSignPage.getRecords(),UserSignMonthVo.class);

        return new PageResult<>(singMonthVos,(int) userSignPage.getTotal());
    }

    /**
     * 签到
     *
     * @return SystemGroupDataSignConfigVo
     */
    @Override
    public SystemGroupDataSignConfigVo sign() {
         UserDTO user = UserHolder.getUser();

        // 获取最后一次签到记录
        UserSign lastUserSign = getLastDayByUid(user.getUid());
        if (ObjectUtil.isNull(lastUserSign)) {
            //没有签到过 重置签到次数
            user.setSignNum(0);
        }else {
            String lastDate = JanUtils.dateToStr(lastUserSign.getCreateDay(), Constants.DATE_FORMAT_DATE);
            String nowDate = JanUtils.nowDate(Constants.DATE_FORMAT_DATE);
            //对比今天数据
            if (JanUtils.compareDate(lastDate, nowDate, Constants.DATE_FORMAT_DATE) == 0) {
                throw new BizException("今日以签到.不可重复签到");
            }
            String nextDate =JanUtils.addDay(lastUserSign.getCreateDay(),1,Constants.DATE_FORMAT_DATE);
            int compareDate = JanUtils.compareDate(nextDate, nowDate, Constants.DATE_FORMAT_DATE);

            if (compareDate != 0) {
                //不相等，所以不是连续签到,重置签到次数
                user.setSignNum(0);
            }
        }

        //获取签到数据
        List<SystemGroupDataSignConfigVo> config = getSignConfig();
        if (CollUtil.isEmpty(config)) {
            throw new BizException("签到配置不存在，请在管理端配置签到数据");
        }

        //如果已经签到一个周 那么再次签到的时候直接从第一天重新开始
        if (user.getSignNum().equals(config.size())) {
            user.setSignNum(0);
        }

        user.setSignNum(user.getSignNum() +1);
        SystemGroupDataSignConfigVo configVo =null;
        for (SystemGroupDataSignConfigVo systemSignConfigVo : config) {
            if (user.getSignNum().equals(systemSignConfigVo.getDay())) {
                configVo = systemSignConfigVo;
                break ;
            }
        }
        if (ObjectUtil.isNull(configVo)) {
            throw new BizException("请先配置签到天数");
        }

        //保存签到数据

        UserSign userSign =new UserSign();
        userSign.setUid(user.getUid());
        userSign.setTitle(Constants.SIGN_TYPE_INTEGRAL_TITLE);
        userSign.setNumber(configVo.getIntegral());
        userSign.setType(Constants.SIGN_TYPE_INTEGRAL);
        userSign.setBalance(user.getIntegral() + configVo.getIntegral());
        userSign.setCreateDay(JanUtils.strToDate(JanUtils.nowDate(Constants.DATE_FORMAT_DATE), Constants.DATE_FORMAT_DATE));


        UserIntegralRecord integralRecord = new UserIntegralRecord();
        integralRecord.setUid(user.getUid());
        integralRecord.setLinkType(IntegralRecordConstants.INTEGRAL_RECORD_LINK_TYPE_SIGN);
        integralRecord.setType(IntegralRecordConstants.INTEGRAL_RECORD_TYPE_ADD);
        integralRecord.setTitle(IntegralRecordConstants.BROKERAGE_RECORD_TITLE_SIGN);
        integralRecord.setIntegral(configVo.getIntegral());
        integralRecord.setBalance(user.getIntegral() + configVo.getIntegral());
        integralRecord.setMark(StrUtil.format("签到积分奖励增加了{}积分", configVo.getIntegral()));
        integralRecord.setStatus(IntegralRecordConstants.INTEGRAL_RECORD_STATUS_COMPLETE);


        //更新用户经验信息
        UserExperienceRecord experienceRecord = new UserExperienceRecord();
        experienceRecord.setUid(user.getUid());
        experienceRecord.setLinkType(ExperienceRecordConstants.EXPERIENCE_RECORD_LINK_TYPE_SIGN);
        experienceRecord.setType(ExperienceRecordConstants.EXPERIENCE_RECORD_TYPE_ADD);
        experienceRecord.setTitle(ExperienceRecordConstants.EXPERIENCE_RECORD_TITLE_SIGN);
        experienceRecord.setExperience(configVo.getExperience());
        experienceRecord.setBalance(user.getExperience() + configVo.getExperience());
        experienceRecord.setMark(StrUtil.format("签到经验奖励增加了{}经验", configVo.getExperience()));
        experienceRecord.setStatus(ExperienceRecordConstants.EXPERIENCE_RECORD_STATUS_CREATE);

        // 更新用户积分
        user.setIntegral(user.getIntegral() + configVo.getIntegral());
        // 更新用户经验
        user.setExperience(user.getExperience() + configVo.getExperience());


         User user1 = BeanUtil.copyProperties(user, User.class);


        Boolean execute = transactionTemplate.execute(e -> {
            //保存签到数据
            save(userSign);
            // 更新用户积分记录
            userIntegralRecordService.save(integralRecord);
            //更新用户经验信息
            userExperienceRecordService.save(experienceRecord);
            //更新用户 签到天数、积分、经验
            userService.updateById(user1);
            // 用户升级
            userLevelService.upLevel(user);
            return Boolean.TRUE;
        });

        if (!execute) {
            throw new BizException("修改用户签到信息失败!");
        }

        return configVo;
    }



    /**
     * 签到配置
     *
     * @return List<SystemGroupDataSignConfigVo>
     */

    @Override
    public List<SystemGroupDataSignConfigVo> getSignConfig() {
        //获取配置数据
        return systemGroupDataService.getListByGid(SysGroupDataConstants.GROUP_DATA_ID_SIGN, SystemGroupDataSignConfigVo.class);

    }

    /**
     * 今日记录详情
     *
     * @return HashMap
     */
    @Override
    public HashMap<String, Object> get() {
         HashMap<String, Object> map = new HashMap<>();
         //当前积分
        User info = userService.getInfo();

        map.put("integral", info.getIntegral());
        //总计签到天数
        map.put("count", signCount(info.getUid()));
        //连续签到数据

        //今日是否已经签到
        map.put("today", false);
        return map;
    }

    /**
     * 获取用户签到信息
     *
     * @return UserSignInfoResponse
     */
    @Override
    public UserSignInfoResponse getUserSignInfo() {

        UserDTO user = null;
        try {
            user = UserHolder.getUser();
        }catch (Exception e) {
            throw new  BizException("用户没有登录");
        }
        UserSignInfoResponse userSignInfoResponse = new UserSignInfoResponse();

        BeanUtils.copyProperties(user, userSignInfoResponse);

        //签到总次数
        // 签到总次数
        userSignInfoResponse.setSumSignDay(getCount(user.getUid()));
        // 今天是否签到
        Boolean isCheckNowDaySign = checkDaySign(user.getUid());
        userSignInfoResponse.setIsDaySign(isCheckNowDaySign);

        // 昨天是否签到
        Boolean isYesterdaySign = checkYesterdaySign(user.getUid());
        userSignInfoResponse.setIsYesterdaySign(isYesterdaySign);

        if (!isYesterdaySign) {
            // 今天是否签到
            if (!isCheckNowDaySign) {
                userSignInfoResponse.setSignNum(0);
            }
        }
        //连续签到天数: 当前用户已经签到完一个周期 那么重置
        if (userSignInfoResponse.getSignNum() > 0 && userSignInfoResponse.getSignNum().equals(getSignConfig().size())) {
            userSignInfoResponse.setSignNum(0);
            userService.repeatSignNum(user.getUid());
        }

        userSignInfoResponse.setIntegral(user.getIntegral());
        return userSignInfoResponse;
    }

    /**
     * 检测昨天天是否签到
     *
     * @param userId Integer 用户id
     * @return UserSignInfoResponse
     * @author Mr.Zhang
     * @since 2020-05-29
     */
    private Boolean checkYesterdaySign(Integer userId) {
        String day = JanUtils.nowDate(Constants.DATE_FORMAT_DATE);
        String yesterday = JanUtils.addDay(day, -1, Constants.DATE_FORMAT_DATE);
        List<UserSign> userSignList = getInfoByDay(userId, yesterday);
        return userSignList.size() != 0;
    }

    /**
     * 检测今天是否签到
     *
     * @param userId Integer 用户id
     * @return UserSignInfoResponse
     * @author Mr.Zhang
     * @since 2020-05-29
     */
    private Boolean checkDaySign(Integer userId) {
        List<UserSign> userSignList = getInfoByDay(userId, JanUtils.nowDate(Constants.DATE_FORMAT_DATE));
        return userSignList.size() != 0;
    }

    /**
     * 根据日期查询数据
     *
     * @param userId Integer 用户id
     * @param date   Date 日期
     * @return UserSignInfoResponse
     * @author Mr.Zhang
     * @since 2020-05-29
     */
    private List<UserSign> getInfoByDay(Integer userId, String date) {
        LambdaQueryWrapper<UserSign> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserSign::getUid, userId).eq(UserSign::getType, 1).eq(UserSign::getCreateDay, date);
        return dao.selectList(lambdaQueryWrapper);
    }

    /**
     * 累计签到次数
     *
     * @param userId Integer 用户id
     * @return UserSignInfoResponse
     * @author Mr.Zhang
     * @since 2020-05-29
     */
    private Integer getCount(Integer userId) {
        LambdaQueryWrapper<UserSign> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserSign::getUid, userId).eq(UserSign::getType, 1);
        return dao.selectCount(lambdaQueryWrapper);
    }

    /**
     * 累计签到天数
     *
     * @param userId Integer 用户id
     * @return Integer
     * @author Mr.Zhang
     * @since 2020-04-30
     */
    private Integer signCount(Integer userId) {
        LambdaQueryWrapper<UserSign> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserSign::getUid, userId);
        return dao.selectCount(lambdaQueryWrapper);
    }

    /**
     * 获取签到的最后一条记录
     * @param uid 用户id
     * @return UserSign
     */
    private UserSign getLastDayByUid(Integer uid) {
        return dao.selectOne(new LambdaQueryWrapper<UserSign>()
                .select(UserSign::getCreateDay)
                .eq(UserSign::getUid,uid)
                .orderByDesc(UserSign::getCreateDay)
                .last(" limit 1"));
    }
}
