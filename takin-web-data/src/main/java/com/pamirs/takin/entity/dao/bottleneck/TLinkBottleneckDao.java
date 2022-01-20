package com.pamirs.takin.entity.dao.bottleneck;

import com.pamirs.takin.entity.domain.entity.LinkBottleneck;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TLinkBottleneckDao {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_link_bottleneck
     *
     * @mbg generated
     */
    int deleteByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_link_bottleneck
     *
     * @mbg generated
     */
    int insert(LinkBottleneck record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_link_bottleneck
     *
     * @mbg generated
     */
    int insertSelective(LinkBottleneck record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_link_bottleneck
     *
     * @mbg generated
     */
    LinkBottleneck selectByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_link_bottleneck
     *
     * @mbg generated
     */
    int updateByPrimaryKeySelective(LinkBottleneck record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_link_bottleneck
     *
     * @mbg generated
     */
    int updateByPrimaryKeyWithBLOBs(LinkBottleneck record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_link_bottleneck
     *
     * @mbg generated
     */
    int updateByPrimaryKey(LinkBottleneck record);
}
