package com.shangxu.crm.common.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * 极简 CSV 工具：导出带 UTF-8 BOM（Excel 直接打开不乱码），导入支持引号包裹与逗号转义。
 */
public final class CsvUtils {

    private CsvUtils() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    public static String line(Object... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells[i] == null ? "" : String.valueOf(cells[i])));
        }
        return sb.append("\r\n").toString();
    }

    /** 带 BOM 的 CSV 内容，Excel 打开中文不乱码 */
    public static byte[] withBom(String content) {
        return ("\uFEFF" + content).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static List<String[]> parse(String text) {
        List<String[]> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return rows;
        }
        if (text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(c);
                }
                continue;
            }
            if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                cells.add(cell.toString().trim());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                cells.add(cell.toString().trim());
                cell.setLength(0);
                if (!(cells.size() == 1 && cells.get(0).isEmpty())) {
                    rows.add(cells.toArray(new String[0]));
                }
                cells.clear();
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !cells.isEmpty()) {
            cells.add(cell.toString().trim());
            if (!(cells.size() == 1 && cells.get(0).isEmpty())) {
                rows.add(cells.toArray(new String[0]));
            }
        }
        return rows;
    }
}
