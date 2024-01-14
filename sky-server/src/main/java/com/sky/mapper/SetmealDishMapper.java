package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import javax.swing.*;
import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据dishId查套餐Id
     * @param dishId
     * @return
     */
    List<Long> getSetmealIdsByDishId(List<Long> dishId);

}
