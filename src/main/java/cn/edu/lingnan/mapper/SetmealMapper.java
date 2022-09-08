package cn.edu.lingnan.mapper;

import cn.edu.lingnan.entity.Setmeal;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {
    @Update({
            "<script>",
                "update",
                "setmeal",
                "set status = #{status}",
                "where id in",
                    "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
                        "#{id}",
                    "</foreach>",
            "</script>"
    })
    Integer updateStatusBatchByIds(@Param("status") Integer status, @Param("ids") long[] ids);
}
