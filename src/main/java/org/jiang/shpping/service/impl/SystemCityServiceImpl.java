package org.jiang.shpping.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jiang.shpping.dao.SystemCityDao;
import org.jiang.shpping.entity.SystemCity;
import org.jiang.shpping.service.SystemCityService;
import org.jiang.shpping.utils.Constants;
import org.jiang.shpping.vo.SystemCityTreeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemCityServiceImpl extends ServiceImpl<SystemCityDao, SystemCity> implements SystemCityService {

    @Autowired
    private SystemCityDao dao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;



    /**
     * 根据城市名称获取城市详细数据
     *
     * @param cityName 城市名称
     * @return 城市数据
     */
    @Override
    public SystemCity getCityByCityName(String cityName) {
         SystemCity systemCity = dao.selectOne(new LambdaQueryWrapper<SystemCity>()
                .eq(SystemCity::getName, cityName)
                .eq(SystemCity::getIsShow, 1)
                .eq(SystemCity::getLevel, 1));
        return systemCity;
    }

    /**
     * 获取城市
     *
     * @param cityId 城市id
     * @return SystemCity
     */
    @Override
    public SystemCity getCityByCityId(Integer cityId) {
         SystemCity systemCity = dao.selectOne(new LambdaQueryWrapper<SystemCity>()
                .eq(SystemCity::getCityId, cityId)
                .eq(SystemCity::getIsShow, 1));
        return systemCity;
    }

    /**
     * 获取城市树
     */
    @Override
    public List<SystemCityTreeVo> getListTree() {
        String cityList = stringRedisTemplate.opsForValue().get(Constants.CITY_LIST_TREE);
         List<SystemCityTreeVo> jsonObject = JSONObject.parseObject(cityList, List.class);

        if (CollUtil.isEmpty(jsonObject)) {
            setListTree();
        }
        String cityList2 =stringRedisTemplate.opsForValue().get(Constants.CITY_LIST_TREE);
        List<SystemCityTreeVo> jsonObject2 = JSONObject.parseObject(cityList, List.class);
        return jsonObject2;
    }

    public void setListTree() {
        //循环数据，把数据对象变成带list结构的vo
        List<SystemCityTreeVo> treeList = new ArrayList<>();

        LambdaQueryWrapper<SystemCity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.select(SystemCity::getId, SystemCity::getCityId, SystemCity::getParentId, SystemCity::getName);
        lambdaQueryWrapper.eq(SystemCity::getIsShow, true);
        List<SystemCity> allTree = dao.selectList(lambdaQueryWrapper);
        if (CollUtil.isEmpty(allTree)) {
            return;
        }

        for (SystemCity systemCity : allTree) {
            SystemCityTreeVo systemCityTreeVo = new SystemCityTreeVo();
            BeanUtils.copyProperties(systemCity, systemCityTreeVo);
            treeList.add(systemCityTreeVo);
        }
        //返回
        Map<Integer, SystemCityTreeVo> map = new HashMap<>();
        //cityId 为 key 存储到map 中
        for (SystemCityTreeVo systemCityTreeVo1 : treeList) {
            map.put(systemCityTreeVo1.getCityId(), systemCityTreeVo1);
        }
        List<SystemCityTreeVo> list = new ArrayList<>();
        for (SystemCityTreeVo tree : treeList) {
            //子集ID返回对象，有则添加。
            SystemCityTreeVo tree1 = map.get(tree.getParentId());
            if (tree1 != null) {
                tree1.getChild().add(tree);
            } else {
                list.add(tree);
            }
        }
         String json  = JSONObject.toJSONString(list);
        stringRedisTemplate.opsForValue().set(Constants.CITY_LIST_TREE, json);
    }
}
