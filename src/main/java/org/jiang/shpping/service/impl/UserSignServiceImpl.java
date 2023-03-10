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
     * ??????
     *
     * @return SystemGroupDataSignConfigVo
     */
    @Override
    public SystemGroupDataSignConfigVo sign() {
         UserDTO user = UserHolder.getUser();

        // ??????????????????????????????
        UserSign lastUserSign = getLastDayByUid(user.getUid());
        if (ObjectUtil.isNull(lastUserSign)) {
            //??????????????? ??????????????????
            user.setSignNum(0);
        }else {
            String lastDate = JanUtils.dateToStr(lastUserSign.getCreateDay(), Constants.DATE_FORMAT_DATE);
            String nowDate = JanUtils.nowDate(Constants.DATE_FORMAT_DATE);
            //??????????????????
            if (JanUtils.compareDate(lastDate, nowDate, Constants.DATE_FORMAT_DATE) == 0) {
                throw new BizException("???????????????.??????????????????");
            }
            String nextDate =JanUtils.addDay(lastUserSign.getCreateDay(),1,Constants.DATE_FORMAT_DATE);
            int compareDate = JanUtils.compareDate(nextDate, nowDate, Constants.DATE_FORMAT_DATE);

            if (compareDate != 0) {
                //????????????????????????????????????,??????????????????
                user.setSignNum(0);
            }
        }

        //??????????????????
        List<SystemGroupDataSignConfigVo> config = getSignConfig();
        if (CollUtil.isEmpty(config)) {
            throw new BizException("?????????????????????????????????????????????????????????");
        }

        //??????????????????????????? ?????????????????????????????????????????????????????????
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
            throw new BizException("????????????????????????");
        }

        //??????????????????

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
        integralRecord.setMark(StrUtil.format("???????????????????????????{}??????", configVo.getIntegral()));
        integralRecord.setStatus(IntegralRecordConstants.INTEGRAL_RECORD_STATUS_COMPLETE);


        //????????????????????????
        UserExperienceRecord experienceRecord = new UserExperienceRecord();
        experienceRecord.setUid(user.getUid());
        experienceRecord.setLinkType(ExperienceRecordConstants.EXPERIENCE_RECORD_LINK_TYPE_SIGN);
        experienceRecord.setType(ExperienceRecordConstants.EXPERIENCE_RECORD_TYPE_ADD);
        experienceRecord.setTitle(ExperienceRecordConstants.EXPERIENCE_RECORD_TITLE_SIGN);
        experienceRecord.setExperience(configVo.getExperience());
        experienceRecord.setBalance(user.getExperience() + configVo.getExperience());
        experienceRecord.setMark(StrUtil.format("???????????????????????????{}??????", configVo.getExperience()));
        experienceRecord.setStatus(ExperienceRecordConstants.EXPERIENCE_RECORD_STATUS_CREATE);

        // ??????????????????
        user.setIntegral(user.getIntegral() + configVo.getIntegral());
        // ??????????????????
        user.setExperience(user.getExperience() + configVo.getExperience());


         User user1 = BeanUtil.copyProperties(user, User.class);


        Boolean execute = transactionTemplate.execute(e -> {
            //??????????????????
            save(userSign);
            // ????????????????????????
            userIntegralRecordService.save(integralRecord);
            //????????????????????????
            userExperienceRecordService.save(experienceRecord);
            //???????????? ??????????????????????????????
            userService.updateById(user1);
            // ????????????
            userLevelService.upLevel(user);
            return Boolean.TRUE;
        });

        if (!execute) {
            throw new BizException("??????????????????????????????!");
        }

        return configVo;
    }



    /**
     * ????????????
     *
     * @return List<SystemGroupDataSignConfigVo>
     */

    @Override
    public List<SystemGroupDataSignConfigVo> getSignConfig() {
        //??????????????????
        return systemGroupDataService.getListByGid(SysGroupDataConstants.GROUP_DATA_ID_SIGN, SystemGroupDataSignConfigVo.class);

    }

    /**
     * ??????????????????
     *
     * @return HashMap
     */
    @Override
    public HashMap<String, Object> get() {
         HashMap<String, Object> map = new HashMap<>();
         //????????????
        User info = userService.getInfo();

        map.put("integral", info.getIntegral());
        //??????????????????
        map.put("count", signCount(info.getUid()));
        //??????????????????

        //????????????????????????
        map.put("today", false);
        return map;
    }

    /**
     * ????????????????????????
     *
     * @return UserSignInfoResponse
     */
    @Override
    public UserSignInfoResponse getUserSignInfo() {

        UserDTO user = null;
        try {
            user = UserHolder.getUser();
        }catch (Exception e) {
            throw new  BizException("??????????????????");
        }
        UserSignInfoResponse userSignInfoResponse = new UserSignInfoResponse();

        BeanUtils.copyProperties(user, userSignInfoResponse);

        //???????????????
        // ???????????????
        userSignInfoResponse.setSumSignDay(getCount(user.getUid()));
        // ??????????????????
        Boolean isCheckNowDaySign = checkDaySign(user.getUid());
        userSignInfoResponse.setIsDaySign(isCheckNowDaySign);

        // ??????????????????
        Boolean isYesterdaySign = checkYesterdaySign(user.getUid());
        userSignInfoResponse.setIsYesterdaySign(isYesterdaySign);

        if (!isYesterdaySign) {
            // ??????????????????
            if (!isCheckNowDaySign) {
                userSignInfoResponse.setSignNum(0);
            }
        }
        //??????????????????: ??????????????????????????????????????? ????????????
        if (userSignInfoResponse.getSignNum() > 0 && userSignInfoResponse.getSignNum().equals(getSignConfig().size())) {
            userSignInfoResponse.setSignNum(0);
            userService.repeatSignNum(user.getUid());
        }

        userSignInfoResponse.setIntegral(user.getIntegral());
        return userSignInfoResponse;
    }

    /**
     * ???????????????????????????
     *
     * @param userId Integer ??????id
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
     * ????????????????????????
     *
     * @param userId Integer ??????id
     * @return UserSignInfoResponse
     * @author Mr.Zhang
     * @since 2020-05-29
     */
    private Boolean checkDaySign(Integer userId) {
        List<UserSign> userSignList = getInfoByDay(userId, JanUtils.nowDate(Constants.DATE_FORMAT_DATE));
        return userSignList.size() != 0;
    }

    /**
     * ????????????????????????
     *
     * @param userId Integer ??????id
     * @param date   Date ??????
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
     * ??????????????????
     *
     * @param userId Integer ??????id
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
     * ??????????????????
     *
     * @param userId Integer ??????id
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
     * ?????????????????????????????????
     * @param uid ??????id
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
