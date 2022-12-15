package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.SystemCity;
import org.jiang.shpping.vo.SystemCityTreeVo;

import java.util.List;

public interface SystemCityService extends IService<SystemCity> {


    /**
     * 根据城市名称获取城市详细数据
     * @param cityName 城市名称
     * @return 城市数据
     */
    SystemCity getCityByCityName(String cityName);


    /**
     * 获取城市
     * @param cityId 城市id
     * @return SystemCity
     */
    SystemCity getCityByCityId(Integer cityId);

    /**
     * 获取城市树
     */
    List<SystemCityTreeVo> getListTree();
}
