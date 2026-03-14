package BackForFinanceAnalyzer.demo.services;

import BackForFinanceAnalyzer.demo.models.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    public File generateCsvReport(List<Transaction> transactions) throws Exception {
        Path tempPath = Files.createTempFile("report_", ".csv");
        FileWriter out = new FileWriter(tempPath.toFile());
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("Date", "Description", "Merchant", "Amount", "Currency", "Type", "Category"))) {
            for (Transaction t : transactions) {
                printer.printRecord(t.getDate(), t.getDescription(), t.getMerchant(), t.getAmount(), t.getCurrency(), t.getType(), t.getCategory());
            }
        }
        return tempPath.toFile();
    }

    public File generateExcelReport(List<Transaction> transactions) throws Exception {
        Path tempPath = Files.createTempFile("report_", ".xlsx");
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(tempPath.toFile())) {
            Sheet sheet = workbook.createSheet("Transactions");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Description");
            header.createCell(2).setCellValue("Merchant");
            header.createCell(3).setCellValue("Amount");
            header.createCell(4).setCellValue("Currency");
            header.createCell(5).setCellValue("Type");
            header.createCell(6).setCellValue("Category");

            int rowNum = 1;
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.getDate().format(DateTimeFormatter.ISO_DATE));
                row.createCell(1).setCellValue(t.getDescription());
                row.createCell(2).setCellValue(t.getMerchant());
                row.createCell(3).setCellValue(t.getAmount());
                row.createCell(4).setCellValue(t.getCurrency());
                row.createCell(5).setCellValue(t.getType());
                row.createCell(6).setCellValue(t.getCategory());
            }
            workbook.write(out);
        }
        return tempPath.toFile();
    }

    public File generatePdfReport(String title, List<String> summaryLines) throws Exception {
        Path tempPath = Files.createTempFile("report_", ".pdf");
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        PdfWriter.getInstance(document, new FileOutputStream(tempPath.toFile()));
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font textFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

        document.add(new Paragraph(title, titleFont));
        document.add(new Paragraph(" ", textFont)); // spacer

        for (String line : summaryLines) {
            document.add(new Paragraph(line, textFont));
        }

        document.close();
        return tempPath.toFile();
    }
}
