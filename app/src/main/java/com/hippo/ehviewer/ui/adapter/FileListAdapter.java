package com.hippo.ehviewer.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.model.FileItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 文件列表适配器
 */
public class FileListAdapter extends BaseAdapter {

    private Context mContext;
    private List<FileItem> mFileList;
    private LayoutInflater mInflater;
    private SimpleDateFormat mDateFormat;

    public FileListAdapter(Context context, List<FileItem> fileList) {
        mContext = context;
        mFileList = fileList;
        mInflater = LayoutInflater.from(context);
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    @Override
    public int getCount() {
        return mFileList.size();
    }

    @Override
    public FileItem getItem(int position) {
        return mFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_file_list, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.file_icon);
            holder.name = convertView.findViewById(R.id.file_name);
            holder.info = convertView.findViewById(R.id.file_info);
            holder.date = convertView.findViewById(R.id.file_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FileItem item = getItem(position);
        
        // 设置图标
        holder.icon.setImageResource(getFileIcon(item));
        
        // 设置文件名
        holder.name.setText(item.name);
        
        // 设置文件信息
        if (item.isParent) {
            holder.info.setText("返回上级目录");
            holder.date.setText("");
        } else {
            holder.info.setText(item.getDisplaySize());
            holder.date.setText(mDateFormat.format(new Date(item.lastModified)));
        }

        return convertView;
    }

    private int getFileIcon(FileItem item) {
        if (item.isParent) {
            return R.drawable.ic_arrow_back;
        } else if (item.isDirectory) {
            return R.drawable.ic_folder;
        } else {
            String extension = item.getFileExtension();
            switch (extension) {
                case "apk":
                    return R.drawable.ic_android;
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                case "bmp":
                case "webp":
                    return R.drawable.ic_image;
                case "mp4":
                case "avi":
                case "mkv":
                case "mov":
                case "wmv":
                case "flv":
                case "webm":
                case "3gp":
                    return R.drawable.ic_video;
                case "mp3":
                case "wav":
                case "ogg":
                case "aac":
                case "flac":
                case "m4a":
                case "wma":
                    return R.drawable.ic_audio;
                case "pdf":
                    return R.drawable.ic_pdf;
                case "txt":
                case "log":
                case "md":
                case "json":
                case "xml":
                    return R.drawable.ic_text;
                case "zip":
                case "rar":
                case "7z":
                case "tar":
                case "gz":
                    return R.drawable.ic_archive;
                default:
                    return R.drawable.ic_file;
            }
        }
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView info;
        TextView date;
    }
}