package BackForFinanceAnalyzer.demo.controllers;

import BackForFinanceAnalyzer.demo.models.Budget;
import BackForFinanceAnalyzer.demo.models.SavingsGoal;
import BackForFinanceAnalyzer.demo.models.Transaction;
import BackForFinanceAnalyzer.demo.repositories.BudgetRepository;
import BackForFinanceAnalyzer.demo.repositories.SavingsGoalRepository;
import BackForFinanceAnalyzer.demo.repositories.TransactionRepository;
import BackForFinanceAnalyzer.demo.repositories.UserRepository;
import BackForFinanceAnalyzer.demo.services.FinanceService;
import BackForFinanceAnalyzer.demo.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FinanceController {

    @Autowired private FinanceService financeService;
    @Autowired private ReportService reportService;
    @Autowired private TransactionRepository transactionRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private BudgetRepository budgetRepo;
    @Autowired private SavingsGoalRepository savingsGoalRepo;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(financeService.getUserDashboardData(userId));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            int count = financeService.processUpload(getUserId(request), file);
            return ResponseEntity.ok(Map.of("message", "Imported " + count + " transactions"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/transactions/manual")
    public ResponseEntity<?> addTransaction(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Transaction t = new Transaction();
        t.setUser(userRepository.findById(getUserId(request)).orElseThrow());
        t.setDate(LocalDate.parse(body.getOrDefault("date", LocalDate.now().toString())));
        t.setDescription(body.get("description"));
        t.setAmount(Double.parseDouble(body.getOrDefault("amount", "0")));
        t.setType(body.getOrDefault("type", "expense"));
        transactionRepo.save(t);
        return ResponseEntity.ok(Map.of("message", "Transaction added"));
    }

    @PostMapping("/budgets")
    public ResponseEntity<?> setBudget(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Budget b = budgetRepo.findByUserAndCategory(userRepository.findById(getUserId(request)).orElseThrow(), body.get("category"))
                .orElse(new Budget());
        b.setUser(userRepository.findById(getUserId(request)).orElseThrow());
        b.setCategory(body.get("category"));
        b.setLimitAmount(Double.parseDouble(body.get("limit")));
        budgetRepo.save(b);
        return ResponseEntity.ok(Map.of("message", "Budget saved"));
    }

    @PostMapping("/goals")
    public ResponseEntity<?> setGoal(@RequestBody Map<String, String> body, HttpServletRequest request) {
        SavingsGoal g = new SavingsGoal();
        g.setUser(userRepository.findById(getUserId(request)).orElseThrow());
        g.setName(body.get("name"));
        g.setTargetAmount(Double.parseDouble(body.get("target")));
        g.setCurrentAmount(Double.parseDouble(body.getOrDefault("current", "0")));
        if (body.containsKey("deadline") && !body.get("deadline").isEmpty()) {
            g.setDeadline(LocalDate.parse(body.get("deadline")));
        }
        savingsGoalRepo.save(g);
        return ResponseEntity.ok(Map.of("message", "Goal saved"));
    }

    @GetMapping("/health")
    public ResponseEntity<?> getHealth(HttpServletRequest request) {
        return ResponseEntity.ok(financeService.computeFinancialHealth(getUserId(request)));
    }

    @GetMapping("/forecast")
    public ResponseEntity<?> getForecast(HttpServletRequest request) {
        return ResponseEntity.ok(financeService.forecastExpenses(getUserId(request)));
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String reply = financeService.chatRespond(getUserId(request), body.get("message"));
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    @GetMapping("/reports/{kind}")
    public ResponseEntity<?> getReport(@PathVariable String kind, @RequestParam(defaultValue = "pdf") String format, HttpServletRequest request) {
        try {
            List<Transaction> txns = transactionRepo.findByUser(userRepository.findById(getUserId(request)).orElseThrow());
            File file;
            String type;
            if ("csv".equalsIgnoreCase(format)) {
                file = reportService.generateCsvReport(txns);
                type = "text/csv";
            } else if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
                file = reportService.generateExcelReport(txns);
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                List<String> summary = List.of("Total Transactions: " + txns.size());
                file = reportService.generatePdfReport("Finance Report (" + kind + ")", summary);
                type = "application/pdf";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.parseMediaType(type))
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
