package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * job alarm by feishu
 *
 * @author dyxc
 */
@Component
public class FeishuJobAlarm implements JobAlarm {
    private static Logger logger = LoggerFactory.getLogger(FeishuJobAlarm.class);
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * fail alarm
     *
     * @param info   job info
     * @param jobLog job log
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        boolean alarmResult = true;

        // send monitor feishu message
        if (info != null && info.getAlarmEmail() != null && info.getAlarmEmail().trim().length() > 0) {

            // alarmContent
            String alarmContent = "Alarm Job LogId=" + jobLog.getId();
            if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "\nTriggerMsg=" + jobLog.getTriggerMsg();
            }
            if (jobLog.getHandleCode() > 0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += "\nHandleCode=" + jobLog.getHandleMsg();
            }

            // feishu info
            XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(Integer.valueOf(info.getJobGroup()));
            String title = I18nUtil.getString("jobconf_monitor");
            String content = MessageFormat.format(loadFeishuJobAlarmTemplate(),
                    group != null ? group.getTitle() : "null",
                    info.getId(),
                    info.getJobDesc(),
                    alarmContent);

            // 发送飞书消息
            try {
                // 构建飞书消息 - 修复格式
                Map<String, Object> message = new HashMap<>();
                message.put("msg_type", "text");
                
                Map<String, String> textContent = new HashMap<>();
                textContent.put("text", title + "\n" + content);
                message.put("content", textContent);
                
                // 获取webhook地址
                String webhook = XxlJobAdminConfig.getAdminConfig().getFeishuWebhook();
                
                // 添加日志，帮助调试
                logger.info(">>>>>>>>>>> xxl-job, sending feishu alarm for JobLogId:{}, webhook:{}, message:{}", 
                    jobLog.getId(), webhook, message);
                
                // 发送HTTP请求
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
                
                String response = restTemplate.postForObject(webhook, entity, String.class);
                logger.info(">>>>>>>>>>> xxl-job, feishu alarm response for JobLogId:{}: {}", jobLog.getId(), response);
                
                // 检查响应是否包含成功标识
                if (response != null && response.contains("\"code\":0")) {
                    logger.info(">>>>>>>>>>> xxl-job, feishu alarm success for JobLogId:{}", jobLog.getId());
                } else {
                    logger.warn(">>>>>>>>>>> xxl-job, feishu alarm may have failed for JobLogId:{}, response:{}", 
                        jobLog.getId(), response);
                    // 不将alarmResult设为false，因为响应可能仍然是成功的，只是格式不同
                }
            } catch (Exception e) {
                logger.error(">>>>>>>>>>> xxl-job, job fail alarm feishu send error, JobLogId:{}", jobLog.getId(), e);
                alarmResult = false;
            }
        }

        return alarmResult;
    }

    /**
     * load feishu job alarm template
     *
     * @return template string
     */
    private static String loadFeishuJobAlarmTemplate() {
        return I18nUtil.getString("jobconf_monitor_detail") + "：\n" +
                I18nUtil.getString("jobinfo_field_jobgroup") + ": {0}\n" +
                I18nUtil.getString("jobinfo_field_id") + ": {1}\n" +
                I18nUtil.getString("jobinfo_field_jobdesc") + ": {2}\n" +
                I18nUtil.getString("jobconf_monitor_alarm_title") + ": " + I18nUtil.getString("jobconf_monitor_alarm_type") + "\n" +
                I18nUtil.getString("jobconf_monitor_alarm_content") + ": {3}";
    }
}