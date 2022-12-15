package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.SystemGroupData;
import org.jiang.shpping.vo.SystemGroupDataSignConfigVo;

import java.util.List;

public interface SystemGroupDataService extends IService<SystemGroupData> {

    /**
     * 通过gid获取列表 推荐二开使用
     * @param gid Integer group id
     * @return List<T>
     */
    <T> List<T> getListByGid(Integer gid, Class<T> cls);
}
