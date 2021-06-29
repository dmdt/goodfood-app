package pw.prsk.goodfood.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.annotation.LayoutRes
import pw.prsk.goodfood.R
import java.util.*

abstract class BaseAutocompleteAdapter<T>(
    context: Context,
    @LayoutRes val resource: Int,
    private val items: List<T>
) : ArrayAdapter<T>(context, resource, items), Filterable {
    private val suggestions: MutableList<T> = mutableListOf()

    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.lowercase(Locale.getDefault())
            val filterResults = FilterResults()

            filterResults.values = if (query.isNullOrEmpty()) {
                items
            } else {
                items.filter { getItemName(it).lowercase(Locale.getDefault()).contains(query) }
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            suggestions.clear()
            // Unchecked cast Any to List
            @Suppress("UNCHECKED_CAST")
            val list = (results?.values as List<*>).mapNotNull { it as? T }
            list.also { suggestions.addAll(it) }
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence {
            return getItemName(resultValue)
        }
    }

    override fun getFilter(): Filter {
        return filter
    }

    override fun getCount(): Int = suggestions.size

    override fun getItem(position: Int): T = suggestions[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder?
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(resource, null)
            viewHolder = ViewHolder(view.findViewById(R.id.tvItemText))
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        viewHolder.itemText.text = getItemName(suggestions[position])
        return view
    }

    abstract fun getItemName(item: Any?): String

    class ViewHolder(val itemText: TextView)
}
