package pw.prsk.goodfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pw.prsk.goodfood.adapters.ProductAdapter

class ProductsFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_products, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rvProducts: RecyclerView = view.findViewById(R.id.rvProductsList)
        rvProducts.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = ProductAdapter()
        }
        super.onViewCreated(view, savedInstanceState)
    }
}