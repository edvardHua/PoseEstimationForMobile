package com.epmus.mobile.program

import com.epmus.mobile.program_fragment.ExerciceNameList
import java.util.ArrayList
import java.util.HashMap

object ProgramContent {

    val ITEMS: MutableList<ProgramItem> = ArrayList()
    val ITEM_MAP: MutableMap<String, ProgramItem> = HashMap()

    init {
        var id = 1
        enumValues<ExerciceNameList>().forEach {
            var programItem = createProgramItem(id++, it.exerciceName)
            ITEMS.add(programItem)
            ITEM_MAP.put(programItem.id, programItem)
        }
    }

    private fun createProgramItem(position: Int, name: String): ProgramItem {
        return ProgramItem(position.toString(), name, makeDetails(position))
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position)
        for (i in 0..position - 1) {
            builder.append("\nMore details information here.")
        }
        return builder.toString()
    }

    data class ProgramItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }
}