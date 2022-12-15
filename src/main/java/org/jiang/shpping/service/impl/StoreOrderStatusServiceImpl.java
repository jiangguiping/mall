package org.jiang.shpping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.StoreOrderStatusDao;
import org.jiang.shpping.entity.StoreOrderStatus;
import org.jiang.shpping.service.StoreOrderStatusService;
import org.jiang.shpping.utils.JanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class StoreOrderStatusServiceImpl extends ServiceImpl<StoreOrderStatusDao, StoreOrderStatus> implements StoreOrderStatusService {


    @Resource
    private StoreOrderStatusDao dao;

    /**
     * 创建记录日志
     * @param orderId Integer 订单号
     * @param type String 类型
     * @param message String 消息
     * @return Boolean
     */
    @Override
    public Boolean createLog(Integer orderId, String type, String message) {
        StoreOrderStatus storeOrderStatus = new StoreOrderStatus();
        storeOrderStatus.setOid(orderId);
        storeOrderStatus.setChangeType(type);
        storeOrderStatus.setChangeMessage(message);
        storeOrderStatus.setCreateTime(JanUtils.nowDateTime());
        return save(storeOrderStatus);
    }

    /**
     * 根据实体获取
     * @param storeOrderStatus 订单状态参数
     * @return 查询结果
     */
    @Override
    public List<StoreOrderStatus> getByEntity(StoreOrderStatus storeOrderStatus) {
        LambdaQueryWrapper<StoreOrderStatus> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.setEntity(storeOrderStatus);
        return dao.selectList(lambdaQueryWrapper);
    }
}
