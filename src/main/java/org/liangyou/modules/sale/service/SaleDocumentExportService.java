package org.liangyou.modules.sale.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.sale.vo.SaleOutDetailResponse;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class SaleDocumentExportService {
    private final SaleOutService orders;

    @Value("${sale.pdf.font-path:}") private String configuredFont;
    @Value("${sale.pdf.page-width-points:420}") private float pageWidth;

    public byte[] pdf(Long id) { return renderPdf(id, false); }

    public byte[] deliveryPdf(Long id) { return renderPdf(id, true); }

    private byte[] renderPdf(Long id, boolean delivery) {
        SaleOutDetailResponse order = exportable(id);
        if (delivery && !"delivery".equals(order.getFulfillmentType()))
            throw new BusinessException(409, "sale-order-not-delivery");
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FontHandle handle = loadFont(document);
            try (handle) {
                PdfLines page = new PdfLines(document, handle.font(), pageWidth);
                page.line("粮油配送中心销售清单", 15);
                page.line("客户：" + order.getCustomerName(), 11);
                page.line("录单：" + order.getRecordedTime() + "  编号：" + order.getOrderNo(), 9);
                page.line("状态：" + order.getOrderStatus() + "  打印：" + java.time.LocalDateTime.now(), 9);
                page.line("商品/规格   单位   数量   单价   金额", 9);
                page.line("----------------------------------------", 9);
                for (var item : order.getItems()) {
                    String name = item.getProductName() + (item.getSpec() == null ? "" : " " + item.getSpec());
                    for (String part : wrap(name, 22)) page.line(part, 10);
                    page.line("  " + item.getUnitName() + "   " + item.getQty() + "   " + item.getUnitPrice() + "   " + item.getAmount(), 10);
                    if (item.getRemark() != null && !item.getRemark().isBlank())
                        for (String part : wrap("备注：" + item.getRemark(), 32)) page.line(part, 9);
                }
                page.line("本单应收：" + order.getTotalAmount(), 12);
                page.line("经手人：" + safe(order.getHandlerName()) + "  电话：" + safe(order.getContactPhone()), 10);
                for (String part : wrap("地址：" + safe(order.getDeliveryAddress()), 32)) page.line(part, 10);
                if (delivery && order.getDeliveryRemark() != null && !order.getDeliveryRemark().isBlank())
                    for (String part : wrap("配送备注：" + order.getDeliveryRemark(), 32)) page.line(part, 10);
                if (delivery) {
                    page.line("收货人签字：________________", 10);
                    page.line("实际收货日期：________________", 10);
                }
                page.close();
                document.save(out);
            }
            orders.recordDocumentRequest(id, delivery ? "print_pdf" : "export_pdf");
            return out.toByteArray();
        } catch (IOException ex) { throw new BusinessException(500, "sale-export-failed"); }
    }

    private FontHandle loadFont(PDDocument document) throws IOException {
        List<String> paths = new ArrayList<>();
        if (configuredFont != null && !configuredFont.isBlank()) paths.add(configuredFont);
        paths.add("/System/Library/Fonts/Supplemental/Arial Unicode.ttf");
        paths.add("/Library/Fonts/Arial Unicode.ttf");
        paths.add("/System/Library/Fonts/PingFang.ttc");
        paths.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc");
        paths.add("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc");
        for (String path : paths) {
            if (!Files.isRegularFile(Path.of(path))) continue;
            if (path.endsWith(".ttc")) {
                TrueTypeCollection collection = new TrueTypeCollection(new File(path));
                final TrueTypeFont[] selected = {null};
                collection.processAllFonts(font -> { if (selected[0] == null && font.getUnicodeCmap() != null) selected[0] = font; });
                if (selected[0] != null) return new FontHandle(PDType0Font.load(document, selected[0], true), collection);
                collection.close();
            } else return new FontHandle(PDType0Font.load(document, new File(path)), null);
        }
        throw new IOException("CJK font unavailable; configure sale.pdf.font-path");
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private static List<String> wrap(String text, int chars) {
        List<String> parts = new ArrayList<>();
        int[] codepoints = text.codePoints().toArray();
        for (int i = 0; i < codepoints.length; i += chars)
            parts.add(new String(codepoints, i, Math.min(chars, codepoints.length - i)));
        if (parts.isEmpty()) parts.add("");
        return parts;
    }

    private record FontHandle(PDType0Font font, TrueTypeCollection collection) implements AutoCloseable {
        public void close() throws IOException { if (collection != null) collection.close(); }
    }

    private static final class PdfLines implements AutoCloseable {
        private final PDDocument document;
        private final PDType0Font font;
        private final float width;
        private PDPageContentStream stream;
        private float y;
        private int pageNumber;
        PdfLines(PDDocument document, PDType0Font font, float width) throws IOException {
            this.document = document; this.font = font; this.width = width; nextPage();
        }
        private void nextPage() throws IOException {
            if (stream != null) { footer(); stream.close(); }
            PDPage page = new PDPage(new PDRectangle(width, 595)); document.addPage(page);
            stream = new PDPageContentStream(document, page); y = 566; pageNumber++;
            if (pageNumber > 1) {
                direct("粮油配送中心销售清单（续）", 12);
                direct("商品/规格   单位   数量   单价   金额", 9);
            }
        }
        void line(String text, int size) throws IOException {
            StringBuilder part = new StringBuilder();
            for (int point : text.codePoints().toArray()) {
                String next = new String(Character.toChars(point));
                if (!part.isEmpty() && font.getStringWidth(part + next) * size / 1000 > width - 48) {
                    if (y < 55) nextPage();
                    direct(part.toString(), size);
                    part.setLength(0);
                }
                part.append(next);
            }
            if (y < 55) nextPage();
            direct(part.toString(), size);
        }
        private void direct(String text, int size) throws IOException {
            stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(24, y);
            stream.showText(text); stream.endText(); y -= Math.max(17, size + 6);
        }
        private void footer() throws IOException {
            stream.beginText(); stream.setFont(font, 9); stream.newLineAtOffset(width - 95, 23);
            stream.showText("第 " + pageNumber + " 页"); stream.endText();
        }
        public void close() throws IOException { if (stream != null) { footer(); stream.close(); stream = null; } }
    }


    public byte[] excel(Long id) {
        SaleOutDetailResponse order = exportable(id);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("销售清单");
            sheet.createRow(0).createCell(0).setCellValue("粮油配送中心销售清单");
            var customer = sheet.createRow(1); customer.createCell(0).setCellValue("客户"); customer.createCell(1).setCellValue(order.getCustomerName());
            var meta = sheet.createRow(2); meta.createCell(0).setCellValue("订单编号"); meta.createCell(1).setCellValue(order.getOrderNo());
            meta.createCell(2).setCellValue("录单时间"); meta.createCell(3).setCellValue(order.getRecordedTime().toString());
            var headers = sheet.createRow(3);
            String[] names = {"商品名称/规格", "单位", "数量", "单价", "金额", "备注"};
            for (int i = 0; i < names.length; i++) headers.createCell(i).setCellValue(names[i]);
            int rowNo = 4;
            for (var item : order.getItems()) {
                var row = sheet.createRow(rowNo++);
                row.createCell(0).setCellValue(item.getProductName() + (item.getSpec() == null ? "" : " " + item.getSpec()));
                row.createCell(1).setCellValue(item.getUnitName());
                row.createCell(2).setCellValue(item.getQty().doubleValue());
                row.createCell(3).setCellValue(item.getUnitPrice().doubleValue());
                row.createCell(4).setCellValue(item.getAmount().doubleValue());
                row.createCell(5).setCellValue(item.getRemark() == null ? "" : item.getRemark());
            }
            var total = sheet.createRow(rowNo++); total.createCell(0).setCellValue("本单应收"); total.createCell(4).setCellValue(order.getTotalAmount().doubleValue());
            var contact = sheet.createRow(rowNo++); contact.createCell(0).setCellValue("经手人");
            contact.createCell(1).setCellValue(order.getHandlerName() == null ? "" : order.getHandlerName());
            contact.createCell(2).setCellValue("电话"); contact.createCell(3).setCellValue(order.getContactPhone() == null ? "" : order.getContactPhone());
            var address = sheet.createRow(rowNo++); address.createCell(0).setCellValue("地址");
            address.createCell(1).setCellValue(order.getDeliveryAddress() == null ? "" : order.getDeliveryAddress());
            if ("delivery".equals(order.getFulfillmentType())) {
                var note = sheet.createRow(rowNo); note.createCell(0).setCellValue("配送备注");
                note.createCell(1).setCellValue(order.getDeliveryRemark() == null ? "" : order.getDeliveryRemark());
            }
            for (int i = 0; i < names.length; i++) sheet.setColumnWidth(i, i == 0 ? 9000 : 5000);
            workbook.write(out);
            orders.recordDocumentRequest(id, "export_excel");
            return out.toByteArray();
        } catch (IOException ex) { throw new BusinessException(500, "sale-export-failed"); }
    }

    public SaleOutDetailResponse exportable(Long id) {
        SaleOutDetailResponse order = orders.detail(id);
        if ("draft".equals(order.getOrderStatus()) || "cancelled".equals(order.getOrderStatus()))
            throw new BusinessException(409, "sale-order-not-exportable");
        return order;
    }
}
