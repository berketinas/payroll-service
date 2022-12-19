package com.ember.payroll.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SalaryCalculator {
//    private static Gson JSONMapper;
    @Value("${income.ssi-max}")
    private double SSI_MAX;

    @Value("${income.ssi-min}")
    private double SSI_MIN;

    @Value("${income.min-gross}")
    private double MIN_GROSS;

    @Value("${income.min-net}")
    private double MIN_NET;

    @Value("${multipliers.employee.ssi}")
    private double MUL_EMPLOYEE_SSI;

    @Value("${multipliers.employee.unemployment}")
    private double MUL_EMPLOYEE_UNEMPLOYMENT;

    @Value("${multipliers.employer.ssi}")
    private double MUL_EMPLOYER_SSI;

    @Value("${multipliers.employer.unemployment}")
    private double MUL_EMPLOYER_UNEMPLOYMENT;

    @Value("${multipliers.stamp}")
    private double MUL_STAMP;

    @Value("#{${multipliers.tax-brackets}}")
    private HashMap<Integer, Double> MUL_TAX_BRACKETS;

    @Value("#{${breakpoints.tax-brackets}}")
    private HashMap<Integer, Double> BRKPNTS_TAX_BRACKETS;

    // IF JSON CONVERSION IS REQUIRED
//    public SalaryCalculator(Gson JSONMapper) {
//        SalaryCalculator.JSONMapper = JSONMapper;
//    }

    private double getSSIEmployee(double gross) {
        return MUL_EMPLOYEE_SSI * (gross > SSI_MAX ? SSI_MAX : gross);
    }

    private double getUnemploymentEmployee(double gross) {
        return MUL_EMPLOYEE_UNEMPLOYMENT * (gross > SSI_MAX ? SSI_MAX : gross);
    }

    private double getBase(double gross) {
        return gross - (getSSIEmployee(gross) + getUnemploymentEmployee(gross));
    }

    private double getStamp(double gross) {
        return gross * MUL_STAMP;
    }

    private int getBracket(double cumulativeBase) {
        for(int i = 1; i <= BRKPNTS_TAX_BRACKETS.size(); i++) {
            if(cumulativeBase <= BRKPNTS_TAX_BRACKETS.get(i)) {
                return i;
            }
        }

        return -1;
    }

    private double getPIT(double cumulativeBase, double base, int bracket, double taxAccumulator) {
        if(bracket > 1 && cumulativeBase - base <= BRKPNTS_TAX_BRACKETS.get(bracket - 1)) {
            double overflow = cumulativeBase - BRKPNTS_TAX_BRACKETS.get(bracket - 1);
            taxAccumulator += overflow * MUL_TAX_BRACKETS.get(bracket);

            return getPIT(BRKPNTS_TAX_BRACKETS.get(bracket - 1), base - overflow, bracket - 1, taxAccumulator);
        } else {
            taxAccumulator += base * MUL_TAX_BRACKETS.get(bracket);
            return taxAccumulator > 0 ? taxAccumulator : 0;
        }
    }

    private double applyExemption(double net) {
        return net + (getPIT(0.00d, MIN_NET, 1, 0.00d) + getStamp(MIN_GROSS));
    }

    private double grossToNet(double gross, double cumulativeBase, double base, int bracket) {
        return gross - (getSSIEmployee(gross) + getUnemploymentEmployee(gross) + getPIT(cumulativeBase, base, bracket, 0.00d) + getStamp(gross));
    }

    private Map<String, Double> underSSI_MAX_netToGross(double net, int bracket, double cumulativeBase, double grossAccumulator) {
        double estimatedGross = net / (1 - (MUL_EMPLOYEE_SSI
                + MUL_EMPLOYEE_UNEMPLOYMENT
                + MUL_TAX_BRACKETS.get(bracket) * (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT))
                + MUL_STAMP));
        double base = estimatedGross * (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT));

        if(cumulativeBase + base <= BRKPNTS_TAX_BRACKETS.get(bracket)) {
            grossAccumulator += estimatedGross;
            cumulativeBase += base;

            return Map.of("gross", grossAccumulator, "cumulative", cumulativeBase);
        } else {
            double availableBase = BRKPNTS_TAX_BRACKETS.get(bracket) - cumulativeBase;
            double availableGross = availableBase / (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT));

            double availableNet = availableGross - (availableGross * (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT) + availableBase * MUL_TAX_BRACKETS.get(bracket) + availableGross * MUL_STAMP);

            grossAccumulator += availableGross;
            return underSSI_MAX_netToGross(net - availableNet, bracket + 1, BRKPNTS_TAX_BRACKETS.get(bracket), grossAccumulator);
        }
    }

    private Map<String, Double> overSSI_MAX_netToGross(double net, int bracket, double cumulativeBase) {
        double maxSU = SSI_MAX * (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT);

        double upperBound;
        double lowerBound = 0.00d;
        double converger;
        double tester;

        do {
            bracket++;
            converger = BRKPNTS_TAX_BRACKETS.get(bracket);
            tester = grossToNet(converger + maxSU, BRKPNTS_TAX_BRACKETS.get(bracket), converger, bracket);
        } while(net > tester && bracket < 5);

        upperBound = BRKPNTS_TAX_BRACKETS.get(bracket);

        int limit = 0;
        while(limit < 100 && (tester < net - 0.1 || tester > net + 0.1)) {
            if(tester > net) {
                upperBound = converger;
            } else {
                lowerBound = converger;
            }

            converger = (upperBound + lowerBound) / 2;
            tester = grossToNet(converger + maxSU, cumulativeBase + converger, converger, bracket);
            limit++;
        }

        double estimatedGross = converger + maxSU;
        return Map.of("gross", estimatedGross, "cumulative", cumulativeBase + converger);
    }

    private Map<String, Double> netToGross(double net, int bracket, double cumulativeBase) {
        double estimatedGross = net / (1 - (MUL_EMPLOYEE_SSI
                                    + MUL_EMPLOYEE_UNEMPLOYMENT
                                    + MUL_TAX_BRACKETS.get(bracket) * (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT))
                                    + MUL_STAMP));
        double availableSU = SSI_MAX * (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT);
        double base;

        if(estimatedGross * (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT) > availableSU) {
            estimatedGross = (net + availableSU - availableSU * MUL_TAX_BRACKETS.get(bracket)) / (1 - (MUL_TAX_BRACKETS.get(bracket) + MUL_STAMP));
            base = estimatedGross - availableSU;

            if(getBracket(cumulativeBase + base) == bracket) {
                // EXCEEDS SSI MAX, BUT NO BRACKET CHANGE
                return Map.of("gross", estimatedGross, "cumulative", cumulativeBase + base);
            } else {
                // EXCEEDS SSI MAX, AND BRACKET CHANGES
                return overSSI_MAX_netToGross(net, bracket, cumulativeBase);
            }
        } else {
            base = estimatedGross * (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT));

            if(getBracket(cumulativeBase + base) == bracket) {
                // DOES NOT EXCEED SSI MAX, AND NO BRACKET CHANGE
                return Map.of("gross", estimatedGross, "cumulative", cumulativeBase + base);
            } else {
                // DOES NOT EXCEED SSI MAX, BUT BRACKET CHANGES
                return underSSI_MAX_netToGross(net, bracket, cumulativeBase, 0.00d);
            }
        }
    }

    @GetMapping("/")
    public void testProperties() {
        System.out.println("MUL_TAX_BRACKETS: " + MUL_TAX_BRACKETS.get(1) + " - " + MUL_TAX_BRACKETS.get(2) + " - " + MUL_TAX_BRACKETS.get(3) + " - " + MUL_TAX_BRACKETS.get(4) + " - " + MUL_TAX_BRACKETS.get(5));
        System.out.println("BRKPNTS_TAX_BRACKETS: " + BRKPNTS_TAX_BRACKETS.get(1) + " - " + BRKPNTS_TAX_BRACKETS.get(2) + " - " + BRKPNTS_TAX_BRACKETS.get(3) + " - " + BRKPNTS_TAX_BRACKETS.get(4));
        System.out.println("EMPLOYER SSI, UNEMPLOYMENT: " + MUL_EMPLOYER_SSI + " - " + MUL_EMPLOYER_UNEMPLOYMENT);
        System.out.println("EMPLOYEE SSI, UNEMPLOYMENT: " + MUL_EMPLOYEE_SSI + " - " + MUL_EMPLOYEE_UNEMPLOYMENT);
        System.out.println("MINIMUM WAGE GROSS, NET: " + MIN_GROSS + " - " + MIN_NET);
        System.out.println("SSI GROSS MAX, MIN: " + SSI_MAX + " - " + SSI_MIN);
        System.out.println("MUL_STAMP: " + MUL_STAMP);
    }

    @GetMapping("/tr/gross-to-net")
    public LinkedHashMap<String, Double> trGrossToNet(@RequestBody LinkedHashMap<String, Double> yearlyGross) {
        LinkedHashMap<String, Double> yearlyNet = new LinkedHashMap<>();
        double cumulativeBase = 0.00d;

        for(String month: yearlyGross.keySet()) {
            if(yearlyGross.get(month) >= MIN_GROSS) {
                double base = getBase(yearlyGross.get(month));
                cumulativeBase += base;

                int bracket = getBracket(cumulativeBase);

                yearlyNet.put(month, applyExemption(grossToNet(yearlyGross.get(month), cumulativeBase, base, bracket)));
            } else {
                yearlyNet.put(month, 0.00d);
            }
        }

        return yearlyNet;
    }

    @GetMapping("/tr/net-to-gross")
    public LinkedHashMap<String, Double> trNetToGross(@RequestBody LinkedHashMap<String, Double> yearlyNet) {
        LinkedHashMap<String, Double> yearlyGross = new LinkedHashMap<>();
        double cumulativeBase = 0.00d;

        for(String month: yearlyNet.keySet()) {
            if(yearlyNet.get(month) >= MIN_NET) {
                int bracket = getBracket(cumulativeBase);
                double baseNet = yearlyNet.get(month) - (getPIT(0.00d, MIN_NET, 1, 0.00d) + getStamp(MIN_GROSS));

                Map<String, Double> output = netToGross(baseNet, bracket, cumulativeBase);

                yearlyGross.put(month, output.get("gross"));
                cumulativeBase = output.get("cumulative");
            } else {
                yearlyGross.put(month, 0.00d);
            }
        }

        return yearlyGross;
    }
}
