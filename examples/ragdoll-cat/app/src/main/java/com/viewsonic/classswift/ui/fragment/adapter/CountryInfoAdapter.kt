package com.viewsonic.classswift.ui.fragment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewAccountInfoItemBinding

class CountryInfoAdapter(private val context: Context, private val items: List<Pair<String, String>>) : BaseAdapter() {

    private var selectedPosition: Int = 0

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            ViewAccountInfoItemBinding.inflate(inflater, parent, false)
        } else {
            ViewAccountInfoItemBinding.bind(convertView)
        }
        binding.tvName.text = items[position].second
        if (position == selectedPosition) {
            binding.ivCheck.visibility = View.VISIBLE
            binding.tvName.setTextColor(ContextCompat.getColor(context, R.color.brand_blue))
        } else {
            binding.ivCheck.visibility = View.GONE
            binding.tvName.setTextColor(ContextCompat.getColor(context, R.color.account_info_item_color))
        }
        return binding.root
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }
}