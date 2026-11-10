package com.shangxu.crm.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.mapper.CustomerMapper;
import com.shangxu.crm.notice.service.NoticeService;
import com.shangxu.crm.receivable.entity.Receivable;
import com.shangxu.crm.receivable.mapper.ReceivableMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 业务定时任务：把「CRM 该催的事」自动推给负责人。
 * <p>
 * 多实例部署时只让一个实例跑：其余实例设 crm.scheduler.enabled=false，
 * 该 Bean 不会被注册（而不是注册后空转），避免重复推送与重复回收。
 */
@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "crm.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CrmScheduler {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReceivableMapper receivableMapper;
    private final com.shangxu.crm.changelog.service.ChangeLogService changeLogService;
    private final CustomerMapper customerMapper;
    private final NoticeService noticeService;

    @Value("${crm.scheduler.enabled:true}")
    private boolean enabled;

    @Value("${crm.pool.recycle-days:30}")
    private int recycleDays;

    @Value("${crm.scheduler.follow-remind-hours:24}")
    private int followRemindHours;

    /** 每天 09:00 逾期应收提醒 */
    @Scheduled(cron = "${crm.scheduler.receivable-cron:0 0 9 * * ?}")
    public void remindOverdueReceivable() {
        if (!enabled) {
            return;
        }
        List<Receivable> list = receivableMapper.selectOverdueAll();
        int sent = 0;
        for (Receivable r : list) {
            BigDecimal unpaid = nz(r.getAmount()).subtract(nz(r.getReceivedAmount()));
            long days = Duration.between(r.getDueDate().atStartOfDay(), LocalDateTime.now()).toDays();
            noticeService.push(r.getOwnerUserId(),
                    "应收单 " + r.getReceivableNo() + " 已逾期 " + Math.max(days, 1) + " 天",
                    r.getCustomerName() + " 应收 " + unpaid.toPlainString() + " 元，应收日期 "
                            + r.getDueDate().format(DATE) + "，请尽快催收。",
                    "RECEIVABLE", "RECEIVABLE", r.getId(), r.getReceivableNo());
            sent++;
        }
        log.info("逾期应收提醒完成，扫描 {} 条，发送 {} 条", list.size(), sent);
    }

    /** 每天 09:05 跟进提醒 */
    @Scheduled(cron = "${crm.scheduler.follow-cron:0 5 9 * * ?}")
    public void remindFollowUp() {
        if (!enabled) {
            return;
        }
        List<Customer> list = customerMapper.selectFollowDue(followRemindHours);
        for (Customer c : list) {
            noticeService.push(c.getOwnerUserId(),
                    "客户「" + c.getName() + "」待跟进",
                    "计划跟进时间 " + c.getNextFollowTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            + "，请及时联系并记录跟进结果。",
                    "FOLLOWUP", "CUSTOMER", c.getId(), null);
        }
        log.info("跟进提醒完成，扫描 {} 条", list.size());
    }

    /** 每天 09:10 公海自动回收：私海客户超过 N 天无跟进，自动放回公海 */
    @Scheduled(cron = "${crm.scheduler.pool-cron:0 10 9 * * ?}")
    public void recycleIdleCustomers() {
        if (!enabled || recycleDays <= 0) {
            return;
        }
        List<Customer> list = customerMapper.selectIdlePrivate(recycleDays);
        for (Customer c : list) {
            customerMapper.update(null, Wrappers.<Customer>lambdaUpdate()
                    .eq(Customer::getId, c.getId())
                    .set(Customer::getPoolType, "PUBLIC")
                    .set(Customer::getOwnerUserId, null)
                    .set(Customer::getDeptId, null));
            changeLogService.recordOwnerChange(c.getId(), c.getCustomerNo(), c.getOwnerUserId(),
                    "RECYCLE", "超过 " + recycleDays + " 天未跟进，自动回收至公海");
            noticeService.push(c.getOwnerUserId(),
                    "客户「" + c.getName() + "」已自动回收至公海",
                    "超过 " + recycleDays + " 天未跟进，系统已自动回收，可重新领取。",
                    "POOL", "CUSTOMER", c.getId(), null);
        }
        log.info("公海回收完成，回收 {} 个客户", list.size());
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
