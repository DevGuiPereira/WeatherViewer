package com.example.weatherviewer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class WeatherArrayAdapter extends ArrayAdapter<Weather> {

    // Pattern View Holder para performance (Item 7.3.5 do livro)
    private static class ViewHolder {
        TextView conditionIcon;
        TextView dayTextView;
        TextView lowTextView;
        TextView hiTextView;
        TextView humidityTextView;
        TextView descriptionTextView;
    }

    public WeatherArrayAdapter(Context context, List<Weather> forecast) {
        super(context, -1, forecast);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Weather day = getItem(position);
        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item, parent, false);

            viewHolder.conditionIcon = convertView.findViewById(R.id.conditionIcon);
            viewHolder.dayTextView = convertView.findViewById(R.id.dayTextView);
            viewHolder.lowTextView = convertView.findViewById(R.id.lowTextView);
            viewHolder.hiTextView = convertView.findViewById(R.id.hiTextView);
            viewHolder.humidityTextView = convertView.findViewById(R.id.humidityTextView);
            viewHolder.descriptionTextView = convertView.findViewById(R.id.descriptionTextView);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Preencher os dados
        if (day != null) {
            viewHolder.conditionIcon.setText(day.icon); // Emoji direto
            viewHolder.dayTextView.setText(day.dayOfWeek);
            viewHolder.lowTextView.setText("Min: " + day.minTemp);
            viewHolder.hiTextView.setText("Max: " + day.maxTemp);
            viewHolder.humidityTextView.setText("Hum: " + day.humidity);
            viewHolder.descriptionTextView.setText(day.description);
        }

        return convertView;
    }
}