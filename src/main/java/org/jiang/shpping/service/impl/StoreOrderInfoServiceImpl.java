package org.jiang.shpping.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.StoreOrderInfoDao;
import org.jiang.shpping.entity.StoreOrderInfo;
import org.jiang.shpping.service.StoreOrderInfoService;
import org.jiang.shpping.service.StoreProductReplyService;
import org.jiang.shpping.vo.OrderInfoDetailVo;
import org.jiang.shpping.vo.StoreOrderInfoOldVo;
import org.jiang.shpping.vo.StoreOrderInfoVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class StoreOrderInfoServiceImpl extends ServiceImpl<StoreOrderInfoDao, StoreOrderInfo>
        implements StoreOrderInfoService {

    @Resource
    private StoreOrderInfoDao dao;

    @Autowired
    private StoreProductReplyService storeProductReplyService;


    /**
     * 获取订单详情vo列表
     *
     * @param orderId 订单id
     * @return List<StoreOrderInfoVo>
     */
    @Override
    public List<StoreOrderInfoVo> getVoListByOrderId(Integer orderId) {
         List<StoreOrderInfo> systemStoreStaffList = dao.selectList(new LambdaQueryWrapper<StoreOrderInfo>()
                .eq(StoreOrderInfo::getOrderId, orderId));
        if(systemStoreStaffList.size() < 1){
            return null;
        }
        List<StoreOrderInfoVo> storeOrderInfoVoList = new ArrayList<>();
        for (StoreOrderInfo storeOrderInfo : systemStoreStaffList) {
            StoreOrderInfoVo storeOrderInfoVo = new StoreOrderInfoVo();
            BeanUtils.copyProperties(storeOrderInfo, storeOrderInfoVo, "info");
            storeOrderInfoVo.setInfo(JSON.parseObject(storeOrderInfo.getInfo(), OrderInfoDetailVo.class));
            storeOrderInfoVoList.add(storeOrderInfoVo);
        }
        return storeOrderInfoVoList;
    }

    /**
     * 批量添加订单详情
     *
     * @param storeOrderInfos 订单详情集合
     * @return 保存结果
     */
    @Override
    public boolean saveOrderInfos(List<StoreOrderInfo> storeOrderInfos) {
        return saveBatch(storeOrderInfos);
    }

    /**
     * 获取订单详情-订单编号
     *
     * @param orderNo 订单编号
     * @return List
     */
    @Override
    public List<StoreOrderInfo> getListByOrderNo(String orderNo) {
        LambdaQueryWrapper<StoreOrderInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrderInfo::getOrderNo, orderNo);
        return dao.selectList(lqw);
    }

    /**
     * 根据id集合查询数据，返回 map
     * @param orderId Integer id
     * @author Mr.Zhang
     * @since 2020-04-17
     * @return HashMap<Integer, StoreCart>
     */
    @Override
    public List<StoreOrderInfoOldVo> getOrderListByOrderId(Integer orderId){
        LambdaQueryWrapper<StoreOrderInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StoreOrderInfo::getOrderId, orderId);
        List<StoreOrderInfo> systemStoreStaffList = dao.selectList(lambdaQueryWrapper);
        if(systemStoreStaffList.size() < 1){
            return null;
        }

        List<StoreOrderInfoOldVo> storeOrderInfoVoList = new ArrayList<>();
        for (StoreOrderInfo storeOrderInfo : systemStoreStaffList) {
            //解析商品详情JSON
            StoreOrderInfoOldVo storeOrderInfoVo = new StoreOrderInfoOldVo();
            BeanUtils.copyProperties(storeOrderInfo, storeOrderInfoVo, "info");
            storeOrderInfoVo.setInfo(JSON.parseObject(storeOrderInfo.getInfo(), OrderInfoDetailVo.class));
            storeOrderInfoVo.getInfo().setIsReply(
                    storeProductReplyService.isReply(storeOrderInfoVo.getUnique(), storeOrderInfoVo.getOrderId()) ? 1 : 0
            );
            storeOrderInfoVoList.add(storeOrderInfoVo);
        }
        return storeOrderInfoVoList;
    }

    /**
     * 通过订单编号和规格号查询
     * @param uni 规格号
     * @param orderId 订单编号
     * @return StoreOrderInfo
     */
    @Override
    public StoreOrderInfo getByUniAndOrderId(String uni, Integer orderId) {
        //带 StoreOrderInfo 类的多条件查询
        LambdaQueryWrapper<StoreOrderInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StoreOrderInfo::getOrderId, orderId);
        lambdaQueryWrapper.eq(StoreOrderInfo::getUnique, uni);
        return dao.selectOne(lambdaQueryWrapper);
    }
}
