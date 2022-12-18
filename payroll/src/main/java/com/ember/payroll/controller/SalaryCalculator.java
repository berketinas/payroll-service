package com.ember.payroll.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
public class SalaryCalculator {
    @Value("${income.ssi-max}")
    private double SSI_MAX;

    @Value("${income.ssi-min}")
    private double SSI_MIN;

    @Value("${income.min-gross}")
    private double MIN_GROSS;

    @Value("${income.min-net}")
    private double MIN_NET;

    @Value("${multipliers.employee.ssk}")
    private double MUL_EMPLOYEE_SSK;

    @Value("${multipliers.employee.unemployment}")
    private double MUL_EMPLOYEE_UNEMPLOYMENT;

    @Value("${multipliers.employer.ssk}")
    private double MUL_EMPLOYER_SSK;

    @Value("${multipliers.employer.unemployment}")
    private double MUL_EMPLOYER_UNEMPLOYMENT;

    @Value("#{${multipliers.tax-brackets}}")
    private HashMap<Integer, Double> MUL_TAX_BRACKETS;

    @Value("#{${breakpoints.tax-brackets}}")
    private HashMap<Integer, Double> BRKPNTS_TAX_BRACKETS;

    @GetMapping("/")
    public void testProperties() {
        System.out.println("MUL_TAX_BRACKETS: " + MUL_TAX_BRACKETS.get(1) + " - " + MUL_TAX_BRACKETS.get(2) + " - " + MUL_TAX_BRACKETS.get(3) + " - " + MUL_TAX_BRACKETS.get(4) + " - " + MUL_TAX_BRACKETS.get(5));
        System.out.println("BRKPNTS_TAX_BRACKETS: " + BRKPNTS_TAX_BRACKETS.get(1) + " - " + BRKPNTS_TAX_BRACKETS.get(2) + " - " + BRKPNTS_TAX_BRACKETS.get(3) + " - " + BRKPNTS_TAX_BRACKETS.get(4));
        System.out.println("EMPLOYER SSK, UNEMPLOYMENT: " + MUL_EMPLOYER_SSK + " - " + MUL_EMPLOYER_UNEMPLOYMENT);
        System.out.println("EMPLOYEE SSK, UNEMPLOYMENT: " + MUL_EMPLOYEE_SSK + " - " + MUL_EMPLOYEE_UNEMPLOYMENT);
        System.out.println("MINIMUM WAGE GROSS, NET: " + MIN_GROSS + " - " + MIN_NET);
        System.out.println("SSI GROSS MAX, MIN: " + SSI_MAX + " - " + SSI_MIN);
    }
}
