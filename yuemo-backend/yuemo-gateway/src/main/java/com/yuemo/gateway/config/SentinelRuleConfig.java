package com.yuemo.gateway.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelRuleConfig {

    @PostConstruct
    public void initRules() {
        initFlowRules();
        initDegradeRules();
    }

    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        rules.add(createFlowRule("api-user", 50));
        rules.add(createFlowRule("api-product", 200));
        rules.add(createFlowRule("api-order", 50));
        rules.add(createFlowRule("api-payment", 30));
        rules.add(createFlowRule("api-cart", 100));
        rules.add(createFlowRule("api-coupon", 50));
        rules.add(createFlowRule("api-admin", 35));

        FlowRuleManager.loadRules(rules);
    }

    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        String[] resources = {"api-user", "api-product", "api-order",
                "api-payment", "api-cart", "api-coupon", "api-admin"};

        for (String resource : resources) {
            DegradeRule rule = new DegradeRule(resource);
            rule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
            rule.setCount(1000);   // 最大 RT 1000ms
            rule.setTimeWindow(30); // 熔断时长 30s
            rule.setMinRequestAmount(10);
            rule.setSlowRatioThreshold(0.5);
            rules.add(rule);
        }

        DegradeRuleManager.loadRules(rules);
    }

    private FlowRule createFlowRule(String resource, int qps) {
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        return rule;
    }
}
