package com.shangxu.crm.common.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvUtilsTest {

    @Test
    @DisplayName("含逗号、引号、换行的字段应被正确转义并可回读")
    void escapeAndParse() {
        String content = CsvUtils.line("客户名称", "备注")
                + CsvUtils.line("测试,科技", "他说\"很好\"")
                + CsvUtils.line("换行", "第一行\n第二行");
        List<String[]> rows = CsvUtils.parse(content);
        assertEquals(3, rows.size());
        assertEquals("测试,科技", rows.get(1)[0]);
        assertEquals("他说\"很好\"", rows.get(1)[1]);
        assertEquals("第一行\n第二行", rows.get(2)[1]);
    }

    @Test
    @DisplayName("导出内容带 UTF-8 BOM，Excel 打开不乱码")
    void withBom() {
        byte[] bytes = CsvUtils.withBom(CsvUtils.line("客户名称", "级别"));
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("客户名称"));
    }

    @Test
    @DisplayName("空行与结尾空行不应产生脏数据")
    void ignoreBlankLines() {
        List<String[]> rows = CsvUtils.parse("a,b\r\n\r\nc,d\r\n");
        assertEquals(2, rows.size());
    }
}
