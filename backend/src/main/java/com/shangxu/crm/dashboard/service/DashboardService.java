package com.shangxu.crm.dashboard.service;

import com.shangxu.crm.activity.entity.Activity;
import com.shangxu.crm.activity.service.ActivityService;
import com.shangxu.crm.contract.entity.Contract;
import com.shangxu.crm.contract.entity.PaymentPlan;
import com.shangxu.crm.contract.mapper.ContractMapper;
import com.shangxu.crm.contract.mapper.PaymentPlanMapper;
import com.shangxu.crm.contract.service.ContractService;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.mapper.CustomerMapper;
import com.shangxu.crm.lead.mapper.LeadMapper;
import com.shangxu.crm.opportunity.mapper.OpportunityMapper;
import com.shangxu.crm.opportunity.service.OpportunityService;
import com.shangxu.crm.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台统计
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerMapper customerMapper;
    private final LeadMapper leadMapper;
    private final OpportunityMapper opportunityMapper;
    private final ContractMapper contractMapper;
    private final PaymentPlanMapper paymentPlanMapper;
    private final com.shangxu.crm.order.mapper.SalesOrderMapper salesOrderMapper;
    private final com.shangxu.crm.delivery.mapper.DeliveryMapper deliveryMapper;
    private final com.shangxu.crm.receivable.mapper.ReceivableMapper receivableMapper;
    private final ContractService contractService;
    private final ActivityService activityService;

    public Map<String, Object> overview() {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();

        long customerTotal = customerMapper.countAll(owner, dept);
        long customerNew = customerMapper.countNewThisMonth(owner, dept);
        long openCount = opportunityMapper.countOpen(owner, dept);
        BigDecimal openAmount = opportunityMapper.sumOpenAmount(owner, dept);
        long won = opportunityMapper.countWon(owner, dept);
        long lost = opportunityMapper.countLost(owner, dept);
        long signedCount = contractMapper.countSigned(owner, dept);
        BigDecimal signedAmount = contractMapper.sumSignedAmount(owner, dept);
        BigDecimal received = paymentPlanMapper.sumReceived(owner, dept);
        long leadTotal = leadMapper.countAll(owner, dept);
        long leadNew = leadMapper.countNewThisMonth(owner, dept);
        long leadConverted = leadMapper.countConverted(owner, dept);

        int winRate = (won + lost) == 0 ? 0 : (int) Math.round(won * 100.0 / (won + lost));

        long orderThisMonth = salesOrderMapper.countThisMonth(owner, dept);
        BigDecimal orderAmount = salesOrderMapper.sumApprovedAmount(owner, dept);
        long pendingDelivery = salesOrderMapper.countPendingDelivery(owner, dept);
        long deliveryDraft = deliveryMapper.countPending(owner, dept);
        BigDecimal receivableTotal = receivableMapper.sumAmount(owner, dept);
        BigDecimal receivableReceived = receivableMapper.sumReceived(owner, dept);
        BigDecimal receivableOverdue = receivableMapper.sumOverdue(owner, dept);
        long receivableOverdueCount = receivableMapper.countOverdue(owner, dept);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerTotal", customerTotal);
        result.put("customerNew", customerNew);
        result.put("leadTotal", leadTotal);
        result.put("leadNew", leadNew);
        result.put("leadConverted", leadConverted);
        result.put("opportunityOpenCount", openCount);
        result.put("opportunityOpenAmount", openAmount);
        result.put("opportunityWonCount", won);
        result.put("winRate", winRate);
        result.put("contractSignedCount", signedCount);
        result.put("contractSignedAmount", signedAmount);
        result.put("receivedAmount", received);
        result.put("pendingAmount", signedAmount.subtract(received).max(BigDecimal.ZERO));
        result.put("orderThisMonth", orderThisMonth);
        result.put("orderAmount", orderAmount);
        result.put("pendingDeliveryCount", pendingDelivery);
        result.put("deliveryDraftCount", deliveryDraft);
        result.put("receivableTotal", receivableTotal);
        result.put("receivableReceived", receivableReceived);
        result.put("receivableUnpaid", receivableTotal.subtract(receivableReceived));
        result.put("receivableOverdue", receivableOverdue);
        result.put("receivableOverdueCount", receivableOverdueCount);
        return result;
    }

    /** 销售漏斗 */
    public List<Map<String, Object>> funnel() {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();
        Map<String, Map<String, Object>> stats = new LinkedHashMap<>();
        opportunityMapper.countByStage(owner, dept).forEach(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stage", row.get("stage"));
            item.put("count", toLong(row.get("total")));
            item.put("amount", toDecimal(row.get("amount")));
            stats.put(String.valueOf(row.get("stage")), item);
        });
        List<Map<String, Object>> result = new ArrayList<>();
        OpportunityService.STAGES.forEach((stage, rate) -> {
            Map<String, Object> item = stats.getOrDefault(stage, Map.of(
                    "stage", stage, "count", 0L, "amount", BigDecimal.ZERO));
            Map<String, Object> row = new LinkedHashMap<>(item);
            row.put("winRate", rate);
            result.add(row);
        });
        return result;
    }

    /** 月度趋势：新增客户数 & 签约金额（万元） */
    public Map<String, Object> trend() {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();
        long[] customerCounts = new long[12];
        BigDecimal[] contractAmounts = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            contractAmounts[i] = BigDecimal.ZERO;
        }
        customerMapper.countMonthly(owner, dept).forEach(row ->
                customerCounts[monthIndex(row.get("name"))] = toLong(row.get("value")));
        contractMapper.sumSignedMonthly(owner, dept).forEach(row ->
                contractAmounts[monthIndex(row.get("name"))] = toDecimal(row.get("value")));

        List<String> months = new ArrayList<>();
        List<Long> customers = new ArrayList<>();
        List<BigDecimal> contracts = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            months.add((i + 1) + "月");
            customers.add(customerCounts[i]);
            contracts.add(contractAmounts[i].divide(BigDecimal.valueOf(10000), 1, java.math.RoundingMode.HALF_UP));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", months);
        result.put("customers", customers);
        result.put("contracts", contracts);
        return result;
    }

    /** 业绩排行 */
    public List<Map<String, Object>> rank() {
        return opportunityMapper.rankByOwner(DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
    }

    /** 客户来源分布 */
    public List<Map<String, Object>> source() {
        return customerMapper.countBySource(DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
    }

    /** 客户级别分布 */
    public List<Map<String, Object>> level() {
        return customerMapper.countByLevel(DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
    }

    /** 待办事项 */
    public Map<String, Object> todo() {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();
        List<Customer> followCustomers = customerMapper.selectTodoFollow(owner, dept);
        List<PaymentPlan> payments = contractService.pendingPayments();
        List<Contract> pendingContracts = contractMapper.selectPendingList(owner, dept);
        List<Activity> activities = activityService.recent(8);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("followCustomers", followCustomers);
        result.put("pendingPayments", payments);
        result.put("pendingContracts", pendingContracts);
        result.put("pendingDeliveries", salesOrderMapper.selectPendingDelivery(owner, dept));
        result.put("pendingReceivables", receivableMapper.selectPendingList(owner, dept));
        result.put("recentActivities", activities);
        return result;
    }

    private int monthIndex(Object value) {
        try {
            int month = Integer.parseInt(String.valueOf(value));
            return Math.min(Math.max(month - 1, 0), 11);
        } catch (Exception e) {
            return 0;
        }
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
