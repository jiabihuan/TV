package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.AdapterCinemaCategoryBinding;

import java.util.ArrayList;
import java.util.List;

public class CinemaCategoryAdapter extends RecyclerView.Adapter<CinemaCategoryAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<Class> items = new ArrayList<>();
    private int selected = 0;

    public interface OnClickListener {
        void onItemClick(Class item, int position);
    }

    public CinemaCategoryAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Class> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void addAll(List<Class> list) {
        int start = items.size();
        items.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

    public void setSelected(int position) {
        int old = selected;
        selected = position;
        if (old >= 0 && old < items.size()) notifyItemChanged(old);
        if (position >= 0 && position < items.size()) notifyItemChanged(position);
    }

    public int getSelected() {
        return selected;
    }

    public Class getItem(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    public int indexOf(Class item) {
        return items.indexOf(item);
    }

    public int getItemCountTotal() {
        return items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterCinemaCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Class item = items.get(position);
        holder.binding.text.setText(item.getTypeName());
        holder.binding.text.setSelected(position == selected);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final AdapterCinemaCategoryBinding binding;

        ViewHolder(AdapterCinemaCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            if (position >= 0 && position < items.size() && listener != null) {
                setSelected(position);
                listener.onItemClick(items.get(position), position);
            }
        }
    }
}
