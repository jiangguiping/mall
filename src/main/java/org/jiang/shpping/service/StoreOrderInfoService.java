package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.StoreOrderInfo;
import org.jiang.shpping.vo.StoreOrderInfoOldVo;
import org.jiang.shpping.vo.StoreOrderInfoVo;

import java.util.List;

public interface StoreOrderInfoService extends IService<StoreOrderInfo> {
    /**
     * 获取订单详情vo列表
     * @param orderId 订单id
     * @return List<StoreOrderInfoVo>
     */
    List<StoreOrderInfoVo> getVoListByOrderId(Integer orderId);



    /**
     * 批量添加订单详情
     * @param storeOrderInfos 订单详情集合
     * @return 保存结果
     */
    boolean saveOrderInfos(List<StoreOrderInfo> storeOrderInfos);

    /**
     * 获取订单详情-订单编号
     * @param orderNo 订单编号
     * @return List
     */
    List<StoreOrderInfo> getListByOrderNo(String orderNo);

    List<StoreOrderInfoOldVo> getOrderListByOrderId(Integer orderId);

    /**
     * 通过订单编号和规格号查询
     * @param uni 规格号
     * @param orderId 订单编号
     * @return StoreOrderInfo
     */
    StoreOrderInfo getByUniAndOrderId(String uni, Integer orderId);

}
