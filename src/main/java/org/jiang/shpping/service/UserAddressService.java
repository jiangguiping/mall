package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.UserAddress;
import org.jiang.shpping.request.UserAddressRequest;
import org.jiang.shpping.vo.PageResult;

import java.util.List;

public interface UserAddressService extends IService<UserAddress> {
    PageResult<UserAddress> getList(PageDTO paheDto);

    /**
     * 添加用户地址
     * @param request 地址请求参数
     * @return UserAddress
     */
    UserAddress create(UserAddressRequest request);


    /**
     * 删除用户地址
     * @param id 地址id
     * @return Boolean
     */
    Boolean delete(Integer id);

    /**
     * 获取地址详情
     * @param id 地址id
     * @return UserAddress
     */
    UserAddress getDetail(Integer id);

    /**
     * 获取用户默认地址
     * @return UserAddress
     */
    UserAddress getDefault();

    /**
     * 设置默认地址
     * @param id 地址id
     * @return Boolean
     */
    Boolean def(Integer id);

    /**
     * 获取默认地址
     * @return UserAddress
     */
    UserAddress getDefaultByUid(Integer uid);
}
