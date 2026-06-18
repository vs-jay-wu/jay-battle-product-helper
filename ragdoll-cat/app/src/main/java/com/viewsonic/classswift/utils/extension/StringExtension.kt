package com.viewsonic.classswift.utils.extension

fun String.omit(maxLength: Int): String =
    if (this.length > maxLength) "${this.take(maxLength)}..." else this

fun String.capitalizeFirstLetter(): String =
    this.replaceFirstChar {
        // 判断是否为小写字母，如果是则转换为大写，否则保持不变
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

fun String.isLatexContent(): Boolean {
    val latexPatterns = listOf(
        Regex("""\$\$[\s\S]+?\$\$"""),      // $$...$$ (display)
        Regex("""\$[^$]+\$"""),             // $...$ (inline)
        Regex("""\\\([\s\S]+?\\\)"""),      // \( ... \)
        Regex("""\\\[[\s\S]+?\\]"""),       // \[ ... \]
        Regex("""\\begin\{[^}]+\}"""),      // \begin{...}
        Regex("""\\[A-Za-z]+""")            // \sqrt, \frac, \sum, ...
    )

    return latexPatterns.any { it.containsMatchIn(this) }
}