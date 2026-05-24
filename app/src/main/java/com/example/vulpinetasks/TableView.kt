package com.example.vulpinetasks.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.vulpinetasks.R

class TableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        setPadding(8, 8, 8, 8)
    }

    fun setTableData(rows: Int, cols: Int, headers: List<String> = emptyList(), cells: List<List<String>> = emptyList()) {
        removeAllViews()

        // Заголовок таблицы
        if (headers.isNotEmpty()) {
            val headerRow = createRow()
            for (c in 0 until cols) {
                val headerCell = createCell(
                    text = headers.getOrElse(c) { "Заголовок ${c + 1}" },
                    isHeader = true
                )
                headerRow.addView(headerCell)
            }
            addView(headerRow)
        }

        // Тело таблицы
        for (r in 0 until rows) {
            val bodyRow = createRow()
            for (c in 0 until cols) {
                val cellValue = cells.getOrNull(r)?.getOrNull(c) ?: "Ячейка ${c + 1}"
                val bodyCell = createCell(text = cellValue, isHeader = false)
                bodyRow.addView(bodyCell)
            }
            addView(bodyRow)
        }
    }

    private fun createRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createCell(text: String, isHeader: Boolean): TextView {
        return TextView(context).apply {
            this.text = text
            setPadding(16, 12, 16, 12)
            gravity = Gravity.CENTER_VERTICAL

            val params = LinearLayout.LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            )
            params.setMargins(1, 1, 1, 1)
            layoutParams = params

            if (isHeader) {
                setBackgroundColor(ContextCompat.getColor(context, R.color.table_header_bg))
                setTextColor(ContextCompat.getColor(context, R.color.table_header_text))
                textSize = 14f
            } else {
                setBackgroundColor(ContextCompat.getColor(context, R.color.table_cell_bg))
                setTextColor(ContextCompat.getColor(context, R.color.table_cell_text))
                textSize = 14f
            }
        }
    }
}