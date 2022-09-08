package cn.edu.lingnan.dto;

import cn.edu.lingnan.entity.Setmeal;
import cn.edu.lingnan.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
