package com.shangxu.crm.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.contract.entity.ContractItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractItemMapper extends BaseMapper<ContractItem> {

    @Select("SELECT * FROM crm_contract_item WHERE contract_id = #{contractId} ORDER BY id ASC")
    List<ContractItem> selectByContractId(@Param("contractId") Long contractId);

    @Delete("DELETE FROM crm_contract_item WHERE contract_id = #{contractId}")
    int deleteByContractId(@Param("contractId") Long contractId);
}
