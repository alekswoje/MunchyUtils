package munchyutils.client;

public class BalanceParser {
    public static double parseBalance(String balanceStr) {
        if (balanceStr == null) return 0.0;
        balanceStr = balanceStr.replace(",", "").trim();
        double multiplier = 1.0;
        if (balanceStr.endsWith("B")) {
            multiplier = 1_000_000_000;
            balanceStr = balanceStr.substring(0, balanceStr.length() - 1);
        } else if (balanceStr.endsWith("M")) {
            multiplier = 1_000_000;
            balanceStr = balanceStr.substring(0, balanceStr.length() - 1);
        } else if (balanceStr.endsWith("K")) {
            multiplier = 1_000;
            balanceStr = balanceStr.substring(0, balanceStr.length() - 1);
        }
        try {
            return Double.parseDouble(balanceStr) * multiplier;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
} 