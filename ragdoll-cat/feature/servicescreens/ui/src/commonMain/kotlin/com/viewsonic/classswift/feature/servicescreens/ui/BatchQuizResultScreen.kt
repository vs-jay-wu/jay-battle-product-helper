package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode

/**
 * One per-question column for [BatchQuizResultList], mirrors `item_batch_quiz_result.xml`:
 * a clickable title card (quiz no + category), three vertical bars (correct/incorrect/no-answer),
 * an accuracy percent, and a right divider. [submitted] is the per-question denominator that scales
 * the bars (matches `CSBatchQuizBarChart.setAnswerStatusCount(count, submittedStudentCount)`).
 */
data class BatchQuizResultItemUi(
    val quizNo: String,
    val category: String,
    val correct: Int,
    val incorrect: Int,
    val noAnswer: Int,
    val percent: String,
    val sequence: Int,
) {
    val submitted: Int get() = correct + incorrect + noAnswer
}

// batch_quiz_chart_bar_max_height
private val BarMaxHeight = 330.dp
// batch_quiz_result_item_width / batch_quiz_result_title_height
private val ItemWidth = 154.66.dp
private val TitleHeight = 53.33.dp

/**
 * CMP port of `BatchQuizResultWindow`'s RecyclerView (`rv_result_list`) — a horizontal list of
 * per-question result columns. Window chrome (title, legend, frozen Quiz/Bar Chart/Accuracy labels)
 * and the overlays (loading, retry, detail popup, toast, masks, offline) stay native around it.
 */
@Composable
fun BatchQuizResultList(
    items: List<BatchQuizResultItemUi> = emptyList(),
    onItemClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxSize().horizontalScroll(rememberScrollState()).designNode("batch_quiz_result_list")) {
        items.forEach { item -> BatchQuizResultItemView(item, onItemClick) }
    }
}

@Composable
private fun BatchQuizResultItemView(item: BatchQuizResultItemUi, onItemClick: (Int) -> Unit) {
    Row(Modifier.fillMaxHeight().designNode("bqr_item_${item.sequence}")) {
        Spacer(Modifier.width(6.66.dp))
        Column(Modifier.width(ItemWidth).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            // CSBatchQuizItemTitle: white rounded card (radius_400, neutral_500 border_200), clickable.
            Box(
                Modifier.fillMaxWidth().height(TitleHeight)
                    .clip(RoundedCornerShape(5.33.dp)).background(Color.White)
                    .border(0.66.dp, Neutral500, RoundedCornerShape(5.33.dp))
                    .clickable { onItemClick(item.sequence) }.designNode("bqr_title_${item.sequence}"),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(Modifier.padding(top = 6.66.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.quizNo, color = Neutral900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(item.category, color = Neutral900, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.66.dp))
                }
            }
            // Bars: correct / incorrect / no-answer, 36dp wide, 10.66dp apart, bottom-aligned.
            Row(
                Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Bar(item.correct, item.submitted, Green500)
                Spacer(Modifier.width(10.66.dp))
                Bar(item.incorrect, item.submitted, Red400)
                Spacer(Modifier.width(10.66.dp))
                Bar(item.noAnswer, item.submitted, BorderC2C2C2)
            }
            // tv_answer_percent
            Box(Modifier.fillMaxWidth().height(TitleHeight), contentAlignment = Alignment.Center) {
                Text(item.percent, color = Neutral900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(6.66.dp))
        // view_divider: 0.66dp, neutral_500, full height
        Box(Modifier.width(0.66.dp).fillMaxHeight().background(Neutral500))
    }
}

@Composable
private fun Bar(count: Int, submitted: Int, color: Color) {
    Column(Modifier.width(36.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        val barHeight = if (count >= 1 && submitted >= 1) (count * BarMaxHeight.value / submitted) else 0f
        if (barHeight > 0f) Box(Modifier.fillMaxWidth().height(barHeight.dp).background(color))
        Spacer(Modifier.height(6.66.dp))
        Text("$count", color = Neutral900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.66.dp))
    }
}
