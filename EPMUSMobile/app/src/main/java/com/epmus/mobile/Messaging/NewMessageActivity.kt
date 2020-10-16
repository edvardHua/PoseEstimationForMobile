package com.epmus.mobile.Messaging

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.epmus.mobile.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*

class NewMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        supportActionBar?.title = "Select User"

        val adapter = GroupAdapter<ViewHolder>()

        adapter.add(UserItem())

        recyclerView_newMessage.adapter = adapter

        adapter.setOnItemClickListener { item, view ->
            val userItem = item as UserItem

            val intent = Intent(
                view.context,
                Chatlog::class.java
            )

            startActivity(intent)

            finish()
        }
    }
}

class UserItem : Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.Username_textView_newMessage.text = "Physiothérapeute"
    }

    override fun getLayout(): Int {
        return R.layout.user_row_new_message
    }
}