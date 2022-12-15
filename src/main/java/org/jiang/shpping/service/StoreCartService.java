package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.StoreCart;
import org.jiang.shpping.request.CartNumRequest;
import org.jiang.shpping.request.CartRequest;
import org.jiang.shpping.response.CartInfoResponse;
import org.jiang.shpping.vo.PageResult;

import java.util.List;
import java.util.Map;

public interface StoreCartService extends IService<StoreCart> {

    /**
     * 新增购物车数据
     * @param storeCartRequest 新增购物车参数
     * @return 新增结果
     */
    String saveCate(CartRequest storeCartRequest);

    /**
     * 删除购物车
     * @param ids 待删除id
     * @return 返回删除状态
     */
    Boolean deleteCartByIds(List<Long> ids);

    /**
     * 修改购物车商品数量
     * @param id 购物车id
     * @param number 数量
     */
    Boolean updateCartNum(Integer id, Integer number);

    /**
     * 获取当前购物车数量
     * @param request 请求参数
     * @return 数量
     */
    Map<String, Integer> getUserCount(CartNumRequest request);

    PageResult<CartInfoResponse> getPageList(PageDTO pageDTO);

    /**
     * 通过id和uid获取购物车信息
     * @param id 购物车id
     * @param uid 用户uid
     * @return StoreCart
     */
    StoreCart getByIdAndUid(Long id, Integer uid);
}
