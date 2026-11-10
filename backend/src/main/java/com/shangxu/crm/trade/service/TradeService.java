package com.shangxu.crm.trade.service;

import com.shangxu.crm.contract.mapper.ContractMapper;
import com.shangxu.crm.delivery.mapper.DeliveryMapper;
import com.shangxu.crm.invoice.mapper.InvoiceMapper;
import com.shangxu.crm.returns.mapper.ReturnOrderMapper;
import com.shangxu.crm.order.mapper.SalesOrderMapper;
import com.shangxu.crm.receivable.mapper.ReceivableMapper;
import com.shangxu.crm.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易中心：把 合同 / 订单 / 出库 / 应收 / 回款 串成一条交易链路
 */
@Service
@RequiredArgsConstructor
public class TradeService {

    private final ContractMapper contractMapper;
    private final SalesOrderMapper orderMapper;
    private final DeliveryMapper deliveryMapper;
    private final ReceivableMapper receivableMapper;
    private final InvoiceMapper invoiceMapper;
    private final ReturnOrderMapper returnOrderMapper;

    public Map<String, Object> overview() {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();

        BigDecimal contractAmount = contractMapper.sumSignedAmount(owner, dept);
        long contractCount = contractMapper.countSigned(owner, dept);
        BigDecimal orderAmount = orderMapper.sumApprovedAmount(owner, dept);
        long orderCount = orderMapper.countApproved(owner, dept);
        BigDecimal deliveredAmount = deliveryMapper.sumShippedAmount(owner, dept);
        long deliveredCount = deliveryMapper.countShipped(owner, dept);
        BigDecimal receivableTotal = receivableMapper.sumAmount(owner, dept);
        BigDecimal received = receivableMapper.sumReceived(owner, dept);
        BigDecimal overdue = receivableMapper.sumOverdue(owner, dept);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractAmount", contractAmount);
        result.put("contractCount", contractCount);
        result.put("orderAmount", orderAmount);
        result.put("orderCount", orderCount);
        result.put("deliveredAmount", deliveredAmount);
        result.put("deliveredCount", deliveredCount);
        result.put("receivableAmount", receivableTotal);
        result.put("receivedAmount", received);
        result.put("unpaidAmount", receivableTotal.subtract(received));
        result.put("overdueAmount", overdue);
        result.put("orderPendingDelivery", orderMapper.countPendingDelivery(owner, dept));
        result.put("deliveryDraft", deliveryMapper.countPending(owner, dept));
        result.put("receivableOverdueCount", receivableMapper.countOverdue(owner, dept));
        result.put("invoiceAmount", invoiceMapper.sumIssued(owner, dept));
        return result;
    }

    /** 近 12 个月：合同签订额 / 订单额 / 回款额（万元由前端换算） */
    public Map<String, Object> trend() {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();

        BigDecimal[] contracts = new BigDecimal[12];
        BigDecimal[] orders = new BigDecimal[12];
        BigDecimal[] receipts = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            contracts[i] = BigDecimal.ZERO;
            orders[i] = BigDecimal.ZERO;
            receipts[i] = BigDecimal.ZERO;
        }
        contractMapper.sumSignedMonthly(owner, dept).forEach(row ->
                contracts[monthIndex(row.get("name"))] = toDecimal(row.get("value")));
        orderMapper.sumOrderMonthly(owner, dept).forEach(row ->
                orders[monthIndex(row.get("name"))] = toDecimal(row.get("value")));
        receivableMapper.sumReceivedMonthly(owner, dept).forEach(row ->
                receipts[monthIndex(row.get("name"))] = toDecimal(row.get("value")));

        List<String> months = new ArrayList<>();
        List<BigDecimal> contractList = new ArrayList<>();
        List<BigDecimal> orderList = new ArrayList<>();
        List<BigDecimal> receiptList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            months.add((i + 1) + "月");
            contractList.add(contracts[i].divide(BigDecimal.valueOf(10000), 1, java.math.RoundingMode.HALF_UP));
            orderList.add(orders[i].divide(BigDecimal.valueOf(10000), 1, java.math.RoundingMode.HALF_UP));
            receiptList.add(receipts[i].divide(BigDecimal.valueOf(10000), 1, java.math.RoundingMode.HALF_UP));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", months);
        result.put("contracts", contractList);
        result.put("orders", orderList);
        result.put("receipts", receiptList);
        return result;
    }

    /** 交易流水：合同签订、订单下达、出库、收款 四类事件合并倒序 */
    public List<Map<String, Object>> flow(int limit) {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();
        int size = Math.min(Math.max(limit, 1), 100);

        List<Map<String, Object>> flow = new ArrayList<>();
        append(flow, contractMapper.selectRecentSigned(owner, dept, size), "CONTRACT", "合同");
        append(flow, orderMapper.selectRecentApproved(owner, dept, size), "ORDER", "订单");
        append(flow, deliveryMapper.selectRecentShipped(owner, dept, size), "DELIVERY", "出库");
        append(flow, receivableMapper.selectRecentReceived(owner, dept, size), "RECEIPT", "收款");
        append(flow, invoiceMapper.selectRecentIssued(owner, dept, size), "INVOICE", "开票");
        append(flow, returnOrderMapper.selectRecentRefunded(owner, dept, size), "REFUND", "退款");

        flow.sort(Comparator.comparing((Map<String, Object> item) -> String.valueOf(item.get("time"))).reversed());
        return flow.size() > size ? new ArrayList<>(flow.subList(0, size)) : flow;
    }

    /** 客户交易排行（按已审核订单金额） */
    public List<Map<String, Object>> rank() {
        return orderMapper.selectCustomerRank(DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
    }

    private void append(List<Map<String, Object>> target, List<Map<String, Object>> rows, String type, String typeName) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("type", type);
            item.put("typeName", typeName);
            target.add(item);
        }
    }

    private int monthIndex(Object value) {
        try {
            int month = Integer.parseInt(String.valueOf(value));
            return Math.min(Math.max(month - 1, 0), 11);
        } catch (Exception e) {
            return 0;
        }
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
