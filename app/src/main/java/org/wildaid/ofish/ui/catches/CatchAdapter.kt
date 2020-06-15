package org.wildaid.ofish.ui.catches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wildaid.ofish.R
import org.wildaid.ofish.databinding.ItemEditCatchBinding
import org.wildaid.ofish.ui.base.AdapterDiffCallback
import org.wildaid.ofish.ui.base.PhotoItem
import org.wildaid.ofish.util.setVisible


class CatchAdapter(
    private val dataList: ArrayList<CatchItem> = ArrayList(),
    private val searchListener: (id: Int, CatchItem) -> Unit,
    private val catchEditModeListener: (CatchItem) -> Unit,
    private val catchAddAttachmentListener: (CatchItem) -> Unit,
    private val catchRemoveListener: (Int) -> Unit,
    private val catchRemoveNoteListener: (CatchItem) -> Unit,
    private val catchRemovePhotoListener: (PhotoItem, CatchItem) -> Unit
) : RecyclerView.Adapter<CatchAdapter.CatchViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return dataList[position].catch.hashCode().toLong()
    }

    fun setItems(items: List<CatchItem>) {
        val diffResult = DiffUtil.calculateDiff(AdapterDiffCallback(items, dataList))
        diffResult.dispatchUpdatesTo(this)
        dataList.clear()
        dataList.addAll(items)
    }

    override fun getItemCount() = dataList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CatchViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_edit_catch,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: CatchViewHolder, position: Int) {
        holder.bindItem(dataList[position])
    }

    inner class CatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var currentItem: CatchItem

        private val weightString = view.resources.getString(R.string.weight)
        private val countString = view.resources.getString(R.string.count)

        private val catchEditBinding: ItemEditCatchBinding =
            ItemEditCatchBinding.bind(view).apply {
                holder = this@CatchViewHolder
            }

        fun bindItem(item: CatchItem) {
            catchEditBinding.holder = this
            currentItem = item


            // Edit Views
            val editVisible = item.inEditMode
            initUnitSpinner()

            catchEditBinding.catchEditGroup.setVisible(editVisible)
            catchEditBinding.catchNoteLayout.setVisible(editVisible && item.attachmentItem.hasNotes())

            catchEditBinding.catchEditPhotos.onPhotoRemoveListener = {
                catchRemovePhotoListener.invoke(it, item)
            }

            catchEditBinding.catchNoteLayout.setEndIconOnClickListener {
                catchRemoveNoteListener.invoke(item)
            }

            catchEditBinding.catchActionRemove.apply {
                text = context.getString(R.string.remove_catch, adapterPosition.inc())
            }
            catchEditBinding.catchEditGroupWeight.setVisible(
                item.amount.contains(
                    weightString,
                    true
                ) && editVisible
            )
            catchEditBinding.countEditLayout.setVisible(
                item.amount.contains(
                    countString,
                    true
                ) && editVisible
            )

            // View groups
            val viewVisible = !item.inEditMode
            catchEditBinding.catchViewGroup.setVisible(viewVisible)
            catchEditBinding.catchViewLayout.reportSpecies.text = item.catch.fish
            catchEditBinding.catchViewLayout.catchViewAttachments.attachmentNoteGroup
                .setVisible(currentItem.attachmentItem.hasNotes())

            if (item.catch.unit.isBlank() || item.catch.weight <= 0) {
                catchEditBinding.catchViewLayout.reportCatchAmountType1.setText(R.string.count)
                catchEditBinding.catchViewLayout.reportCatchAmount1.text = item.catch.number.toString()
            } else {
                catchEditBinding.catchViewLayout.reportCatchAmountType1.setText(R.string.weight)
                catchEditBinding.catchViewLayout.reportCatchAmount1.text =
                    "${item.catch.weight} ${item.catch.unit}"

                if (item.catch.number > 0) {
                    catchEditBinding.catchViewLayout.reportCatchAmountType2.setText(R.string.count)
                    catchEditBinding.catchViewLayout.reportCatchAmount2.text =
                        item.catch.number.toString()
                } else {
                    catchEditBinding.catchViewLayout.reportCatchAmountType2.setVisible(false)
                    catchEditBinding.catchViewLayout.reportCatchAmount2.setVisible(false)
                }
            }

            clearAmountIfNeeded(item)
        }

        private fun clearAmountIfNeeded(item: CatchItem) {
            if (!item.amount.contains(weightString, true)) item.catch.weight = 0.0
            if (!item.amount.contains(countString, true)) item.catch.number = 0L
        }

        private fun initUnitSpinner() {
            val spinner = catchEditBinding.spinnerUnit
            val unit = currentItem.catch.unit

            ArrayAdapter.createFromResource(
                catchEditBinding.root.context,
                R.array.weight_units,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }

            if (unit.isNotBlank()) {
                val array = catchEditBinding.root.context.resources.getStringArray(R.array.weight_units)
                spinner.setSelection(array.indexOf(unit))
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    currentItem.catch.unit = parent?.getItemAtPosition(position) as String
                }
            }
        }

        fun onItemRemove() {
            catchRemoveListener.invoke(adapterPosition)
        }

        fun onCatchEditClicked() {
            catchEditModeListener.invoke(currentItem)
        }


        fun onCatchAddAttachmentClicked() {
            catchAddAttachmentListener.invoke(currentItem)
        }

        fun onEditTextClicked(id: Int) {
            searchListener.invoke(id, currentItem)
        }
    }
}