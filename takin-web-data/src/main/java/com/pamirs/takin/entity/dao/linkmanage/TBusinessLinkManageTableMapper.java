package com.pamirs.takin.entity.dao.linkmanage;

import java.util.List;

import com.pamirs.takin.entity.domain.dto.linkmanage.BusinessActiveIdAndNameDto;
import com.pamirs.takin.entity.domain.dto.linkmanage.BusinessLinkDto;
import com.pamirs.takin.entity.domain.entity.linkmanage.BusinessLinkManageTable;
import com.pamirs.takin.entity.domain.entity.linkmanage.LinkQueryVo;
import org.apache.ibatis.annotations.Param;

public interface TBusinessLinkManageTableMapper {
    int deleteByPrimaryKey(Long linkId);

    int insert(BusinessLinkManageTable record);

    int insertSelective(BusinessLinkManageTable record);

    BusinessLinkManageTable selectByPrimaryKey(Long linkId);

    int updateByPrimaryKeySelective(BusinessLinkManageTable record);

    int updateByPrimaryKey(BusinessLinkManageTable record);

    int countByBussinessName(@Param("linkName") String linkName);

    List<BusinessLinkDto> selectBussinessLinkListBySelective2(@Param("query") LinkQueryVo queryVo,@Param("userIds") List<Long> userIds);

    BusinessLinkDto selectBussinessLinkById(@Param("id") Long id);

    List<BusinessLinkManageTable> selectBussinessLinkByIdList(@Param("list") List<Long> businessIds);

    int updateByTechId(BusinessLinkManageTable businessLinkManageTable);

    //统计有效的业务活动
    long count();

    int updateIsChangeByTechId(@Param("techLinkId") Long techLinkId);

    List<BusinessActiveIdAndNameDto> bussinessActiveNameFuzzSearch(
        @Param("bussinessActiveName") String bussinessActiveName);

    /**
     * 按照系统流程的id修改业务活动中的入口名
     *
     * @param linkId
     * @param newEntrance
     * @return
     */
    int updateEntranceNameBySystemProcessId(@Param("linkId") String linkId, @Param("newEntrance") String newEntrance);

    /**
     * 根据业务活动id集合查询系统流程id集合
     *
     * @param ids
     * @return
     */
    List<String> selectTechIdsByBusinessIds(@Param("list") List<Long> ids);

    List<BusinessLinkManageTable> selectByPrimaryKeys(@Param("list") List<Long> businessIds);

    long cannotdelete(@Param("list") List<Long> relateBusinessLinkIds, @Param("canDelete") Long canDelete);
}
