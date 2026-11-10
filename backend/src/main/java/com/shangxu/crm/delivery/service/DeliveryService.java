package com.shangxu.crm.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.customer.entity.Contact;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.mapper.ContactMapper;
import com.shangxu.crm.customer.mapper.CustomerMapper;
import com.shangxu.crm.delivery.entity.Delivery;
import com.shangxu.crm.delivery.entity.DeliveryItem;
import com.shangxu.crm.delivery.mapper.DeliveryItemMapper;
import com.shangxu.crm.delivery.mapper.DeliveryMapper;
import com.shangxu.crm.order.entity.OrderItem;
import com.shangxu.crm.order.entity.SalesOrder;
import com.shangxu.crm.order.mapper.OrderItemMapper;
import com.shangxu.crm.order.mapper.SalesOrderMapper;
import com.shangxu.crm.order.service.OrderService;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;


    private final DeliveryMapper deliveryMapper;
    private final DeliveryItemMapper deliveryItemMapper;
    private final SalesOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;
    private final com.shangxu.crm.receivable.service.ReceivableService receivableService;
    private final com.shangxu.crm.receivable.mapper.ReceivableMapper receivableMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final com.shangxu.crm.system.mapper.SysUserMapper userMapper;

    public PageResult<Delivery> page(PageQuery query, String status, Long orderId) {
        Page<Delivery> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Delivery> result = deliveryMapper.selectPageList(page, query.getKeyword(), status, orderId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public Delivery detail(Long id) {
        Delivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BizException("出库单不存在");
        }
        delivery.setItems(deliveryItemMapper.selectByDeliveryId(id));
        if (delivery.getOwnerUserId() != null) {
            com.shangxu.crm.system.entity.SysUser owner = userMapper.selectById(delivery.getOwnerUserId());
            delivery.setOwnerName(owner == null ? null : owner.getNickname());
        }
        return delivery;
    }

    public List<Delivery> listByOrder(Long orderId) {
        Page<Delivery> page = new Page<>(1, 50);
        return deliveryMapper.selectPageList(page, null, null, orderId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords();
    }

    /**
     * 由订单生成出库单（自动带出明细、收货人与地址）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromOrder(Long orderId) {
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException("只有已审核的订单才能生成出库单");
        }
        if (deliveryMapper.countActiveByOrder(orderId) > 0) {
            throw new BizException("该订单已生成出库单，请勿重复操作");
        }
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new BizException("订单没有明细，无法出库");
        }

        Delivery delivery = new Delivery();
        delivery.setDeliveryNo(sequenceService.nextNo("delivery", "DN"));
        delivery.setOrderId(orderId);
        delivery.setOrderNo(order.getOrderNo());
        delivery.setCustomerId(order.getCustomerId());
        delivery.setCustomerName(order.getCustomerName());
        delivery.setDeliveryDate(LocalDate.now());
        delivery.setWarehouse("主仓");
        delivery.setStatus("DRAFT");
        delivery.setAmount(order.getAmount());
        delivery.setOwnerUserId(order.getOwnerUserId());
        delivery.setDeptId(order.getDeptId());
        delivery.setRemark("由订单 " + order.getOrderNo() + " 生成");

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer != null) {
            delivery.setAddress((customer.getProvince() == null ? "" : customer.getProvince())
                    + (customer.getCity() == null ? "" : customer.getCity())
                    + (customer.getAddress() == null ? "" : customer.getAddress()));
            delivery.setReceiver(customer.getShortName());
            delivery.setReceiverPhone(customer.getPhone());
        }
        List<Contact> contacts = contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, order.getCustomerId())
                .orderByDesc(Contact::getIsPrimary)
                .orderByAsc(Contact::getId));
        if (!contacts.isEmpty()) {
            Contact contact = contacts.get(0);
            delivery.setReceiver(contact.getName());
            delivery.setReceiverPhone(contact.getMobile());
        }

        BigDecimal totalQty = orderItems.stream()
                .map(item -> item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        delivery.setTotalQty(totalQty);
        deliveryMapper.insert(delivery);

        for (OrderItem item : orderItems) {
            DeliveryItem deliveryItem = new DeliveryItem();
            deliveryItem.setDeliveryId(delivery.getId());
            deliveryItem.setProductName(item.getProductName());
            deliveryItem.setUnit(item.getUnit());
            deliveryItem.setQuantity(item.getQuantity());
            deliveryItemMapper.insert(deliveryItem);
        }
        return delivery.getId();
    }

    public void update(Delivery delivery) {
        Delivery exist = deliveryMapper.selectById(delivery.getId());
        if (exist == null) {
            throw new BizException("出库单不存在");
        }
        if (!"DRAFT".equals(exist.getStatus())) {
            throw new BizException("出库单已提交审核，不能修改；如需调整请先驳回");
        }
        delivery.setDeliveryNo(null);
        delivery.setOrderId(null);
        deliveryMapper.updateById(delivery);
    }

    /** 确认出库：同步刷新订单出库状态 */
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id) {
        Delivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BizException("出库单不存在");
        }
        if (!"APPROVED".equals(delivery.getStatus())) {
            throw new BizException("出库单需审核通过后才能确认出库");
        }
        Delivery update = new Delivery();
        update.setId(id);
        update.setStatus("SHIPPED");
        update.setDeliveryDate(LocalDate.now());
        deliveryMapper.updateById(update);
        orderService.refreshDeliveryStatus(delivery.getOrderId(), deliveryMapper.sumShippedQty(delivery.getOrderId()));
    }

    /**
     * 出库单审批：DRAFT 提交审核 -> PENDING；PENDING -> APPROVED / REJECTED。
     * 审核通过时自动为该订单生成应收单（已生成则跳过），打通"交付即确认应收"。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String status) {
        Delivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BizException("出库单不存在");
        }
        String current = delivery.getStatus();
        boolean allowed = switch (status) {
            case "PENDING" -> "DRAFT".equals(current) || "REJECTED".equals(current);
            case "APPROVED", "REJECTED" -> "PENDING".equals(current);
            default -> false;
        };
        if (!allowed) {
            throw new BizException("当前状态不允许该操作");
        }
        Delivery update = new Delivery();
        update.setId(id);
        update.setStatus(status);
        deliveryMapper.updateById(update);

        if ("APPROVED".equals(status)
                && receivableMapper.countBySource("ORDER", delivery.getOrderId()) == 0) {
            // 先查再建：避免内部抛异常把外层事务标记为回滚
            receivableService.createFromOrder(delivery.getOrderId());
        }
    }

    /** 客户签收 */
    public void sign(Long id) {
        Delivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BizException("出库单不存在");
        }
        if (!"SHIPPED".equals(delivery.getStatus())) {
            throw new BizException("只有已出库的单据可以签收");
        }
        Delivery update = new Delivery();
        update.setId(id);
        update.setStatus("SIGNED");
        deliveryMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Delivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new BizException("出库单不存在");
        }
        if ("SIGNED".equals(delivery.getStatus())) {
            throw new BizException("已签收的出库单不能作废");
        }
        Delivery update = new Delivery();
        update.setId(id);
        update.setStatus("CANCELLED");
        deliveryMapper.updateById(update);
        orderService.refreshDeliveryStatus(delivery.getOrderId(), deliveryMapper.sumShippedQty(delivery.getOrderId()));
    }

    public void delete(Long id) {
        Delivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            return;
        }
        if ("SHIPPED".equals(delivery.getStatus()) || "SIGNED".equals(delivery.getStatus())) {
            throw new BizException("已出库的单据不能删除，请先作废");
        }
        deliveryMapper.deleteById(id);
        deliveryItemMapper.deleteByDeliveryId(id);
    }

    public void createManual(Delivery delivery, List<DeliveryItem> items) {
        delivery.setId(null);
        delivery.setDeliveryNo(sequenceService.nextNo("delivery", "DN"));
        if (delivery.getOwnerUserId() == null) {
            delivery.setOwnerUserId(SecurityUtils.getUserId());
            delivery.setDeptId(SecurityUtils.getDeptId());
        }
        if (delivery.getStatus() == null) {
            delivery.setStatus("DRAFT");
        }
        if (delivery.getDeliveryDate() == null) {
            delivery.setDeliveryDate(LocalDate.now());
        }
        BigDecimal totalQty = items == null ? BigDecimal.ZERO : items.stream()
                .map(item -> item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        delivery.setTotalQty(totalQty);
        deliveryMapper.insert(delivery);
        if (items != null) {
            for (DeliveryItem item : items) {
                item.setId(null);
                item.setDeliveryId(delivery.getId());
                deliveryItemMapper.insert(item);
            }
        }
    }
}
