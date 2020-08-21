package com.hm.myapplication2.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.hm.myapplication2.R
import com.hm.myapplication2.databinding.ListItemDeviceBinding
import com.hm.myapplication2.server.DeviceData

/**
 * Created by Harry Mehta on 07/08/20 at 1:38 PM
 */
class DeviceListAdapter() : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    private var deviceList: ArrayList<DeviceData>? = null
    private var onItemClickListener: onItemClick? = null

    public interface onItemClick {
        fun onClick(data: DeviceData)
    }

    constructor(deviceList: ArrayList<DeviceData>, onItemClickListener: onItemClick?) : this() {

        this.deviceList = deviceList
        this.onItemClickListener = onItemClickListener

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ListItemDeviceBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.getContext()),
            R.layout.list_item_device, parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = deviceList!!.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(deviceList!![position])


    inner class ViewHolder(val binding: ListItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DeviceData) {
            with(binding) {

                tvName.text = item.name +"   ${item.endpoint}"

                this.root.setOnClickListener {

                    onItemClickListener?.onClick(item)

                }

            }
        }

    }
}