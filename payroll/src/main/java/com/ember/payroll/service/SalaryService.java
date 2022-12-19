package com.ember.payroll.service;

import com.ember.payroll.model.PayloadDTO;
import com.ember.payroll.model.ResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SalaryService {
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

    public void testProperties() {
        System.out.println("MUL_TAX_BRACKETS: " + MUL_TAX_BRACKETS.get(1) + " - " + MUL_TAX_BRACKETS.get(2) + " - " + MUL_TAX_BRACKETS.get(3) + " - " + MUL_TAX_BRACKETS.get(4) + " - " + MUL_TAX_BRACKETS.get(5));
        System.out.println("BRKPNTS_TAX_BRACKETS: " + BRKPNTS_TAX_BRACKETS.get(1) + " - " + BRKPNTS_TAX_BRACKETS.get(2) + " - " + BRKPNTS_TAX_BRACKETS.get(3) + " - " + BRKPNTS_TAX_BRACKETS.get(4));
        System.out.println("EMPLOYER SSI, UNEMPLOYMENT: " + MUL_EMPLOYER_SSI + " - " + MUL_EMPLOYER_UNEMPLOYMENT);
        System.out.println("EMPLOYEE SSI, UNEMPLOYMENT: " + MUL_EMPLOYEE_SSI + " - " + MUL_EMPLOYEE_UNEMPLOYMENT);
        System.out.println("MINIMUM WAGE GROSS, NET: " + MIN_GROSS + " - " + MIN_NET);
        System.out.println("SSI GROSS MAX, MIN: " + SSI_MAX + " - " + SSI_MIN);
        System.out.println("MUL_STAMP: " + MUL_STAMP);
    }

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

    private double getPIT(double cumulativeBase, double base, int bracket, double taxAccumulator, ResponseDTO month) {
        if(month != null) {
            month.addBracket(bracket);
        }

        if(bracket > 1 && cumulativeBase - base <= BRKPNTS_TAX_BRACKETS.get(bracket - 1)) {
            double overflow = cumulativeBase - BRKPNTS_TAX_BRACKETS.get(bracket - 1);
            taxAccumulator += overflow * MUL_TAX_BRACKETS.get(bracket);

            return getPIT(BRKPNTS_TAX_BRACKETS.get(bracket - 1), base - overflow, bracket - 1, taxAccumulator, month);
        } else {
            taxAccumulator += base * MUL_TAX_BRACKETS.get(bracket);
            return taxAccumulator > 0 ? taxAccumulator : 0;
        }
    }

    private double applyExemption(double net, ResponseDTO month) {
        double exempt_income_tax = getPIT(0.00d, MIN_NET, 1, 0.00d, null);
        double exempt_stamp_tax = getStamp(MIN_GROSS);
        month.setMin_wage_exempt_tax(exempt_income_tax + exempt_stamp_tax);

        return net + exempt_income_tax + exempt_stamp_tax;
    }

    private double grossToNet(double gross, double cumulativeBase, double base, int bracket, ResponseDTO month) {
        double ssi_employee = getSSIEmployee(gross);
        double unemployment_employee = getUnemploymentEmployee(gross);
        double income_tax = getPIT(cumulativeBase, base, bracket, 0.00d, month);
        double stamp_tax = getStamp(gross);

        if(month != null) {
            month.setSsi_employee(ssi_employee);
            month.setUnemployment_employee(unemployment_employee);
            month.setIncome_tax(income_tax);
            month.setStamp_tax(stamp_tax);
        }

        return gross - (ssi_employee + unemployment_employee + income_tax + stamp_tax);
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
            tester = grossToNet(converger + maxSU, BRKPNTS_TAX_BRACKETS.get(bracket), converger, bracket, null);
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
            tester = grossToNet(converger + maxSU, cumulativeBase + converger, converger, bracket, null);
            limit++;
        }

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

    public List<ResponseDTO> trGrossToNet_INNER(PayloadDTO payload) {
        List<Double> year = payload.getYearlyReport();
        List<ResponseDTO> yearlyReport = new ArrayList<>();
        double cumulativeBase = 0.00d;

        for(int i = 0; i < 12; i++) {
            ResponseDTO month = new ResponseDTO();

            if(year.get(i) > MIN_GROSS) {
                month.setGross(year.get(i));

                double base = getBase(year.get(i));
                cumulativeBase += base;

                int bracket = getBracket(cumulativeBase);

                // CALCULATE BASE NET, AND APPLY MINIMUM WAGE PERSONAL INCOME & STAMP TAX EXEMPTION ON TOP
                month.setNet(applyExemption(grossToNet(year.get(i), cumulativeBase, base, bracket, month), month));
            } else {
                month.nullifyAll();
            }

            yearlyReport.add(month);
        }

        return yearlyReport;
    }

    public LinkedHashMap<String, Double> trNetToGross_INNER(LinkedHashMap<String, Double> yearlyReport) {
        double cumulativeBase = 0.00d;

        for(String month: yearlyReport.keySet()) {
            if(yearlyReport.get(month) > MIN_NET) {
                int bracket = getBracket(cumulativeBase);

                // UNDO THE EXEMPTION APPLIED TO MINIMUM WAGE PERSONAL INCOME & STAMP TAX
                double baseNet = yearlyReport.get(month) - (getPIT(0.00d, MIN_NET, 1, 0.00d, null) + getStamp(MIN_GROSS));
                Map<String, Double> output = netToGross(baseNet, bracket, cumulativeBase);

                yearlyReport.put(month, output.get("gross"));
                cumulativeBase = output.get("cumulative");
            } else {
                yearlyReport.put(month, 0.00d);
            }
        }

        return yearlyReport;
    }

    public LinkedHashMap<String, Double> trGrossToCost_INNER(LinkedHashMap<String, Double> yearlyReport) {
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
}
