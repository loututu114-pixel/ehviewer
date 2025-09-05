package com.hippo.ehviewer.ui.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.model.AppInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 已安装应用列表适配器
 */
public class InstalledAppsAdapter extends BaseAdapter {
    
    private Context mContext;
    private List<AppInfo> mApps;
    private LayoutInflater mInflater;
    private SimpleDateFormat mDateFormat;
    
    public InstalledAppsAdapter(Context context, List<AppInfo> apps) {
        mContext = context;
        mApps = apps;
        mInflater = LayoutInflater.from(context);
        mDateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
    }
    
    @Override
    public int getCount() {
        return mApps.size();
    }
    
    @Override
    public AppInfo getItem(int position) {
        return mApps.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_installed_app, parent, false);
            holder = new ViewHolder();
            holder.iconView = convertView.findViewById(R.id.app_icon);
            holder.nameView = convertView.findViewById(R.id.app_name);
            holder.packageView = convertView.findViewById(R.id.package_name);
            holder.versionView = convertView.findViewById(R.id.version_info);
            holder.sizeView = convertView.findViewById(R.id.app_size);
            holder.updateTimeView = convertView.findViewById(R.id.update_time);
            holder.typeIndicatorView = convertView.findViewById(R.id.type_indicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        AppInfo app = getItem(position);
        
        // 设置应用图标
        holder.iconView.setImageDrawable(app.icon);
        
        // 设置应用名称
        holder.nameView.setText(app.appName);
        
        // 设置包名
        holder.packageView.setText(app.packageName);
        
        // 设置版本信息
        String versionText = app.versionName + " (" + app.versionCode + ")";
        holder.versionView.setText(versionText);
        
        // 设置文件大小
        String sizeText = Formatter.formatFileSize(mContext, app.apkSize);
        holder.sizeView.setText(sizeText);
        
        // 设置更新时间
        String updateTimeText = mDateFormat.format(new Date(app.lastUpdateTime));
        holder.updateTimeView.setText("更新: " + updateTimeText);
        
        // 设置应用类型指示器
        if (app.isSystemApp) {
            holder.typeIndicatorView.setText("系统");
            holder.typeIndicatorView.setBackgroundResource(R.drawable.type_indicator_system);
        } else {
            holder.typeIndicatorView.setText("用户");
            holder.typeIndicatorView.setBackgroundResource(R.drawable.type_indicator_user);
        }
        
        // 设置启用状态样式
        float alpha = app.isEnabled ? 1.0f : 0.6f;
        convertView.setAlpha(alpha);
        
        return convertView;
    }
    
    private static class ViewHolder {
        ImageView iconView;
        TextView nameView;
        TextView packageView;
        TextView versionView;
        TextView sizeView;
        TextView updateTimeView;
        TextView typeIndicatorView;
    }
}