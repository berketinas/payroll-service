package com.ember.payroll.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SalaryController {
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
        // CALCULATE GROSS WITH NORMAL FORMULA
        double estimatedGross = net / (1 - (MUL_EMPLOYEE_SSI
                + MUL_EMPLOYEE_UNEMPLOYMENT
                + MUL_TAX_BRACKETS.get(bracket) * (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT))
                + MUL_STAMP));
        double base = estimatedGross * (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT));

        // IF NEW CUMULATIVE BASE DOES NOT EXCEED THE BREAKPOINT,
        // CALCULATED GROSS IS ACCURATE
        if(cumulativeBase + base <= BRKPNTS_TAX_BRACKETS.get(bracket)) {
            grossAccumulator += estimatedGross;
            cumulativeBase += base;

            return Map.of("gross", grossAccumulator, "cumulative", cumulativeBase);
        } else {

            // RECALCULATE AVAILABLE GROSS FOR THIS BRACKET BASED ON AVAILABLE BASE
            double availableBase = BRKPNTS_TAX_BRACKETS.get(bracket) - cumulativeBase;
            double availableGross = availableBase / (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT));

            // CONVERT GROSS ACCOUNTED FOR TO NET TO SUBTRACT FROM THE RECURSION CALL
            double availableNet = availableGross - (availableGross * (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT) + availableBase * MUL_TAX_BRACKETS.get(bracket) + availableGross * MUL_STAMP);

            grossAccumulator += availableGross;
            return underSSI_MAX_netToGross(net - availableNet, bracket + 1, BRKPNTS_TAX_BRACKETS.get(bracket), grossAccumulator);
        }
    }

    // ***
    // METHOD IS CALLED WHEN A GROSS CALCULATED FROM NET BOTH EXCEEDS MAXIMUM SSI, AND MOVES UP
    // ONE OR MORE BRACKETS. SINCE THERE IS NO FORMULA POSSIBLE, A BRUTE FORCE BINARY SEARCH
    // IS APPLIED TO FIND THE CORRESPONDING GROSS FOR A NET.
    // ***
    private Map<String, Double> overSSI_MAX_netToGross(double net, int bracket, double cumulativeBase) {

        // SSI_EMPLOYEE_SHARE + UNEMPLOYMENT_EMPLOYEE_SHARE
        double maxSU = SSI_MAX * (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT);

        // UPPER AND LOWER BOUNDS OF SEARCH AREA
        double upperBound;
        double lowerBound = 0.00d;

        // THE "INDEX" OF THE SEARCH, REPRESENTS THE BASE VALUE
        // FOR THE NET SALARY WE ARE TRYING TO FIND
        double converger;

        // VALUE TO COMPARE "NET" AGAINST
        double tester;

        // FIND THE BRACKET WHOSE BREAKPOINT WILL BE USED AS THE UPPER BOUND
        // MOST OPTIMAL BREAKPOINT IS THE FIRST BREAKPOINT WE ENCOUNTER WHOSE CORRESPONDING
        // NET VALUE IS GREATER THAN THE NET VALUE WE ARE LOOKING FOR
        do {
            bracket++;
            converger = BRKPNTS_TAX_BRACKETS.get(bracket);
            tester = grossToNet(converger + maxSU, BRKPNTS_TAX_BRACKETS.get(bracket), converger, bracket);
        } while(net > tester && bracket < 5);

        upperBound = BRKPNTS_TAX_BRACKETS.get(bracket);

        // LIMIT THE SEARCH, AND ALLOW SOME DESIRED OUTPUT-ACTUAL OUTPUT DIFFERENCE TOLERANCE
        int limit = 0;
        while(limit < 50 && (tester < net - 0.1 || tester > net + 0.1)) {

            // TESTER IS DIRECTLY PROPORTIONATE TO CONVERGER, MEANING IF TESTER IS GREATER
            // THAN NET, THEN THE BASE VALUE WE ARE LOOKING FOR IS SMALLER THAN CONVERGER
            // AND VICE VERSA
            if(tester > net) {
                upperBound = converger;
            } else {
                lowerBound = converger;
            }

            // RESET CONVERGER TO THE MIDDLE OF THE SEARCH AREA
            converger = (upperBound + lowerBound) / 2;

            // FIND TESTER FOR NEW VALUE OF CONVERGER
            tester = grossToNet(converger + maxSU, cumulativeBase + converger, converger, bracket);
            limit++;
        }

        System.out.println("BINARY SEARCH FOUND GROSS IN " + limit + " ITERATIONS");

        // CONVERGER WAS THE BASE, SO ADD SSI AND UNEMPLOYMENT SHARES ON TOP TO GET GROSS
        double estimatedGross = converger + maxSU;
        return Map.of("gross", estimatedGross, "cumulative", cumulativeBase + converger);
    }

    private Map<String, Double> netToGross(double net, int bracket, double cumulativeBase) {
        // CALCULATE GROSS WITH NORMAL FORMULA
        double estimatedGross = net / (1 - (MUL_EMPLOYEE_SSI
                                    + MUL_EMPLOYEE_UNEMPLOYMENT
                                    + MUL_TAX_BRACKETS.get(bracket) * (1 - (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT))
                                    + MUL_STAMP));
        double maxSU = SSI_MAX * (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT);
        double base;

        // IF SSI_EMPLOYEE_SHARE + UNEMPLOYMENT_EMPLOYEE_SHARE IS GREATER THAN MAXIMUM,
        if(estimatedGross * (MUL_EMPLOYEE_SSI + MUL_EMPLOYEE_UNEMPLOYMENT) > maxSU) {

            // READJUST CALCULATED GROSS ACCORDING TO MAXIMUM VALUES, CALCULATE BASE WITH NEW GROSS
            estimatedGross = (net + maxSU - maxSU * MUL_TAX_BRACKETS.get(bracket)) / (1 - (MUL_TAX_BRACKETS.get(bracket) + MUL_STAMP));
            base = estimatedGross - maxSU;

            if(getBracket(cumulativeBase + base) == bracket) {
                // EXCEEDS SSI MAX, BUT NO BRACKET CHANGE
                return Map.of("gross", estimatedGross, "cumulative", cumulativeBase + base);
            } else {
                // EXCEEDS SSI MAX, AND BRACKET CHANGES
                return overSSI_MAX_netToGross(net, bracket, cumulativeBase);
            }
        } else {

            // CALCULATE BASE WITHOUT READJUSTING GROSS
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

    private LinkedHashMap<String, Double> trGrossToNet_INNER(LinkedHashMap<String, Double> yearlyReport) {
        double cumulativeBase = 0.00d;

        for(String month: yearlyReport.keySet()) {
            if(yearlyReport.get(month) > MIN_GROSS) {
                double base = getBase(yearlyReport.get(month));
                cumulativeBase += base;

                int bracket = getBracket(cumulativeBase);

                // CALCULATE BASE NET, AND APPLY MINIMUM WAGE PERSONAL INCOME & STAMP TAX EXEMPTION ON TOP
                yearlyReport.put(month, applyExemption(grossToNet(yearlyReport.get(month), cumulativeBase, base, bracket)));
            } else {
                yearlyReport.put(month, 0.00d);
            }
        }

        return yearlyReport;
    }

    private LinkedHashMap<String, Double> trNetToGross_INNER(LinkedHashMap<String, Double> yearlyReport) {
        double cumulativeBase = 0.00d;

        for(String month: yearlyReport.keySet()) {
            if(yearlyReport.get(month) > MIN_NET) {
                int bracket = getBracket(cumulativeBase);

                // UNDO THE EXEMPTION APPLIED TO MINIMUM WAGE PERSONAL INCOME & STAMP TAX
                double baseNet = yearlyReport.get(month) - (getPIT(0.00d, MIN_NET, 1, 0.00d) + getStamp(MIN_GROSS));
                Map<String, Double> output = netToGross(baseNet, bracket, cumulativeBase);

                yearlyReport.put(month, output.get("gross"));
                cumulativeBase = output.get("cumulative");
            } else {
                yearlyReport.put(month, 0.00d);
            }
        }

        return yearlyReport;
    }

    private LinkedHashMap<String, Double> trGrossToCost_INNER(LinkedHashMap<String, Double> yearlyReport) {
        for(String month: yearlyReport.keySet()) {
            if(yearlyReport.get(month) > MIN_GROSS) {
                // COST IS GROSS + SSI_EMPLOYER_SHARE + UNEMPLOYMENT_EMPLOYER_SHARE
                double cost = yearlyReport.get(month) > SSI_MAX ? (SSI_MAX * (MUL_EMPLOYER_SSI + MUL_EMPLOYER_UNEMPLOYMENT)) : (yearlyReport.get(month) * (MUL_EMPLOYER_SSI + MUL_EMPLOYER_UNEMPLOYMENT));
                yearlyReport.put(month, yearlyReport.get(month) + cost);
            } else {
                yearlyReport.put(month, 0.00d);
            }
        }

        return yearlyReport;
    }

    // ROUTE TO TEST WHETHER LOADED PROPERTIES ARE CORRECT
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

    // GROSS SALARY TO NET SALARY CONVERSION
    @GetMapping("/tr/gross-to-net")
    public LinkedHashMap<String, Double> trGrossToNet(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
        return trGrossToNet_INNER(yearlyReport);
    }

    // NET SALARY TO GROSS SALARY CONVERSION
    @GetMapping("/tr/net-to-gross")
    public LinkedHashMap<String, Double> trNetToGross(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
        return trNetToGross_INNER(yearlyReport);
    }

    // NET SALARY TO EMPLOYER COST CONVERSION
    @GetMapping("/tr/net-to-cost")
    public LinkedHashMap<String, Double> trNetToCost(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
        return trGrossToCost_INNER(trNetToGross_INNER(yearlyReport));
    }

    // GROSS SALARY TO EMPLOYER COST CONVERSION
    @GetMapping("/tr/gross-to-cost")
    public LinkedHashMap<String, Double> trGrossToCost(@RequestBody LinkedHashMap<String, Double> yearlyReport) {
        return trGrossToCost_INNER(yearlyReport);
    }
}
