package com.epmus.mobile.program

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
            ITEM_MAP[programItem.id] = programItem
        }
    }

    private fun createProgramItem(position: Int, name: String): ProgramItem {
        return ProgramItem(position.toString(), name, makeDetails(position))
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Détails à propos de Exercice: ").append(position)
        for (i in 0..position - 1) {
            builder.append("\nPlus de détails ici.")
        }
        return builder.toString()
    }

    data class ProgramItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }
}