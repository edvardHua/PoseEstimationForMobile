package com.edvard.poseestimation.Messaging

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.edvard.poseestimation.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chatlog.*
import kotlinx.android.synthetic.main.chat_from_row.view.textView

class Chatlog : AppCompatActivity() {

    companion object{
        val TAG = "ChatLog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatlog)

        supportActionBar?.title = "Chat Log"

        setupDummyData()

        send_button_chat_log.setOnClickListener{
            Log.d(TAG, "Attemp to send message...")
        }
    }


    private fun setupDummyData(){
        val adapter = GroupAdapter<ViewHolder>()

        adapter.add(ChatToItem("Bonjour! J'ai de la difficulté à bien comprendre comment faire l'exercice pour mon biceps."))
        adapter.add(ChatFromItem("Appelez-moi, je vais vous aider!"))
        adapter.add(ChatToItem("Bonjour! J'ai perdu mon papier pour notre prochain rendez-vous, pouvez-vous me rappeler la date? Merci"))
        adapter.add(ChatFromItem("Pas de problème, c'est vendredi prochain à 13h!"))
        adapter.add(ChatToItem("Parfait! Merci beaucoup!"))
        adapter.add(ChatToItem("À vendredi prochain!"))
        adapter.add(ChatFromItem("À bientôt!"))

        recyclerview_chat_log.adapter = adapter
    }
}

class ChatFromItem(val text: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textView.text = text
    }
    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

}

class ChatToItem(val text: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textView.text = text
    }
    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }

}