package BackForFinanceAnalyzer.demo.services;

import BackForFinanceAnalyzer.demo.models.*;
import BackForFinanceAnalyzer.demo.repositories.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    @Autowired private TransactionRepository transactionRepo;
    @Autowired private BudgetRepository budgetRepo;
    @Autowired private SavingsGoalRepository savingsGoalRepo;
    @Autowired private UserRepository userRepository;

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "Food and Dining", List.of("swiggy", "zomato", "restaurant", "cafe", "food", "pizza"),
            "Transportation", List.of("uber", "ola", "fuel", "metro", "bus", "train", "cab"),
            "Shopping", List.of("amazon", "flipkart", "myntra", "mall", "shopping"),
            "Bills and Utilities", List.of("electricity", "water", "gas", "wifi", "broadband", "mobile"),
            "Entertainment", List.of("netflix", "spotify", "prime video", "cinema", "movie"),
            "Healthcare", List.of("pharmacy", "hospital", "clinic", "medical"),
            "Investments", List.of("mutual fund", "stock", "sip", "investment")
    );

    public int processUpload(Long userId, MultipartFile file) throws Exception {
        User user = userRepository.findById(userId).orElseThrow();
        int count = 0;
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        
        if (filename.endsWith(".csv")) {
            InputStreamReader reader = new InputStreamReader(file.getInputStream());
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().parse(reader);
            for (CSVRecord record : records) {
                // Try to find headers
                String dateStr = getMappedValue(record, "date");
                String descStr = getMappedValue(record, "description", "details");
                String amountStr = getMappedValue(record, "amount");
                count += processSingleLine(user, dateStr, descStr, amountStr);
            }
        } else if (filename.endsWith(".xlsx")) {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            int dateIdx = -1, descIdx = -1, amountIdx = -1;
            
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String val = headerRow.getCell(i).getStringCellValue().toLowerCase();
                if (val.contains("date")) dateIdx = i;
                if (val.contains("description") || val.contains("details")) descIdx = i;
                if (val.contains("amount")) amountIdx = i;
            }
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String dateStr = row.getCell(dateIdx) != null ? row.getCell(dateIdx).toString() : null;
                String descStr = row.getCell(descIdx) != null ? row.getCell(descIdx).toString() : null;
                String amountStr = row.getCell(amountIdx) != null ? row.getCell(amountIdx).toString() : null;
                count += processSingleLine(user, dateStr, descStr, amountStr);
            }
            workbook.close();
        } else {
            throw new Exception("Unsupported file format");
        }
        return count;
    }

    private String getMappedValue(CSVRecord record, String... possibleKeys) {
        for (String key : possibleKeys) {
            try {
                return record.get(key);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private int processSingleLine(User user, String dateStr, String descStr, String amountStr) {
        if (dateStr == null || descStr == null || amountStr == null) return 0;
        try {
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                date = LocalDate.now(); // Fallback for simplicity
            }
            double val = Double.parseDouble(amountStr);
            String type = val > 0 ? "income" : "expense";
            double amount = Math.abs(val);
            String merchant = descStr.split("-")[0].trim();
            if (merchant.length() > 120) merchant = merchant.substring(0, 120);
            
            String category = "Others";
            String lowerDesc = descStr.toLowerCase();
            for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
                for (String kw : entry.getValue()) {
                    if (lowerDesc.contains(kw)) {
                        category = entry.getKey();
                        break;
                    }
                }
            }

            Transaction t = new Transaction();
            t.setUser(user);
            t.setDate(date);
            t.setDescription(descStr);
            t.setMerchant(merchant);
            t.setAmount(amount);
            t.setType(type);
            t.setCategory(category);
            transactionRepo.save(t);
            return 1;
        } catch (Exception e) {
            return 0; // Skip invalid lines
        }
    }

    public Map<String, Object> getUserDashboardData(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Transaction> txns = transactionRepo.findByUser(user);
        if (txns.isEmpty()) return Map.of("has_data", false);

        // Compute aggregations
        Map<String, Double> byCategory = txns.stream().filter(t -> "expense".equals(t.getType()))
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));

        List<Map<String, Object>> byCategoryList = byCategory.entrySet().stream()
                .map(e -> Map.<String, Object>of("category", e.getKey(), "abs_amount", e.getValue()))
                .collect(Collectors.toList());

        Map<YearMonth, Double> byMonthExp = txns.stream().filter(t -> "expense".equals(t.getType()))
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate()), Collectors.summingDouble(Transaction::getAmount)));
        Map<YearMonth, Double> byMonthInc = txns.stream().filter(t -> "income".equals(t.getType()))
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate()), Collectors.summingDouble(Transaction::getAmount)));

        List<Map<String, Object>> incomeVsExpList = new ArrayList<>();
        Set<YearMonth> allMonths = new HashSet<>();
        allMonths.addAll(byMonthExp.keySet());
        allMonths.addAll(byMonthInc.keySet());
        allMonths.stream().sorted().forEach(ym -> {
            incomeVsExpList.add(Map.<String, Object>of(
                    "month", ym.atDay(1).toString(),
                    "income", byMonthInc.getOrDefault(ym, 0.0),
                    "expenses", byMonthExp.getOrDefault(ym, 0.0)
            ));
        });

        List<Budget> budgets = budgetRepo.findByUser(user);
        List<Map<String, Object>> budgetStatus = new ArrayList<>();
        for (Budget b : budgets) {
            double spent = byCategory.getOrDefault(b.getCategory(), 0.0);
            String status = "OK";
            if (spent >= b.getLimitAmount()) status = "Over Limit";
            else if (spent >= 0.8 * b.getLimitAmount()) status = "Near Limit";
            budgetStatus.add(Map.<String, Object>of("category", b.getCategory(), "limit", b.getLimitAmount(), "spent", spent, "status", status));
        }

        List<SavingsGoal> goals = savingsGoalRepo.findByUser(user);
        List<Map<String, Object>> goalsData = goals.stream().map(g -> {
            double progress = g.getTargetAmount() > 0 ? (g.getCurrentAmount() / g.getTargetAmount() * 100) : 0;
            return Map.<String, Object>of(
                    "name", g.getName(),
                    "target", g.getTargetAmount(),
                    "current", g.getCurrentAmount(),
                    "progress", (double) Math.round(progress * 10) / 10,
                    "deadline", g.getDeadline() != null ? g.getDeadline().toString() : ""
            );
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("has_data", true);
        result.put("by_category", byCategoryList);
        result.put("income_vs_expenses", incomeVsExpList);
        result.put("budget_status", budgetStatus);
        result.put("goals", goalsData);
        return result;
    }

    public Map<String, Object> forecastExpenses(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Transaction> expenses = transactionRepo.findByUserAndType(user, "expense");
        if (expenses.isEmpty()) return Map.of("message", "No expense data yet.");

        Map<YearMonth, Double> monthly = expenses.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate()), Collectors.summingDouble(Transaction::getAmount)));

        List<YearMonth> sortedMonths = monthly.keySet().stream().sorted().collect(Collectors.toList());
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < sortedMonths.size(); i++) {
            regression.addData(i, monthly.get(sortedMonths.get(i)));
        }

        double nextMonthPred = regression.getN() > 1 ? regression.predict(sortedMonths.size()) : monthly.values().stream().mapToDouble(d->d).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("next_month_total", nextMonthPred);
        result.put("message", "Forecast based on " + sortedMonths.size() + " months of data.");
        return result;
    }

    public Map<String, Object> computeFinancialHealth(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Transaction> txns = transactionRepo.findByUser(user);
        if (txns.isEmpty()) return Map.of("score", 50, "details", "No transactions yet. Add data to see a full score.");

        double expenses = txns.stream().filter(t -> "expense".equals(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double income = txns.stream().filter(t -> "income".equals(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double savings = Math.max(income - expenses, 0);

        double savingsRate = income > 0 ? savings / income : 0;
        double expIncomeRatio = income > 0 ? expenses / income : 1;

        int score = 50;
        if (savingsRate >= 0.3) score += 25;
        else if (savingsRate >= 0.15) score += 15;
        else if (savingsRate >= 0.05) score += 5;

        if (expIncomeRatio <= 0.6) score += 25;
        else if (expIncomeRatio <= 0.8) score += 15;
        else if (expIncomeRatio <= 1.0) score += 5;

        score = Math.max(10, Math.min(100, score));

        String band = "Needs Improvement";
        if (score >= 80) band = "Excellent";
        else if (score >= 60) band = "Good";
        else if (score >= 40) band = "Moderate";

        String details = String.format("Income: ₹%.2f, Expenses: ₹%.2f, Savings rate: %.1f%%, Expense/Income: %.1f%%. Overall financial health: %s.",
                income, expenses, savingsRate * 100, expIncomeRatio * 100, band);

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("details", details);
        return result;
    }

    public String chatRespond(Long userId, String message) {
        // Fallback or simple mock version of the chat response logic
        // We will provide simple regex-like matches to replicate the Python chat bot functionality
        String msg = message == null ? "" : message.toLowerCase();
        User user = userRepository.findById(userId).orElseThrow();
        List<Transaction> txns = transactionRepo.findByUser(user);
        if (txns.isEmpty()) return "I do not see any transactions yet. Please upload a statement or add some data first.";
        
        if (msg.contains("save more") || msg.contains("how can i save")) {
            Map<String, Object> h = computeFinancialHealth(userId);
            return "Your current financial health score is " + h.get("score") + ". " + h.get("details") + " To save more, try reducing discretionary categories like Shopping or Entertainment by 10–20% and increase your monthly savings transfer.";
        }
        if (msg.contains("predict") && msg.contains("next month")) {
            Map<String, Object> fc = forecastExpenses(userId);
            if (!fc.containsKey("next_month_total")) return "I need more expense history before I can forecast.";
            return String.format("Based on your history, I estimate your total expenses next month to be around ₹%.2f.", (Double) fc.get("next_month_total"));
        }
        
        Map<String, Object> health = computeFinancialHealth(userId);
        return "Here is a quick overview. Score: " + health.get("score") + ". " + health.get("details") + " Ask me things like 'predict my spending for next month'.";
    }
}
