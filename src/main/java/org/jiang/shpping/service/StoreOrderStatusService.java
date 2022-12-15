package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.StoreOrderStatus;

import java.util.List;

public interface StoreOrderStatusService extends IService<StoreOrderStatus> {
    /**
     * 添加订单日志
     * @param orderId 订单id
     * @param type 类型
     * @param message 备注
     * @return Boolean
     */
    Boolean createLog(Integer orderId, String type, String message);

    /**
     * 根据实体参数获取
     * @param storeOrderStatus 订单状态参数
     * @return 订单状态结果
     */
    List<StoreOrderStatus> getByEntity(StoreOrderStatus storeOrderStatus);
}
