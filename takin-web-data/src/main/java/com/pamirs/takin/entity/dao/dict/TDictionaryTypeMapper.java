package com.pamirs.takin.entity.dao.dict;

import com.pamirs.takin.entity.domain.entity.TDictionaryType;
import org.apache.ibatis.annotations.Param;

public interface TDictionaryTypeMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dictionary_type
     *
     * @mbggenerated
     */
    int deleteByPrimaryKey(String id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dictionary_type
     *
     * @mbggenerated
     */
    int insert(TDictionaryType record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dictionary_type
     *
     * @mbggenerated
     */
    int insertSelective(TDictionaryType record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dictionary_type
     *
     * @mbggenerated
     */
    TDictionaryType selectByPrimaryKey(String id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dictionary_type
     *
     * @mbggenerated
     */
    int updateByPrimaryKeySelective(TDictionaryType record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dictionary_type
     *
     * @mbggenerated
     */
    int updateByPrimaryKey(TDictionaryType record);

    /**
     * 获取字典类型
     *
     * @param typeAlias
     */
    TDictionaryType selectDictionaryByTypeAlias(@Param("typeAlias") String typeAlias);
}
