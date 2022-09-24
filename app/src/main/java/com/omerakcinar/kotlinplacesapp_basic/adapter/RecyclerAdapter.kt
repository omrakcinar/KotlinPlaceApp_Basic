package com.omerakcinar.kotlinplacesapp_basic.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omerakcinar.kotlinplacesapp_basic.databinding.RecyclerRowBinding
import com.omerakcinar.kotlinplacesapp_basic.model.Place
import com.omerakcinar.kotlinplacesapp_basic.view.MainActivity
import com.omerakcinar.kotlinplacesapp_basic.view.MapsActivity

class RecyclerAdapter(val placeList : List<Place>) : RecyclerView.Adapter<RecyclerAdapter.PlaceHolder>() {
    class PlaceHolder(val recyclerRowBinding: RecyclerRowBinding) : RecyclerView.ViewHolder(recyclerRowBinding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceHolder {
        val recyclerRowBinding : RecyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PlaceHolder(recyclerRowBinding)
    }

    override fun onBindViewHolder(holder: PlaceHolder, position: Int) {
        holder.recyclerRowBinding.placeNameRowText.text = placeList[position].placeName
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context,MapsActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return placeList.size
    }
}