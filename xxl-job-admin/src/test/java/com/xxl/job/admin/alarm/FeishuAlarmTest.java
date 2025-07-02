package com.xxl.job.admin.alarm;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.core.biz.model.ReturnT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

/**
 * 飞书告警测试类
 */
@SpringBootTest
@ActiveProfiles("test") // 如果有测试环境配置文件
public class FeishuAlarmTest {

    @Test
    public void testFeishuAlarm() {
        // 1. 创建一个任务信息对象
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(999); // 测试ID
        jobInfo.setJobGroup(1); // 执行器组ID
        jobInfo.setJobDesc("测试飞书告警任务");
        jobInfo.setAlarmEmail("test@example.com"); // 虽然是飞书告警，但系统仍然使用alarmEmail字段判断是否需要告警
        
        // 2. 创建一个任务日志对象，模拟任务执行失败
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setId(10000L); // 日志ID
        jobLog.setJobGroup(1);
        jobLog.setJobId(999);
        jobLog.setTriggerTime(new Date()); // 触发时间
        jobLog.setTriggerCode(ReturnT.FAIL_CODE); // 触发失败状态码 500
        jobLog.setTriggerMsg("手动触发的测试告警");
        jobLog.setExecutorHandler("testHandler");
        jobLog.setExecutorParam("testParam");
        
        // 3. 获取JobAlarmer并调用告警方法
        boolean alarmResult = XxlJobAdminConfig.getAdminConfig().getJobAlarmer().alarm(jobInfo, jobLog);
        
        System.out.println("告警结果: " + alarmResult);
    }
}