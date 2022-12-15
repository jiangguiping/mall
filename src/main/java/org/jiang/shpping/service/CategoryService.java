package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.entity.Category;
import org.jiang.shpping.vo.CategoryTreeVo;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> getChildVoListByPid(Integer cid);

    /**
     * 获取树形结构数据
     * @param type 分类
     * @param status 状态
     * @param name 名称
     * @return List
     */
    List<CategoryTreeVo> getListTree(Integer type, Integer status, String name);
}
