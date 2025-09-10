/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.PasswordManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 密码列表适配器
 */
public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder> {

    private List<PasswordManager.PasswordEntry> passwordList;
    private OnPasswordClickListener listener;
    private SimpleDateFormat dateFormat;

    public PasswordAdapter(List<PasswordManager.PasswordEntry> passwordList, OnPasswordClickListener listener) {
        this.passwordList = passwordList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public PasswordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_password, parent, false);
        return new PasswordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PasswordViewHolder holder, int position) {
        PasswordManager.PasswordEntry entry = passwordList.get(position);
        holder.bind(entry, listener);
    }

    @Override
    public int getItemCount() {
        return passwordList.size();
    }

    static class PasswordViewHolder extends RecyclerView.ViewHolder {
        TextView tvDomain;
        TextView tvUsername;
        TextView tvLastUsed;
        TextView tvPasswordStrength;

        PasswordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDomain = itemView.findViewById(R.id.tv_domain);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvLastUsed = itemView.findViewById(R.id.tv_last_used);
            tvPasswordStrength = itemView.findViewById(R.id.tv_password_strength);
        }

        void bind(PasswordManager.PasswordEntry entry, OnPasswordClickListener listener) {
            tvDomain.setText(entry.domain);
            tvUsername.setText(entry.username);
            tvLastUsed.setText("最后使用: " + formatDate(entry.lastUsedTime));

            // 显示密码强度
            PasswordManager passwordManager = PasswordManager.getInstance(itemView.getContext());
            PasswordManager.PasswordStrength strength = passwordManager.checkPasswordStrength(entry.password);
            tvPasswordStrength.setText("强度: " + strength.getDescription());
            tvPasswordStrength.setTextColor(getStrengthColor(strength));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPasswordClick(entry);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onPasswordLongClick(entry);
                    return true;
                }
                return false;
            });
        }

        private String formatDate(long timestamp) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        }

        private int getStrengthColor(PasswordManager.PasswordStrength strength) {
            switch (strength) {
                case VERY_WEAK:
                    return itemView.getContext().getColor(android.R.color.holo_red_dark);
                case WEAK:
                    return itemView.getContext().getColor(android.R.color.holo_orange_dark);
                case MEDIUM:
                    return itemView.getContext().getColor(android.R.color.holo_blue_dark);
                case STRONG:
                    return itemView.getContext().getColor(android.R.color.holo_green_dark);
                case VERY_STRONG:
                    return itemView.getContext().getColor(android.R.color.holo_green_light);
                default:
                    return itemView.getContext().getColor(android.R.color.darker_gray);
            }
        }
    }

    public interface OnPasswordClickListener {
        void onPasswordClick(PasswordManager.PasswordEntry entry);
        void onPasswordLongClick(PasswordManager.PasswordEntry entry);
    }
}
