package com.example.vulpinetasks

data class TableData(
    val rows: Int,
    val cols: Int,
    val headers: List<String> = emptyList(),
    val cells: List<List<String>> = emptyList()
)