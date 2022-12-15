package org.jiang.shpping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.UserAddressDao;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.SystemCity;
import org.jiang.shpping.entity.UserAddress;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.request.UserAddressRequest;
import org.jiang.shpping.service.SystemCityService;
import org.jiang.shpping.service.UserAddressService;
import org.jiang.shpping.utils.BeanCopyUtils;
import org.jiang.shpping.utils.UserHolder;
import org.jiang.shpping.vo.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.beans.beancontext.BeanContextProxy;
import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressDao, UserAddress> implements UserAddressService {

    @Resource
    private UserAddressDao userAddressDao;

    @Autowired
    private SystemCityService systemCityService;

    @Override
    public PageResult<UserAddress> getList(PageDTO paheDto) {
        Integer uid = null;
        try {
             uid = UserHolder.getUser().getUid();
        }catch (Exception e) {
           throw new  BizException("用户没有登录");
        }

        Page<UserAddress> page =new Page<>(paheDto.getPage(),paheDto.getLimit());
        LambdaQueryWrapper<UserAddress> lqw =new LambdaQueryWrapper<UserAddress>();
        lqw.select(UserAddress::getId, UserAddress::getRealName, UserAddress::getPhone, UserAddress::getProvince,
                UserAddress::getCity, UserAddress::getDistrict, UserAddress::getDetail, UserAddress::getIsDefault);

        lqw.eq(UserAddress::getUid,uid);
        lqw.eq(UserAddress::getIsDel,false);
        lqw.orderByDesc(UserAddress::getIsDefault);
        lqw.orderByDesc(UserAddress::getId);
        Page<UserAddress> userAddressPage =userAddressDao.selectPage(page,lqw);


        List<UserAddress> userAddressdao = BeanCopyUtils.copyList(userAddressPage.getRecords(),UserAddress.class);

        return new PageResult<>(userAddressdao, (int) userAddressPage.getTotal());
    }

    /**
     * 添加用户地址
     *
     * @param request 地址请求参数
     * @return UserAddress
     */
    @Override
    public UserAddress create(UserAddressRequest request) {

        UserAddress userAddress = new UserAddress();
        BeanUtils.copyProperties(request,userAddress);
        userAddress.setCity(request.getAddress().getCity());
        userAddress.setCityId(request.getAddress().getCityId());
        userAddress.setDistrict(request.getAddress().getDistrict());
        userAddress.setProvince(request.getAddress().getProvince());

        //添加地址时 cityId和城市不能同时为空 如果id为空 必须用城市自查后 set CityId
        if (request.getAddress().getCityId() == 0 && StringUtils.isBlank(request.getAddress().getCity())) {
            throw new BizException("请选择正确的城市数据");
        }
        ////StringUtils.isNotBlank 当Str为空白或者null时，isNotBlank返回false 当Str的length>0时，isNotBlank返回true
        if (StringUtils.isNotBlank(request.getAddress().getCity()) && request.getAddress().getCityId() ==0) {
            //根据城市名称获取城市详细数据
           SystemCity  currentCity =systemCityService.getCityByCityName(request.getAddress().getCity());
            if (ObjectUtil.isNull(currentCity)) {
                throw new BizException("当前城市未找到");
            }
            userAddress.setCityId(currentCity.getCityId());
        }
        if (request.getAddress().getCityId() > 0 && StringUtils.isNotBlank(request.getAddress().getCity())) {
            checkCity(userAddress.getCityId());
        }
        userAddress.setUid(UserHolder.getUser().getUid());
        if(userAddress.getIsDefault()) {
            //把当前用户其他默认地址取消
            cancelDefault(userAddress.getUid());
        }
        saveOrUpdate(userAddress);

        return userAddress;
    }

    /**
     * 删除用户地址
     *
     * @param id 地址id
     * @return Boolean
     */
    @Override
    public Boolean delete(Integer id) {
        userAddressDao.delete(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId,id)
                .eq(UserAddress::getUid,UserHolder.getUser().getUid()));
        return true;
    }

    /**
     * 获取地址详情
     *
     * @param id 地址id
     * @return UserAddress
     */
    @Override
    public UserAddress getDetail(Integer id) {
        return userAddressDao.selectOne(new LambdaQueryWrapper<UserAddress>()
                .select(UserAddress::getId, UserAddress::getRealName, UserAddress::getPhone, UserAddress::getProvince,
                UserAddress::getCity, UserAddress::getDistrict, UserAddress::getDetail, UserAddress::getIsDefault)
                .eq(UserAddress::getId,id)
                .eq(UserAddress::getUid,UserHolder.getUser().getUid())
                .eq(UserAddress::getIsDel,false));
    }

    /**
     * 获取用户默认地址
     *
     * @return UserAddress
     */
    @Override
    public UserAddress getDefault() {
        return userAddressDao.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getIsDefault,true)
                .eq(UserAddress::getUid,UserHolder.getUser().getUid()));
    }

    /**
     * 设置默认地址
     *
     * @param id 地址id
     * @return Boolean
     */
    @Override
    public Boolean def(Integer id) {
        //把当前用户其他默认地址取消
        cancelDefault(UserHolder.getUser().getUid());
        UserAddress userAddress = new UserAddress()
                .setId(id)
                .setUid(UserHolder.getUser().getUid())
                .setIsDefault(true);
        return updateById(userAddress);
    }

    /**
     * 获取默认地址
     *
     * @param uid
     * @return UserAddress
     */
    @Override
    public UserAddress getDefaultByUid(Integer uid) {
        return userAddressDao.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getIsDefault,true)
                .eq(UserAddress::getUid,uid));
    }


    /**
     * 取消默认地址
     * @param userId Integer 城市id
     */
    private void cancelDefault(Integer userId) {
        //检测城市Id是否存在
        UserAddress userAddress = new UserAddress();
        userAddress.setIsDefault(false);
        LambdaQueryWrapper<UserAddress> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserAddress::getUid, userId);
        update(userAddress, lambdaQueryWrapper);
    }

    private void checkCity(Integer cityId) {
        //检测城市ID是否存在
        SystemCity systemCity =systemCityService.getCityByCityId(cityId);
        if (ObjectUtil.isNull(systemCity)) {
            throw new BizException("请选择正确的城市");
        }
    }


}
