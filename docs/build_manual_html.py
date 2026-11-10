"""由 使用手册.md 生成可转换为 Word 的排版 HTML。

用法: python docs/build_manual_html.py
产物: docs/使用手册.html
后续用 workbuddy-docx 的 html-to-doc/scripts/convert_doc.py 转 docx。
"""
import argparse
import html
import io
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "使用手册.md")
DST = os.path.join(HERE, "使用手册.html")


def inline(text):
    text = html.escape(text)
    text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"`(.+?)`", r"<code>\1</code>", text)
    return text


def parse(markdown):
    body, toc, section, subsection = [], [], 0, 0
    table, code, code_buf, in_list = [], False, [], False

    def flush_table():
        nonlocal table
        if not table:
            return
        rows = []
        for i, line in enumerate(table):
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            tag = "th" if i == 0 else "td"
            rows.append("<tr>" + "".join("<%s>%s</%s>" % (tag, inline(c), tag) for c in cells) + "</tr>")
        body.append("<table>" + "\n".join(rows) + "</table>")
        table = []

    def flush_list():
        nonlocal in_list
        if in_list:
            body.append("</ul>")
            in_list = False

    for raw in markdown.split("\n"):
        line = raw.rstrip()
        if line.startswith("```"):
            if code:
                body.append('<pre class="code">' + html.escape("\n".join(code_buf)) + "</pre>")
                code_buf, code = [], False
            else:
                flush_table()
                flush_list()
                code = True
            continue
        if code:
            code_buf.append(raw)
            continue
        if line.startswith("|"):
            flush_list()
            if not re.match(r"^\|[\s\-:|]+\|$", line):
                table.append(line)
            continue
        flush_table()
        if not line.strip() or line.strip() == "---":
            flush_list()
            continue
        if line.startswith("# "):
            continue
        if line.startswith("## "):
            flush_list()
            section, subsection = section + 1, 0
            text = line[3:].strip()
            body.append('<h1 id="section-%d">%s</h1>' % (section, inline(text)))
            toc.append((1, "section-%d" % section, text))
            continue
        if line.startswith("### "):
            flush_list()
            subsection += 1
            text = line[4:].strip()
            body.append('<h2 id="section-%d-%d">%s</h2>' % (section, subsection, inline(text)))
            toc.append((2, "section-%d-%d" % (section, subsection), text))
            continue
        if line.startswith("- "):
            if not in_list:
                body.append("<ul>")
                in_list = True
            body.append("<li>%s</li>" % inline(line[2:]))
            continue
        flush_list()
        cls = ' class="step"' if re.match(r"^\d+\.\s", line) else ""
        body.append("<p%s>%s</p>" % (cls, inline(line)))
    flush_table()
    flush_list()
    return "\n".join(body), toc


def build_toc(toc):
    out = ['<nav class="doc-toc" aria-label="文档目录">', '<p class="toc-title">目录</p>', '<ol class="toc-list">']
    open_sub = False
    for level, anchor, text in toc:
        if level == 1:
            if open_sub:
                out.append("</ol></li>")
                open_sub = False
            out.append('<li><a href="#%s">%s</a>' % (anchor, html.escape(text)))
        else:
            if not open_sub:
                out.append('<ol class="toc-sub">')
                open_sub = True
            out.append('<li><a href="#%s">%s</a></li>' % (anchor, html.escape(text)))
    if open_sub:
        out.append("</ol>")
    out.append("</li></ol></nav>")
    return "\n".join(out)


CSS = """
@page { size: A4; margin: 2.2cm 2.0cm; @bottom-center { content: "第 " counter(page) " 页"; font-family: "SimSun"; font-size: 9pt; color: #8b97a8; } @top-right { content: "__HEADER__"; font-family: "SimSun"; font-size: 8.5pt; color: #b0bac9; } }
@page cover { margin: 0; @bottom-center { content: none; } @top-right { content: none; } }
section[role="cover"] { page: cover; }
body { font-family: "SimSun", "宋体", serif; font-size: 12pt; line-height: 1.75; color: #1f2a3d; }
p { text-indent: 2em; margin: 0 0 6pt 0; text-align: justify; }
h1 { font-family: "SimHei", "黑体", sans-serif; font-size: 17pt; color: #16233b; margin: 20pt 0 10pt 0; padding-bottom: 4pt; border-bottom: 1.2pt solid #2f6bff; text-indent: 0; }
h2 { font-family: "SimHei", "黑体", sans-serif; font-size: 13.5pt; color: #1f2a3d; margin: 14pt 0 6pt 0; text-indent: 0; }
li { font-size: 12pt; margin-bottom: 3pt; line-height: 1.7; }
table { width: 100%; border-collapse: collapse; margin: 8pt 0 10pt 0; font-size: 10.5pt; }
th { background-color: #eef3ff; color: #16233b; border: 0.5pt solid #c9d6ef; padding: 5pt 6pt; text-align: left; font-family: "SimHei", "黑体", sans-serif; }
td { border: 0.5pt solid #d8e0ee; padding: 5pt 6pt; vertical-align: top; }
pre.code { background-color: #f5f8fd; border-left: 2pt solid #2f6bff; padding: 8pt 10pt; font-family: "Consolas", monospace; font-size: 10pt; text-indent: 0; }
.cover { padding: 5.2cm 2.4cm 0 2.4cm; }
.cover-kicker { font-family: "SimHei", "黑体", sans-serif; font-size: 11pt; color: #2f6bff; letter-spacing: 2pt; text-indent: 0; }
.cover-title { font-family: "SimHei", "黑体", sans-serif; font-size: 34pt; color: #16233b; margin: 14pt 0 8pt 0; line-height: 1.3; text-indent: 0; }
.cover-sub { font-size: 14pt; color: #5b6779; margin-bottom: 30pt; text-indent: 0; }
.cover-rule { width: 90pt; height: 3pt; background-color: #2f6bff; margin-bottom: 26pt; }
.cover-meta { font-size: 11pt; color: #5b6779; line-height: 2; text-indent: 0; }
.toc-title { font-family: "SimHei", "黑体", sans-serif; font-size: 20pt; color: #16233b; margin: 0 0 18pt 0; text-indent: 0; }
.toc-list, .toc-list ol, .toc-list ul { list-style: none; padding-left: 0; margin: 0; }
.toc-list > li { font-size: 12pt; margin-bottom: 7pt; }
.toc-sub { padding-left: 22pt; margin-top: 4pt; }
.toc-sub li { font-size: 10.5pt; color: #5b6779; margin-bottom: 3pt; }
a { color: #1f2a3d; text-decoration: none; }
.step { text-indent: 0; padding-left: 2em; }
"""


def main():
    parser = argparse.ArgumentParser(description="Markdown 转可转 Word 的排版 HTML")
    parser.add_argument("--source", default=SRC, help="源 Markdown 路径")
    parser.add_argument("--output", default=DST, help="输出 HTML 路径")
    parser.add_argument("--title", default="商序云 · 销售运营一体化平台 使用手册")
    parser.add_argument("--header", default="商序云 · 使用手册 v4.0")
    parser.add_argument("--kicker", default="SHANGXU CRM · PRODUCT MANUAL")
    parser.add_argument("--cover-title", default="商序云<br/>销售运营一体化平台<br/>使用手册")
    parser.add_argument("--cover-sub", default="从线索到回款，一条链路管到底")
    parser.add_argument("--cover-meta", default="版本：v4.0（产品目录增强版）<br/>发布日期：2026 年 11 月<br/>适用对象：销售专员 / 销售经理 / 销售总监 / 财务 / 系统管理员")
    args = parser.parse_args()

    md = io.open(args.source, encoding="utf-8").read()
    body, toc = parse(md)
    doc = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8" />
<title>__DOC_TITLE__</title>
<style>%s</style>
</head>
<body>
<section role="cover">
  <div class="cover">
    <p class="cover-kicker">__KICKER__</p>
    <p class="cover-title">__COVER_TITLE__</p>
    <div class="cover-rule"></div>
    <p class="cover-sub">__COVER_SUB__</p>
    <p class="cover-meta">__COVER_META__</p>
  </div>
</section>
<section role="cover">
%s
</section>
<section class="chapter" data-page-restart="1">
%s
</section>
</body>
</html>""" % (CSS.replace("__HEADER__", args.header), build_toc(toc), body)
    doc = (doc.replace("__DOC_TITLE__", args.title)
              .replace("__KICKER__", args.kicker)
              .replace("__COVER_TITLE__", args.cover_title)
              .replace("__COVER_SUB__", args.cover_sub)
              .replace("__COVER_META__", args.cover_meta))
    io.open(args.output, "w", encoding="utf-8", newline="").write(doc)
    print("生成成功: %s (%d 字符, %d 个一级章节)" % (args.output, len(doc), len([x for x in toc if x[0] == 1])))


if __name__ == "__main__":
    main()
