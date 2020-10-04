package com.epmus.mobile.program_fragment

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.epmus.mobile.R
import com.epmus.mobile.poseestimation.CameraActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_new_program.*
import kotlinx.android.synthetic.main.program_row_new_program.view.*

class NewProgramActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_program)

        supportActionBar?.title = "Select User"

        val adapter = GroupAdapter<ViewHolder>()

        enumValues<ExerciceNameList>().forEach {
            adapter.add(ProgramItem(it.exerciceName))
        }

        recyclerView_Program.adapter = adapter

        adapter.setOnItemClickListener{item, view ->
            val programItem = item as ProgramItem

            val exerciceData = ExerciceData.getExerciceData(ExerciceNameList.getEnumValue(programItem.text))

            val intent = Intent(view.context,
                CameraActivity::class.java)
            intent.putExtra("exercice", exerciceData.exercice)

            startActivity(intent)

            finish()
        }
    }
}

class ProgramItem(val text: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.Username_textView_newProgram.text = text
    }

    override fun getLayout(): Int {
        return R.layout.program_row_new_program
    }
}